package org.tuna.zoopzoop.backend.domain.space.archive.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.SharingArchive;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpaceArchiveFolderServiceTest {

    @Mock private SpaceService spaceService;
    @Mock private MembershipService membershipService;
    @Mock private FolderService folderService;

    @InjectMocks private SpaceArchiveFolderService spaceArchiveFolderService;

    // --------- helpers ---------
    private Member requester() {
        Member m = new Member();
        ReflectionTestUtils.setField(m, "id", 7);
        return m;
    }

    private Space spaceWithArchive(int spaceId) {
        Space s = new Space();
        ReflectionTestUtils.setField(s, "id", spaceId);

        SharingArchive sa = new SharingArchive();
        Archive a = new Archive();
        ReflectionTestUtils.setField(a, "id", 999);
        sa.setArchive(a);
        sa.setSpace(s);

        s.setSharingArchive(sa);
        return s;
    }

    private Membership membership(Authority auth) {
        Membership ms = new Membership();
        ms.setAuthority(auth);
        return ms;
    }

    // ---------------------- Create ----------------------
    @Test
    @DisplayName("공유 아카이브 폴더 생성 성공 - 권한 있음")
    void createFolder_success() {
        Integer spaceId = 1;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.ADMIN));
        when(folderService.createFolder(any(Archive.class), eq("보고서")))
                .thenReturn(new FolderResponse("보고서", 123));

        FolderResponse rs = spaceArchiveFolderService.createFolder(spaceId, req, "보고서");

        assertThat(rs.folderName()).isEqualTo("보고서");
        assertThat(rs.folderId()).isEqualTo(123);
        verify(folderService).createFolder(eq(space.getSharingArchive().getArchive()), eq("보고서"));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 생성 실패 - 스페이스 없음")
    void createFolder_spaceNotFound() {
        Integer spaceId = 999;
        Member req = requester();

        when(spaceService.findById(spaceId)).thenThrow(new NoResultException("존재하지 않는 스페이스입니다."));

        assertThrows(NoResultException.class,
                () -> spaceArchiveFolderService.createFolder(spaceId, req, "보고서"));
        verifyNoInteractions(folderService);
    }

    @Test
    @DisplayName("공유 아카이브 폴더 생성 실패 - 구성원 아님")
    void createFolder_notMember() {
        Integer spaceId = 1;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> spaceArchiveFolderService.createFolder(spaceId, req, "보고서"));
        verifyNoInteractions(folderService);
    }

    @Test
    @DisplayName("공유 아카이브 폴더 생성 실패 - 권한 없음 (PENDING/READ_ONLY)")
    void createFolder_noAuthority() {
        Integer spaceId = 1;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.READ_ONLY));

        assertThrows(SecurityException.class,
                () -> spaceArchiveFolderService.createFolder(spaceId, req, "보고서"));
        verifyNoInteractions(folderService);
    }

    // ---------------------- Delete ----------------------
    @Test
    @DisplayName("공유 아카이브 폴더 삭제 성공")
    void deleteFolder_success() {
        Integer spaceId = 1, folderId = 10;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.ADMIN));
        when(folderService.deleteFolder(space.getSharingArchive().getArchive(), folderId))
                .thenReturn("docs");

        String deleted = spaceArchiveFolderService.deleteFolder(spaceId, req, folderId);

        assertThat(deleted).isEqualTo("docs");
        verify(folderService).deleteFolder(eq(space.getSharingArchive().getArchive()), eq(folderId));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 삭제 실패 - 권한 없음")
    void deleteFolder_noAuthority() {
        Integer spaceId = 1, folderId = 10;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.READ_ONLY));

        assertThrows(SecurityException.class,
                () -> spaceArchiveFolderService.deleteFolder(spaceId, req, folderId));
        verifyNoInteractions(folderService);
    }

    @Test
    @DisplayName("공유 아카이브 폴더 삭제 실패 - 존재하지 않는 폴더(공통 서비스 예외 전파)")
    void deleteFolder_notFound() {
        Integer spaceId = 1, folderId = 999;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.ADMIN));
        when(folderService.deleteFolder(space.getSharingArchive().getArchive(), folderId))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        assertThrows(NoResultException.class,
                () -> spaceArchiveFolderService.deleteFolder(spaceId, req, folderId));
        verify(folderService).deleteFolder(eq(space.getSharingArchive().getArchive()), eq(folderId));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 삭제 실패 - 기본 폴더는 삭제 불가(공통 서비스 예외 전파)")
    void deleteFolder_defaultForbidden() {
        Integer spaceId = 1, folderId = 0;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.ADMIN));
        when(folderService.deleteFolder(space.getSharingArchive().getArchive(), folderId))
                .thenThrow(new IllegalArgumentException("default 폴더는 삭제할 수 없습니다."));

        assertThrows(IllegalArgumentException.class,
                () -> spaceArchiveFolderService.deleteFolder(spaceId, req, folderId));
        verify(folderService).deleteFolder(eq(space.getSharingArchive().getArchive()), eq(folderId));
    }

    // ---------------------- Update ----------------------
    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 성공")
    void updateFolderName_success() {
        Integer spaceId = 1, folderId = 22;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.ADMIN));
        when(folderService.updateFolderName(space.getSharingArchive().getArchive(), folderId, "새이름"))
                .thenReturn("새이름");

        String updated = spaceArchiveFolderService.updateFolderName(spaceId, req, folderId, "새이름");

        assertThat(updated).isEqualTo("새이름");
        verify(folderService).updateFolderName(eq(space.getSharingArchive().getArchive()), eq(folderId), eq("새이름"));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 실패 - 중복 이름 존재(공통 서비스 예외 전파)")
    void updateFolderName_conflict() {
        Integer spaceId = 1, folderId = 22;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.ADMIN));
        when(folderService.updateFolderName(space.getSharingArchive().getArchive(), folderId, "보고서"))
                .thenThrow(new IllegalArgumentException("이미 존재하는 폴더명입니다."));

        assertThrows(IllegalArgumentException.class,
                () -> spaceArchiveFolderService.updateFolderName(spaceId, req, folderId, "보고서"));
        verify(folderService).updateFolderName(eq(space.getSharingArchive().getArchive()), eq(folderId), eq("보고서"));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 실패 - 폴더 없음(공통 서비스 예외 전파)")
    void updateFolderName_notFound() {
        Integer spaceId = 1, folderId = 22;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.ADMIN));
        when(folderService.updateFolderName(space.getSharingArchive().getArchive(), folderId, "새이름"))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        assertThrows(NoResultException.class,
                () -> spaceArchiveFolderService.updateFolderName(spaceId, req, folderId, "새이름"));
        verify(folderService).updateFolderName(eq(space.getSharingArchive().getArchive()), eq(folderId), eq("새이름"));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 실패 - 권한 없음")
    void updateFolderName_noAuthority() {
        Integer spaceId = 1, folderId = 22;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberJoinedSpace(req, space)).thenReturn(true);
        when(membershipService.findByMemberAndSpace(req, space)).thenReturn(membership(Authority.READ_ONLY));

        assertThrows(SecurityException.class,
                () -> spaceArchiveFolderService.updateFolderName(spaceId, req, folderId, "새이름"));
        verifyNoInteractions(folderService);
    }

    // ---------------------- Read: 목록/파일 ----------------------
    @Test
    @DisplayName("공유 아카이브 폴더 목록 조회 성공")
    void getFolders_success() {
        Integer spaceId = 1;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberInSpace(req, space)).thenReturn(true);
        when(folderService.getFolders(space.getSharingArchive().getArchive()))
                .thenReturn(List.of(new FolderResponse("default", 1), new FolderResponse("docs", 2)));

        List<FolderResponse> rs = spaceArchiveFolderService.getFolders(spaceId, req);

        assertThat(rs).hasSize(2);
        assertThat(rs.getFirst().folderName()).isEqualTo("default");
        verify(folderService).getFolders(eq(space.getSharingArchive().getArchive()));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 목록 조회 실패 - 권한 없음")
    void getFolders_noAuthority() {
        Integer spaceId = 1;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberInSpace(req, space)).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> spaceArchiveFolderService.getFolders(spaceId, req));
        verifyNoInteractions(folderService);
    }

    @Test
    @DisplayName("공유 아카이브 특정 폴더 내 파일 목록 조회 성공")
    void getFilesInFolder_success() {
        Integer spaceId = 1, folderId = 50;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);
        FolderFilesDto dto = new FolderFilesDto(folderId, "docs", List.of());

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberInSpace(req, space)).thenReturn(true);
        when(folderService.getFilesInFolder(space.getSharingArchive().getArchive(), folderId)).thenReturn(dto);

        FolderFilesDto rs = spaceArchiveFolderService.getFilesInFolder(spaceId, req, folderId);

        assertThat(rs.folderId()).isEqualTo(folderId);
        assertThat(rs.folderName()).isEqualTo("docs");
        verify(folderService).getFilesInFolder(eq(space.getSharingArchive().getArchive()), eq(folderId));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 내 파일 목록 조회 실패 - 폴더 없음(공통 서비스 예외 전파)")
    void getFilesInFolder_notFound() {
        Integer spaceId = 1, folderId = 404;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberInSpace(req, space)).thenReturn(true);
        when(folderService.getFilesInFolder(space.getSharingArchive().getArchive(), folderId))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        assertThrows(NoResultException.class,
                () -> spaceArchiveFolderService.getFilesInFolder(spaceId, req, folderId));
        verify(folderService).getFilesInFolder(eq(space.getSharingArchive().getArchive()), eq(folderId));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 내 파일 목록 조회 실패 - 권한 없음")
    void getFilesInFolder_noAuthority() {
        Integer spaceId = 1, folderId = 50;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberInSpace(req, space)).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> spaceArchiveFolderService.getFilesInFolder(spaceId, req, folderId));
        verifyNoInteractions(folderService);
    }

    // ---------------------- Read: default ID ----------------------
    @Test
    @DisplayName("공유 아카이브 default 폴더 ID 조회 성공")
    void getDefaultFolderId_success() {
        Integer spaceId = 1;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberInSpace(req, space)).thenReturn(true);
        when(folderService.getDefaultFolderId(space.getSharingArchive().getArchive()))
                .thenReturn(42);

        Integer id = spaceArchiveFolderService.getDefaultFolderId(spaceId, req);

        assertThat(id).isEqualTo(42);
        verify(folderService).getDefaultFolderId(eq(space.getSharingArchive().getArchive()));
    }

    @Test
    @DisplayName("공유 아카이브 default 폴더 ID 조회 실패 - 권한 없음")
    void getDefaultFolderId_noAuthority() {
        Integer spaceId = 1;
        Member req = requester();
        Space space = spaceWithArchive(spaceId);

        when(spaceService.findById(spaceId)).thenReturn(space);
        when(membershipService.isMemberInSpace(req, space)).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> spaceArchiveFolderService.getDefaultFolderId(spaceId, req));
        verifyNoInteractions(folderService);
    }
}
