package org.tuna.zoopzoop.backend.domain.space.archive.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;

import java.util.List;

// domain.space.archive.service.SpaceArchiveFolderService.java (신규)
@Service
@RequiredArgsConstructor
public class SpaceArchiveFolderService {
    private final SpaceService spaceService;
    private final MembershipService membershipService;
    private final FolderService folderService;

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

        Archive archive = space.getSharingArchive() != null ? space.getSharingArchive().getArchive() : null;
        if (archive == null) throw new NoResultException("스페이스의 공유 아카이브가 없습니다.");
        return archive;
    }

    @Transactional
    public FolderResponse createFolder(Integer spaceId, Member requester, String folderName) {
        return folderService.createFolder(getArchiveWithAuth(spaceId, requester, true), folderName);
    }

    @Transactional
    public String deleteFolder(Integer spaceId, Member requester, Integer folderId) {
        return folderService.deleteFolder(getArchiveWithAuth(spaceId, requester, true), folderId);
    }

    @Transactional
    public String updateFolderName(Integer spaceId, Member requester, Integer folderId, String newName) {
        return folderService.updateFolderName(getArchiveWithAuth(spaceId, requester, true), folderId, newName);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listFolders(Integer spaceId, Member requester) {
        return folderService.listFolders(getArchiveWithAuth(spaceId, requester, false));
    }

    @Transactional(readOnly = true)
    public FolderFilesDto getFilesInFolder(Integer spaceId, Member requester, Integer folderId) {
        return folderService.getFilesInFolder(getArchiveWithAuth(spaceId, requester, false), folderId);
    }

    @Transactional(readOnly = true)
    public Integer getDefaultFolderId(Integer spaceId, Member requester) {
        return folderService.getDefaultFolderId(getArchiveWithAuth(spaceId, requester, false));
    }
}

