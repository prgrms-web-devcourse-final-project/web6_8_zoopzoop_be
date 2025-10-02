package org.tuna.zoopzoop.backend.domain.space.archive.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.SharingArchive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.repository.SpaceRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SpaceDataSourceService {

    private final DataSourceService domain;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceQRepository dataSourceQRepository;
    private final FolderRepository folderRepository;
    private final SpaceRepository spaceRepository;
    private final MembershipRepository membershipRepository;

    private Space getSpace(String raw) {
        Integer spaceId;
        try { spaceId = Integer.valueOf(raw); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("유효하지 않은 spaceId 형식: " + raw); }

        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 스페이스입니다."));
    }
    private void assertReadable(int requesterMemberId, Space space) {
        membershipRepository.findByMemberIdAndSpaceId(requesterMemberId, space.getId())
                .orElseThrow(() -> new NoResultException("스페이스 멤버가 아닙니다."));
    }
    private void assertWritable(int requesterMemberId, Space space) {
        Membership ms = membershipRepository.findByMemberIdAndSpaceId(requesterMemberId, space.getId())
                .orElseThrow(() -> new NoResultException("스페이스 멤버가 아닙니다."));
        if (ms.getAuthority() == Authority.READ_ONLY)
            throw new SecurityException("쓰기 권한 없음");
    }
    private Integer getArchiveId(Space space) {
        SharingArchive sa = space.getSharingArchive();
        if (sa == null || sa.getArchive() == null) throw new NoResultException("공유 아카이브 미준비");
        return sa.getArchive().getId();
    }
    private int resolveTargetFolderIdByArchive(int archiveId, Integer folderIdOrZero) {
        if (folderIdOrZero == null || Objects.equals(folderIdOrZero, 0)) {
            return folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId)
                    .orElseThrow(() -> new NoResultException("공유 기본 폴더 없음")).getId();
        }
        Folder f = folderRepository.findById(folderIdOrZero)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));
        if (!Objects.equals(f.getArchive().getId(), archiveId))
            throw new IllegalArgumentException("해당 스페이스 아카이브 소속 폴더가 아닙니다.");
        return f.getId();
    }

    // ===== 불러오기(개인→공유) =====
    @Transactional
    public int importFromPersonal(int requesterMemberId, String spaceIdRaw, int sourceDataSourceId, Integer targetFolderIdOrZero) {
        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);

        DataSource source = dataSourceRepository.findByIdAndMemberId(sourceDataSourceId, requesterMemberId)
                .orElseThrow(() -> new NoResultException("개인 아카이브에서 자료를 찾을 수 없습니다."));

        int targetFolderId = resolveTargetFolderIdByArchive(archiveId, targetFolderIdOrZero);

        // 복제 생성 (도메인 서비스 이용)
        var cmd = DataSourceService.CreateCmd.builder()
                .title(source.getTitle())
                .summary(source.getSummary())
                .sourceUrl(source.getSourceUrl())
                .imageUrl(source.getImageUrl())
                .source(source.getSource())
                .category(source.getCategory())
                .dataCreatedDate(source.getDataCreatedDate())
                .tags(source.getTags() == null ? null :
                        source.getTags().stream().map(t -> t.getTagName()).toList())
                .build();
        return domain.create(targetFolderId, cmd);
    }

    @Transactional
    public java.util.List<Integer> importManyFromPersonal(int requesterMemberId, String spaceIdRaw, java.util.List<Integer> sourceIds, Integer targetFolderIdOrZero) {
        if (sourceIds == null || sourceIds.isEmpty())
            throw new IllegalArgumentException("불러올 자료 id 배열이 비어있습니다.");

        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);
        int targetFolderId = resolveTargetFolderIdByArchive(archiveId, targetFolderIdOrZero);

        // 소유 검증
        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(requesterMemberId, sourceIds);
        if (existing.size() != sourceIds.size()) {
            Set<Integer> missing = new HashSet<>(sourceIds);
            missing.removeAll(new HashSet<>(existing));
            throw new NoResultException("존재하지 않거나 소유자가 다른 자료 ID 포함: " + missing);
        }

        List<DataSource> list = dataSourceRepository.findAllById(sourceIds);
        if (list.size() != sourceIds.size()) throw new NoResultException("요청한 자료 중 존재하지 않는 항목이 있습니다.");

        List<Integer> created = new java.util.ArrayList<>();
        for (DataSource src : list) {
            var cmd = DataSourceService.CreateCmd.builder()
                    .title(src.getTitle())
                    .summary(src.getSummary())
                    .sourceUrl(src.getSourceUrl())
                    .imageUrl(src.getImageUrl())
                    .source(src.getSource())
                    .category(src.getCategory())
                    .dataCreatedDate(src.getDataCreatedDate())
                    .tags(src.getTags() == null ? null : src.getTags().stream().map(t -> t.getTagName()).toList())
                    .build();
            created.add(domain.create(targetFolderId, cmd));
        }
        return created;
    }

    // ===== 공유 스코프: 삭제/이동/수정/검색 =====

    @Transactional
    public int deleteOne(int requesterMemberId, String spaceIdRaw, int dataSourceId) {
        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);

        dataSourceRepository.findByIdAndArchiveId(dataSourceId, archiveId)
                .orElseThrow(() -> new NoResultException("해당 스페이스에 존재하지 않는 자료입니다."));
        domain.hardDeleteOne(dataSourceId);
        return dataSourceId;
    }

    @Transactional
    public void deleteMany(int requesterMemberId, String spaceIdRaw, java.util.List<Integer> ids) {
        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);

        List<Integer> existing = dataSourceRepository.findExistingIdsInArchive(archiveId, ids);
        if (existing.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        domain.hardDeleteMany(ids);
    }

    @Transactional
    public int softDelete(int requesterMemberId, String spaceIdRaw, java.util.List<Integer> ids) {
        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);

        List<Integer> existing = dataSourceRepository.findExistingIdsInArchive(archiveId, ids);
        if (existing.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        return domain.softDeleteMany(ids);
    }

    @Transactional
    public int restore(int requesterMemberId, String spaceIdRaw, java.util.List<Integer> ids) {
        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);

        List<Integer> existing = dataSourceRepository.findExistingIdsInArchive(archiveId, ids);
        if (existing.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        return domain.restoreMany(ids);
    }

    @Transactional
    public DataSourceService.MoveResult moveOne(int requesterMemberId, String spaceIdRaw, int dataSourceId, Integer targetFolderIdOrZero) {
        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);
        int folderId = resolveTargetFolderIdByArchive(archiveId, targetFolderIdOrZero);

        dataSourceRepository.findByIdAndArchiveId(dataSourceId, archiveId)
                .orElseThrow(() -> new NoResultException("해당 스페이스에 존재하지 않는 자료입니다."));
        return domain.moveOne(dataSourceId, folderId);
    }

    @Transactional
    public void moveMany(int requesterMemberId, String spaceIdRaw, Integer targetFolderIdOrZero, java.util.List<Integer> ids) {
        Space space = getSpace(spaceIdRaw);
        assertWritable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);
        int folderId = resolveTargetFolderIdByArchive(archiveId, targetFolderIdOrZero);

        List<Integer> existing = dataSourceRepository.findExistingIdsInArchive(archiveId, ids);
        if (existing.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        domain.moveMany(ids, folderId);
    }

    public Page<DataSourceSearchItem> search(int requesterMemberId, String spaceIdRaw, DataSourceSearchCondition cond, Pageable pageable) {
        Space space = getSpace(spaceIdRaw);
        assertReadable(requesterMemberId, space);
        Integer archiveId = getArchiveId(space);
        // folderId=0 → default
        if (cond.getFolderId() != null && cond.getFolderId() == 0) {
            int defaultFolderId = folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId)
                    .orElseThrow(() -> new NoResultException("공유 기본 폴더 없음"))
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
        return domain.searchInArchive(archiveId, cond, pageable);
    }
}

