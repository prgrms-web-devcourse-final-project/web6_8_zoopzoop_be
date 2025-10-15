package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.dto.UpdateOutcome;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PersonalDataSourceService {

    private final DataSourceService domain;
    private final DataSourceRepository dataSourceRepository;
    private final FolderRepository folderRepository;
    private final PersonalArchiveRepository personalArchiveRepository;
    private final DataProcessorService dataProcessorService;

    private int getPersonalArchiveId(int memberId) {
        PersonalArchive pa = personalArchiveRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoResultException("개인 아카이브를 찾을 수 없습니다."));
        return pa.getArchive().getId();
    }

    private int resolveTargetFolderIdByMember(int memberId, Integer folderIdOrZero) {
        if (folderIdOrZero == null || Objects.equals(folderIdOrZero, 0)) {
            return folderRepository.findDefaultFolderByMemberId(memberId)
                    .orElseThrow(() -> new NoResultException("기본 폴더가 존재하지 않습니다."))
                    .getId();
        }
        return folderRepository.findById(folderIdOrZero)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."))
                .getId();
    }

    // create
    @Transactional
    public int create(int memberId, String sourceUrl, Integer folderIdOrZero, DataSourceService.CreateCmd meta) throws IOException {
        int folderId = resolveTargetFolderIdByMember(memberId, folderIdOrZero);

        // 1) 태그 후보(사용자 입력) 준비
        List<Tag> baseTags = new ArrayList<>();
        if (meta.tags() != null) {
            for (String t : meta.tags()) baseTags.add(new Tag(t));
        }

        DataSourceDto dto = dataProcessorService.process(sourceUrl, baseTags);

        var cmd = DataSourceService.CreateCmd.builder()
                .title(dto.title())
                .summary(dto.summary())
                .source(dto.source())
                .sourceUrl(sourceUrl)
                .imageUrl(dto.imageUrl())
                .category(dto.category())
                .dataCreatedDate(dto.dataCreatedDate())
                .tags(dto.tags())
                .build();

        return domain.create(folderId, cmd);
    }

    // hard delete
    @Transactional
    public int deleteOne(int memberId, int dataSourceId) {
        DataSource ds = dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        // S3 삭제
        String expectedKey = domain.thumbnailKeyForPersonal(memberId, dataSourceId);
        domain.deleteIfOwnedByExactKey(ds.getImageUrl(), expectedKey);

        domain.hardDeleteOne(dataSourceId);
        return dataSourceId;
    }

    @Transactional
    public void deleteMany(int memberId, List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("삭제할 자료 id 배열이 비었습니다.");

        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);

        if (existing.size() != ids.size()) {
            Set<Integer> missing = new HashSet<>(ids);
            missing.removeAll(new HashSet<>(existing));
            throw new NoResultException("존재하지 않거나 소유자가 다른 자료 ID 포함: " + missing);
        }
        // S3 삭제
        List<DataSource> list = dataSourceRepository.findAllById(ids);
        for (DataSource ds : list) {
            String expectedKey = domain.thumbnailKeyForPersonal(memberId, ds.getId());
            domain.deleteIfOwnedByExactKey(ds.getImageUrl(), expectedKey);
        }

        domain.hardDeleteMany(ids);
    }

    // soft delete
    @Transactional
    public int softDelete(int memberId, List<Integer> ids) {
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size())
            throw new NoResultException("소유자 불일치/존재하지 않는 자료 포함");
        return domain.softDeleteMany(ids);
    }

    // restore
    @Transactional
    public int restore(int memberId, List<Integer> ids) {
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size())
            throw new NoResultException("소유자 불일치/존재하지 않는 자료 포함");
        return domain.restoreMany(ids);
    }

    // move
    @Transactional
    public DataSourceService.MoveResult moveOne(int memberId, int dataSourceId, Integer targetFolderIdOrZero) {
        dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        int folderId = resolveTargetFolderIdByMember(memberId, targetFolderIdOrZero);
        return domain.moveOne(dataSourceId, folderId);
    }

    // move many
    @Transactional
    public void moveMany(int memberId, Integer targetFolderIdOrZero, List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("이동할 자료 id 배열이 비었습니다.");

        // 중복 체크
        var dup = ids.stream()
                .collect(java.util.stream.Collectors.groupingBy(i -> i, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        if (!dup.isEmpty())
            throw new IllegalArgumentException("같은 자료를 두 번 선택: " + dup);

        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size())
            throw new NoResultException("소유자 불일치/존재하지 않는 자료 포함");

        int folderId = resolveTargetFolderIdByMember(memberId, targetFolderIdOrZero);
        domain.moveMany(ids, folderId);
    }

    // update (JSON-only)
    @Transactional
    public int update(int memberId, int dataSourceId, DataSourceService.UpdateCmd cmd) {
        dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        return domain.update(dataSourceId, cmd);
    }

    // update image (S3)
    @Transactional
    public UpdateOutcome updateWithImage(int memberId, int dataSourceId,
                                         DataSourceService.UpdateCmd baseCmd,
                                         MultipartFile image) {
        dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        var cmd = baseCmd;
        String finalUrl = null;

        if (image != null && !image.isEmpty()) {
            String key = domain.thumbnailKeyForPersonal(memberId, dataSourceId);
            finalUrl = domain.uploadThumbnailAndReturnFinalUrl(image, key);
            cmd = DataSourceService.UpdateCmd.builderFrom(baseCmd)
                    .imageUrl(JsonNullable.of(finalUrl))
                    .build();
        }

        int updatedId = domain.update(dataSourceId, cmd);
        return new UpdateOutcome(updatedId, finalUrl);
    }

    // search
    public Page<DataSourceSearchItem> search(int memberId,
                                             DataSourceSearchCondition cond,
                                             Pageable pageable) {
        return domain.searchByMember(memberId, cond, pageable);
    }
}
