package org.tuna.zoopzoop.backend.domain.space.archive.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaceArchiveDataSourceService {

    private final SpaceService spaceService;
    private final MembershipService membershipService;

    private final DataSourceService dataSourceService;
    private final DataSourceRepository dataSourceRepository;
    private final FolderRepository folderRepository;

    private Archive getArchiveWithAuth(Integer spaceId, Member requester, boolean requireWrite) {
        Space space = spaceService.findById(spaceId);

        if (!membershipService.isMemberJoinedSpace(requester, space))
            throw new SecurityException("스페이스의 구성원이 아닙니다.");

        if (requireWrite) {
            Membership m = membershipService.findByMemberAndSpace(requester, space);
            Authority a = m.getAuthority();
            if (a == Authority.PENDING || a == Authority.READ_ONLY)
                throw new SecurityException("권한이 없습니다.");
        }

        Archive archive = space.getSharingArchive() == null ? null : space.getSharingArchive().getArchive();
        if (archive == null) throw new NoResultException("스페이스의 공유 아카이브가 없습니다.");
        return archive;
    }

    /** 개인 → 공유 : 단건 불러오기 (개인 아카이브 소유권은 요청자 기준) */
    @Transactional
    public void importOne(Integer spaceId, Member member, Integer personalDataSourceId) {
        Archive archive = getArchiveWithAuth(spaceId, member, true);

        // 요청자의 개인 아카이브 소유 자료인지 확인
        DataSource src = dataSourceRepository.findByIdAndMemberId(personalDataSourceId, member.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않거나 소유자가 다른 자료입니다."));

        // 타겟 폴더: 공유 아카이브의 default
        Folder target = folderRepository.findByArchiveIdAndIsDefaultTrue(archive.getId())
                .orElseThrow(() -> new NoResultException("default 폴더가 존재하지 않습니다."));

        cloneInto(src, target);
    }

    /** 개인 → 공유 : 다건 불러오기 */
    @Transactional
    public int importMany(Integer spaceId, Member requester, List<Integer> ids) {
        Archive archive = getArchiveWithAuth(spaceId, requester, true);
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("자료 ID 목록이 비었습니다.");

        List<Integer> existing = dataSourceRepository.findExistingIdsInMember(requester.getId(), ids);
        if (existing.isEmpty())
            return 0;

        Folder target = folderRepository.findByArchiveIdAndIsDefaultTrue(archive.getId())
                .orElseThrow(() -> new NoResultException("default 폴더가 존재하지 않습니다."));

        List<DataSource> list = dataSourceRepository.findAllById(existing);
        list.forEach(ds -> cloneInto(ds, target));
        return list.size();
    }

    // 원본 DataSource의 필드/태그 복제하여 타겟 폴더에 저장
    private void cloneInto(DataSource src, Folder targetFolder) {
        DataSource copy = new DataSource();
        copy.setFolder(targetFolder);
        copy.setTitle(src.getTitle());
        copy.setSummary(src.getSummary());
        copy.setSourceUrl(src.getSourceUrl());
        copy.setImageUrl(src.getImageUrl());
        copy.setDataCreatedDate(src.getDataCreatedDate());
        copy.setSource(src.getSource());
        copy.setCategory(src.getCategory());
        copy.setActive(true);

        if (src.getTags() != null) {
            for (Tag t : src.getTags()) {
                Tag nt = new Tag(t.getTagName());
                nt.setDataSource(copy);
                copy.getTags().add(nt);
            }
        }
        dataSourceRepository.save(copy);
    }


    @Transactional
    public int create(Integer spaceId, Member requester, String sourceUrl, Integer folderIdOrNull) {
        return dataSourceService.createDataSource(getArchiveWithAuth(spaceId, requester, true), sourceUrl, folderIdOrNull);
    }

    @Transactional
    public int deleteOne(Integer spaceId, Member requester, Integer dataSourceId) {
        return dataSourceService.deleteById(getArchiveWithAuth(spaceId, requester, true), dataSourceId);
    }

    @Transactional
    public void deleteMany(Integer spaceId, Member requester, List<Integer> ids) {
        dataSourceService.deleteMany(getArchiveWithAuth(spaceId, requester, true), ids);
    }

    @Transactional
    public int softDelete(Integer spaceId, Member requester, List<Integer> ids) {
        return dataSourceService.softDelete(getArchiveWithAuth(spaceId, requester, true), ids);
    }

    @Transactional
    public int restore(Integer spaceId, Member requester, List<Integer> ids) {
        return dataSourceService.restore(getArchiveWithAuth(spaceId, requester, true), ids);
    }

    @Transactional
    public DataSourceService.MoveResult moveOne(Integer spaceId, Member requester, Integer dataSourceId, Integer targetFolderIdOrNull) {
        return dataSourceService.moveDataSource(getArchiveWithAuth(spaceId, requester, true), dataSourceId, targetFolderIdOrNull);
    }

    @Transactional
    public void moveMany(Integer spaceId, Member requester, Integer targetFolderIdOrNull, List<Integer> ids) {
        dataSourceService.moveDataSources(getArchiveWithAuth(spaceId, requester, true), targetFolderIdOrNull, ids);
    }

    @Transactional
    public Integer update(Integer spaceId, Member requester, Integer dataSourceId, String title, String summary) {
        return dataSourceService.updateDataSource(getArchiveWithAuth(spaceId, requester, true), dataSourceId, title, summary);
    }

    @Transactional
    public Page<DataSourceSearchItem> search(Integer spaceId, Member requester, DataSourceSearchCondition cond, Pageable pageable) {
        return dataSourceService.search(getArchiveWithAuth(spaceId, requester, false), cond, pageable);
    }
}
