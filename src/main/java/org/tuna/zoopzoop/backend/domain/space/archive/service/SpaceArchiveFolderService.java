package org.tuna.zoopzoop.backend.domain.space.archive.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.SharingArchive;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;

import java.util.List;
import java.util.Optional;

// package org.tuna.zoopzoop.backend.domain.space.archive.service;

@Service
@RequiredArgsConstructor
public class SpaceArchiveFolderService {

    private final SpaceService spaceService;
    private final MembershipService membershipService;
    private final FolderService folderService;

    @Transactional
    public FolderResponse createFolder(Integer spaceId, Member requester, String folderName) {
        Space space = spaceService.findById(spaceId);

        if (!membershipService.isMemberJoinedSpace(requester, space))
            throw new SecurityException("스페이스의 구성원이 아닙니다.");

        var m = membershipService.findByMemberAndSpace(requester, space);
        var auth = m.getAuthority();
        if (auth == Authority.PENDING || auth == Authority.READ_ONLY)
            throw new SecurityException("폴더 생성 권한이 없습니다.");

        Archive archive = Optional.ofNullable(space.getSharingArchive())
                .map(SharingArchive::getArchive)
                .orElseThrow(() -> new NoResultException("스페이스의 공유 아카이브가 없습니다."));

        return folderService.createFolder(archive, folderName);
    }

    @Transactional
    public String deleteFolder(Integer spaceId, Member requester, Integer folderId) {
        Space space = spaceService.findById(spaceId);
        if (!membershipService.isMemberJoinedSpace(requester, space))
            throw new SecurityException("스페이스의 구성원이 아닙니다.");

        var m = membershipService.findByMemberAndSpace(requester, space);
        var auth = m.getAuthority();
        if (auth == Authority.PENDING || auth == Authority.READ_ONLY)
            throw new SecurityException("폴더 삭제 권한이 없습니다.");

        Archive archive = Optional.ofNullable(space.getSharingArchive())
                .map(SharingArchive::getArchive)
                .orElseThrow(() -> new NoResultException("스페이스의 공유 아카이브가 없습니다."));

        return folderService.deleteFolder(archive, folderId);
    }

    @Transactional
    public String updateFolderName(Integer spaceId, Member requester, Integer folderId, String newName) {
        Space space = spaceService.findById(spaceId);
        if (!membershipService.isMemberJoinedSpace(requester, space))
            throw new SecurityException("스페이스의 구성원이 아닙니다.");

        var m = membershipService.findByMemberAndSpace(requester, space);
        var auth = m.getAuthority();
        if (auth == Authority.PENDING || auth == Authority.READ_ONLY)
            throw new SecurityException("폴더 수정 권한이 없습니다.");

        Archive archive = Optional.ofNullable(space.getSharingArchive())
                .map(SharingArchive::getArchive)
                .orElseThrow(() -> new NoResultException("스페이스의 공유 아카이브가 없습니다."));

        return folderService.updateFolderName(archive, folderId, newName);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFolders(Integer spaceId, Member requester) {
        Space space = spaceService.findById(spaceId);
        if (!membershipService.isMemberInSpace(requester, space)) // 읽기: PENDING도 허용할지 정책대로
            throw new SecurityException("스페이스의 구성원이 아닙니다.");

        Archive archive = Optional.ofNullable(space.getSharingArchive())
                .map(SharingArchive::getArchive)
                .orElseThrow(() -> new NoResultException("스페이스의 공유 아카이브가 없습니다."));

        return folderService.getFolders(archive);
    }

    @Transactional(readOnly = true)
    public FolderFilesDto getFilesInFolder(Integer spaceId,     Member requester, Integer folderId) {
        Space space = spaceService.findById(spaceId);
        if (!membershipService.isMemberInSpace(requester, space))
            throw new SecurityException("스페이스의 구성원이 아닙니다.");

        Archive archive = Optional.ofNullable(space.getSharingArchive())
                .map(SharingArchive::getArchive)
                .orElseThrow(() -> new NoResultException("스페이스의 공유 아카이브가 없습니다."));

        return folderService.getFilesInFolder(archive, folderId);
    }

    @Transactional(readOnly = true)
    public Integer getDefaultFolderId(Integer spaceId, Member requester) {
        Space space = spaceService.findById(spaceId);
        if (!membershipService.isMemberInSpace(requester, space))
            throw new SecurityException("스페이스의 구성원이 아닙니다.");

        Archive archive = Optional.ofNullable(space.getSharingArchive())
                .map(SharingArchive::getArchive)
                .orElseThrow(() -> new NoResultException("스페이스의 공유 아카이브가 없습니다."));

        return folderService.getDefaultFolderId(archive);
    }
}
