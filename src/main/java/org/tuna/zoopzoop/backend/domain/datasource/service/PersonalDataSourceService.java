package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PersonalDataSourceService {

    private final DataSourceService domain;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceQRepository dataSourceQRepository;
    private final FolderRepository folderRepository;
    private final PersonalArchiveRepository personalArchiveRepository;

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

    // ===== 등록 (개인만) =====
    @Transactional
    public int create(int memberId, String sourceUrl, Integer folderIdOrZero, DataSourceService.CreateCmd meta) {
        int folderId = resolveTargetFolderIdByMember(memberId, folderIdOrZero);
        return domain.create(folderId, meta.toBuilder().sourceUrl(sourceUrl).build());
    }

    // ===== 삭제 =====
    @Transactional
    public int deleteOne(int memberId, int dataSourceId) {
        // 소유 검증
        dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        domain.hardDeleteOne(dataSourceId);
        return dataSourceId;
    }

    @Transactional
    public void deleteMany(int memberId, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("삭제할 자료 id 배열이 비었습니다.");
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size()) {
            Set<Integer> missing = new HashSet<>(ids);
            missing.removeAll(new HashSet<>(existing));
            throw new NoResultException("존재하지 않거나 소유자가 다른 자료 ID 포함: " + missing);
        }
        domain.hardDeleteMany(ids);
    }

    // ===== 소프트 삭제/복원 =====
    @Transactional
    public int softDelete(int memberId, List<Integer> ids) {
        // 소유 검증
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size()) throw new NoResultException("소유자 불일치/존재하지 않는 자료 포함");
        return domain.softDeleteMany(ids);
    }

    @Transactional
    public int restore(int memberId, List<Integer> ids) {
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size()) throw new NoResultException("소유자 불일치/존재하지 않는 자료 포함");
        return domain.restoreMany(ids);
    }

    // ===== 이동 =====
    @Transactional
    public DataSourceService.MoveResult moveOne(int memberId, int dataSourceId, Integer targetFolderIdOrZero) {
        // 소유 검증
        dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        int folderId = resolveTargetFolderIdByMember(memberId, targetFolderIdOrZero);
        return domain.moveOne(dataSourceId, folderId);
    }

    @Transactional
    public void moveMany(int memberId, Integer targetFolderIdOrZero, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("이동할 자료 id 배열이 비었습니다.");
        // 중복 체크
        var dup = ids.stream().collect(java.util.stream.Collectors.groupingBy(i -> i, java.util.stream.Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
        if (!dup.isEmpty()) throw new IllegalArgumentException("같은 자료를 두 번 선택: " + dup);

        // 소유 검증
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(memberId, ids);
        if (existing.size() != ids.size()) throw new NoResultException("소유자 불일치/존재하지 않는 자료 포함");

        int folderId = resolveTargetFolderIdByMember(memberId, targetFolderIdOrZero);
        domain.moveMany(ids, folderId);
    }

    // ===== 수정 =====
    @Transactional
    public int update(int memberId, int dataSourceId, DataSourceService.UpdateCmd cmd) {
        dataSourceRepository.findByIdAndMemberId(dataSourceId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        return domain.update(dataSourceId, cmd);
    }

    // ===== 검색 =====
    public Page<DataSourceSearchItem> search(int memberId, DataSourceSearchCondition cond, Pageable pageable) {
        // 0 → default 폴더 매핑
        if (cond.getFolderId() != null && cond.getFolderId() == 0) {
            int defaultFolderId = folderRepository.findDefaultFolderByMemberId(memberId)
                    .orElseThrow(() -> new NoResultException("기본 폴더가 존재하지 않습니다."))
                    .getId();
            cond = DataSourceSearchCondition.builder()
                    .title(cond.getTitle())
                    .summary(cond.getSummary())
                    .category(cond.getCategory())
                    .folderName(cond.getFolderName())
                    .isActive(cond.getIsActive())
                    .folderId(defaultFolderId)
                    .build();
        }
        return dataSourceQRepository.search(memberId, cond, pageable);
    }
}
