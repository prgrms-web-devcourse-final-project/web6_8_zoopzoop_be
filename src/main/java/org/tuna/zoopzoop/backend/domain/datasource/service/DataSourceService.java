package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSourceService { // ← 공통(Archive 스코프) 전용
    private final DataSourceRepository dataSourceRepository;
    private final FolderRepository folderRepository;
    private final TagRepository tagRepository;
    private final DataProcessorService dataProcessorService;
    private final DataSourceQRepository dataSourceQRepository;

    /** ===== 생성 ===== */
    @Transactional
    public int createDataSource(Archive archive, String sourceUrl, Integer folderIdOrNull) {
        Folder folder = resolveTargetFolder(archive, folderIdOrNull);

        // 폴더 하위 태그(중복 제거)
        List<Tag> contextTags = collectDistinctTagsOfFolder(folder.getId());
        DataSource ds = buildDataSource(folder, sourceUrl, contextTags);

        return dataSourceRepository.save(ds).getId();
    }

    /** ===== 단건 삭제 ===== */
    @Transactional
    public int deleteById(Archive archive, Integer dataSourceId) {
        DataSource ds = dataSourceRepository.findByIdAndArchiveId(dataSourceId, archive.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        dataSourceRepository.delete(ds);
        return ds.getId();
    }

    /** ===== 다건 삭제 ===== */
    @Transactional
    public void deleteMany(Archive archive, List<Integer> ids) {
        checkInArchive(archive, ids);
        dataSourceRepository.deleteAllByIdInBatch(ids);
    }

    /** ===== 소프트 삭제 ===== */
    @Transactional
    public int softDelete(Archive archive, List<Integer> ids) {
        checkInArchive(archive, ids);
        return dataSourceRepository.softDeleteAllByIds(ids, LocalDateTime.now());
    }

    /** ===== 복원 ===== */
    @Transactional
    public int restore(Archive archive, List<Integer> ids) {
        checkInArchive(archive, ids);
        return dataSourceRepository.restoreAllByIds(ids);
    }

    /** ===== 단건 이동 ===== */
    @Transactional
    public MoveResult moveDataSource(Archive archive, Integer dataSourceId, Integer targetFolderIdOrNull) {
        DataSource ds = dataSourceRepository.findByIdAndArchiveId(dataSourceId, archive.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        Folder target = resolveTargetFolder(archive, targetFolderIdOrNull);
        if (Objects.equals(ds.getFolder().getId(), target.getId()))
            return new MoveResult(ds.getId(), target.getId());

        ds.setFolder(target);
        return new MoveResult(ds.getId(), target.getId());
    }

    /** ===== 다건 이동 ===== */
    @Transactional
    public void moveDataSources(Archive archive, Integer targetFolderIdOrNull, List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("자료 id 목록이 비었습니다.");
        // 중복 방지
        var dup = ids.stream().collect(Collectors.groupingBy(i -> i, Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
        if (!dup.isEmpty()) throw new IllegalArgumentException("중복 id 포함: " + dup);

        Folder target = resolveTargetFolder(archive, targetFolderIdOrNull);
        checkInArchive(archive, ids);

        List<DataSource> list = dataSourceRepository.findAllByIdIn(ids);
        if (list.size() != ids.size())
            throw new NoResultException("요청한 자료 중 존재하지 않는 항목이 있습니다.");

        list.stream()
                .filter(d -> !Objects.equals(d.getFolder().getArchive().getId(), archive.getId()))
                .findAny()
                .ifPresent(d -> { throw new SecurityException("아카이브 소속이 다른 자료가 포함되었습니다."); });

        list.forEach(d -> { if (!Objects.equals(d.getFolder().getId(), target.getId())) d.setFolder(target); });
    }

    /** ===== 수정 ===== */
    @Transactional
    public Integer updateDataSource(Archive archive, Integer dataSourceId, String newTitle, String newSummary) {
        DataSource ds = dataSourceRepository.findByIdAndArchiveId(dataSourceId, archive.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        if (newTitle != null && !newTitle.isBlank()) ds.setTitle(newTitle);
        if (newSummary != null && !newSummary.isBlank()) ds.setSummary(newSummary);
        return ds.getId();
    }

    /** ===== 검색 ===== */
    @Transactional
    public Page<DataSourceSearchItem> search(Archive archive, DataSourceSearchCondition cond, Pageable pageable) {
        return dataSourceQRepository.searchInArchive(archive.getId(), cond, pageable);
    }

    /** ===== 내부 유틸 ===== */
    private Folder resolveTargetFolder(Archive archive, Integer folderIdOrNull) {
        if (folderIdOrNull == null) {
            return folderRepository.findByArchiveIdAndIsDefaultTrue(archive.getId())
                    .orElseThrow(() -> new NoResultException("default 폴더가 존재하지 않습니다."));
        }
        Folder f = folderRepository.findByIdAndArchiveId(folderIdOrNull, archive.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않거나 다른 아카이브의 폴더입니다."));
        return f;
    }

    private List<Tag> collectDistinctTagsOfFolder(Integer folderId) {
        List<String> names = tagRepository.findDistinctTagNamesByFolderId(folderId);
        return names.stream().map(Tag::new).toList();
    }

    private DataSource buildDataSource(Folder folder, String sourceUrl, List<Tag> tagList) {
        final DataSourceDto dto;
        try {
            dto = dataProcessorService.process(sourceUrl, tagList);
        } catch (IOException e) {
            throw new RuntimeException("자료 처리 중 오류가 발생했습니다.", e);
        }
        DataSource ds = new DataSource();
        ds.setFolder(folder);
        ds.setSourceUrl(dto.sourceUrl());
        ds.setTitle(dto.title());
        ds.setSummary(dto.summary());
        ds.setDataCreatedDate(dto.dataCreatedDate());
        ds.setImageUrl(dto.imageUrl());
        ds.setSource(dto.source());
        ds.setCategory(dto.category());
        ds.setActive(true);

        if (dto.tags() != null) {
            for (String tagName : dto.tags()) {
                Tag tag = new Tag(tagName);
                tag.setDataSource(ds);
                ds.getTags().add(tag);
            }
        }
        return ds;
    }

    private void checkInArchive(Archive archive, List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("id 목록이 비었습니다.");
        List<Integer> existing = dataSourceRepository.findExistingIdsInArchive(archive.getId(), ids);
        if (existing.size() != ids.size()) {
            Set<Integer> missing = new HashSet<>(ids);
            missing.removeAll(new HashSet<>(existing));
            throw new NoResultException("존재하지 않거나 소속이 다른 자료 ID 포함: " + missing);
        }
    }

    public record MoveResult(Integer datasourceId, Integer folderId) {}
}
