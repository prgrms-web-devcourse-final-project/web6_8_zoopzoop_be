package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSourceService {
    private final DataSourceRepository dataSourceRepository;
    private final FolderRepository folderRepository;
    private final PersonalArchiveRepository personalArchiveRepository;
    private final TagRepository tagRepository;
    private final DataProcessorService dataProcessorService;
    private final DataSourceQRepository dataSourceQRepository;

    /**
     * 지정한 folder 위치에 자료 생성
     */
    @Transactional
    public int createDataSource(int currentMemberId, String sourceUrl, Integer folderId) {
        Folder folder;
        if(folderId == null)
            folder = findDefaultFolder(currentMemberId);
        else
            folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        // 폴더 하위 자료 태그 수집(중복 X)
        List<Tag> contextTags = collectDistinctTagsOfFolder(folder.getId());

        DataSource ds = buildDataSource(folder, sourceUrl, contextTags);

        // 4) 저장
        final DataSource saved = dataSourceRepository.save(ds);
        return saved.getId();
    }

    // 폴더 하위 태그 중복없이 list 반환
    private List<Tag> collectDistinctTagsOfFolder(Integer folderId) {
        List<String> names = tagRepository.findDistinctTagNamesByFolderId(folderId);

        return names.stream()
                .map(Tag::new)
                .toList();
    }

    private DataSource buildDataSource(Folder folder, String sourceUrl, List<Tag> tagList) {
        final DataSourceDto dataSourceDto;
        try {
            dataSourceDto = dataProcessorService.process(sourceUrl, tagList);
        } catch (IOException e) {
            throw new RuntimeException("자료 처리 중 오류가 발생했습니다.", e);
        }

        DataSource ds = new DataSource();
        ds.setFolder(folder);
        ds.setSourceUrl(dataSourceDto.sourceUrl());
        ds.setTitle(dataSourceDto.title());
        ds.setSummary(dataSourceDto.summary());
        ds.setDataCreatedDate(dataSourceDto.dataCreatedDate());
        ds.setImageUrl(dataSourceDto.imageUrl());
        ds.setSource(dataSourceDto.source());
        ds.setCategory(dataSourceDto.category());
        ds.setActive(true);

        if (dataSourceDto.tags() != null) {
            for (String tagName : dataSourceDto.tags()) {
                Tag tag = new Tag(tagName);
                tag.setDataSource(ds);
                ds.getTags().add(tag);
            }
        }

        return ds;
    }

    private Folder findDefaultFolder(int currentMemberId) {
        PersonalArchive pa = personalArchiveRepository.findByMemberId(currentMemberId)
                .orElseThrow(() -> new NoResultException("개인 아카이브를 찾을 수 없습니다."));

        Integer archiveId = pa.getArchive().getId();

        return folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId)
                .orElseThrow(() -> new NoResultException("default 폴더를 찾을 수 없습니다."));
    }

    /**
     * 자료 단건 삭제
     */
    @Transactional
    public int deleteById(Integer memberId, Integer dataSourceId) {
        // member 범위에서 자료를 조회하여 소유 확인
        DataSource ds = dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        dataSourceRepository.delete(ds);
        return dataSourceId;
    }

    /**
     * 자료 다건 삭제
     */
    @Transactional
    public void deleteMany(Integer memberId, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 자료 id 배열이 비어있습니다.");
        }

        // 해당 멤버가 소유한 id만 조회
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size()) {
            Set<Integer> missing = new HashSet<>(ids);
            missing.removeAll(new HashSet<>(existing));
            throw new NoResultException("존재하지 않거나 소유자가 다른 자료 ID 포함: " + missing);
        }

        dataSourceRepository.deleteAllByIdInBatch(ids);
    }

    /**
     * 자료 위치 단건 이동
     */
    @Transactional
    public MoveResult moveDataSource(Integer currentMemberId, Integer dataSourceId, Integer targetFolderId) {

        DataSource ds = dataSourceRepository.findByIdAndMemberId(dataSourceId, currentMemberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        Folder targetFolder = resolveTargetFolder(currentMemberId, targetFolderId);

        if (ds.getFolder().getId() == targetFolder.getId())
            return  new MoveResult(ds.getId(), targetFolder.getId());

        ds.setFolder(targetFolder);

        return  new MoveResult(ds.getId(), targetFolder.getId());
    }

    @Transactional
    public void moveDataSources(Integer currentMemberId, Integer targetFolderId, List<Integer> dataSourceIds) {
        if (dataSourceIds.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("자료 id 목록에 null이 포함되어 있습니다.");

        Map<Integer, Long> counts = dataSourceIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        List<Integer> duplicates = counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("같은 자료를 두 번 선택했습니다: " + duplicates);
        }

        Folder targetFolder = resolveTargetFolder(currentMemberId, targetFolderId);

        // 소유 검증: 요청된 id들이 모두 현재 멤버의 소유인지 확인
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(currentMemberId, dataSourceIds);
        if (existing.size() != dataSourceIds.size()) {
            Set<Integer> missing = new HashSet<>(dataSourceIds);
            missing.removeAll(new HashSet<>(existing));
            throw new NoResultException("존재하지 않거나 소유자가 다른 자료 ID 포함: " + missing);
        }

        List<DataSource> list = dataSourceRepository.findAllByIdIn(dataSourceIds);
        if (list.size() != dataSourceIds.size())
            throw new NoResultException("요청한 자료 중 존재하지 않는 항목이 있습니다.");

        List<DataSource> needMove = list.stream()
                .filter(ds -> !Objects.equals(ds.getFolder().getId(), targetFolder.getId()))
                .toList();

        if (needMove.isEmpty())
            return;

        needMove.forEach(ds -> ds.setFolder(targetFolder));
    }

    private Folder resolveTargetFolder(Integer currentMemberId, Integer targetFolderId) {
        if (targetFolderId == null) {
            return folderRepository.findDefaultFolderByMemberId(currentMemberId)
                    .orElseThrow(() -> new NoResultException("기본 폴더가 존재하지 않습니다."));
        }
        return folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));
    }

    /**
     * 자료 수정
     */
    public Integer updateDataSource(Integer memberId, Integer dataSourceId, String newTitle, String newSummary) {
        DataSource ds = dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        if (newTitle != null && !newTitle.isBlank())
            ds.setTitle(newTitle);

        if (newSummary != null && !newSummary.isBlank())
            ds.setSummary(newSummary);

        return ds.getId();
    }


    @Transactional
    public Page<DataSourceSearchItem> search(Integer memberId, DataSourceSearchCondition cond, Pageable pageable) {
        return dataSourceQRepository.search(memberId, cond, pageable);
    }

    public record MoveResult(Integer datasourceId, Integer folderId) {}
}
