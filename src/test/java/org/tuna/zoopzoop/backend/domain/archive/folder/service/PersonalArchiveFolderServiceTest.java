package org.tuna.zoopzoop.backend.domain.archive.folder.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalArchiveFolderServiceTest {

    @Mock private PersonalArchiveRepository personalArchiveRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private FolderService folderService;

    @InjectMocks private PersonalArchiveFolderService personalService;

    private Archive mockArchive() {
        Archive a = new Archive();
        ReflectionTestUtils.setField(a, "id", 10);
        return a;
    }

    private PersonalArchive mockPersonalArchive(Archive a) {
        PersonalArchive pa = new PersonalArchive();
        ReflectionTestUtils.setField(pa, "id", 100);
        pa.setArchive(a);
        return pa;
    }

    private Folder mockFolder(Archive a, Integer id, String name) {
        Folder f = new Folder();
        f.setArchive(a);
        f.setName(name);
        ReflectionTestUtils.setField(f, "id", id);
        return f;
    }

    // ---------------------- Create ----------------------
    @Test
    @DisplayName("개인 아카이브 폴더 생성 성공")
    void createFolder_success() {
        // given
        Integer memberId = 1;
        Archive archive = mockArchive();
        PersonalArchive pa = mockPersonalArchive(archive);

        when(personalArchiveRepository.findByMemberId(memberId)).thenReturn(Optional.of(pa));
        when(folderService.createFolder(eq(archive), eq("보고서")))
                .thenReturn(new FolderResponse("보고서", 999));

        // when
        FolderResponse rs = personalService.createFolder(memberId, "보고서");

        // then
        assertThat(rs.folderName()).isEqualTo("보고서");
        assertThat(rs.folderId()).isEqualTo(999);
        verify(folderService).createFolder(eq(archive), eq("보고서"));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 생성 실패 - 개인 아카이브 없음")
    void createFolder_archiveNotFound() {
        // given
        Integer memberId = 1;
        when(personalArchiveRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class, () -> personalService.createFolder(memberId, "보고서"));
        verify(folderService, never()).createFolder(any(), anyString());
    }

    // ---------------------- Delete ----------------------
    @Test
    @DisplayName("개인 아카이브 폴더 삭제 성공")
    void deleteFolder_success() {
        // given
        Integer memberId = 1;
        Integer folderId = 200;
        Archive archive = mockArchive();
        Folder folder = mockFolder(archive, folderId, "todelete");

        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.of(folder));
        when(folderService.deleteFolder(archive, folderId)).thenReturn("todelete");

        // when
        String deleted = personalService.deleteFolder(memberId, folderId);

        // then
        assertThat(deleted).isEqualTo("todelete");
        verify(folderService).deleteFolder(eq(archive), eq(folderId));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 삭제 실패 - 존재하지 않는 폴더")
    void deleteFolder_notFound() {
        // given
        Integer memberId = 1;
        Integer folderId = 999;
        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class, () -> personalService.deleteFolder(memberId, folderId));
        verify(folderService, never()).deleteFolder(any(), anyInt());
    }

    @Test
    @DisplayName("개인 아카이브 폴더 삭제 실패 - default 금지(공통 서비스 예외 전파)")
    void deleteFolder_defaultForbidden() {
        // given
        Integer memberId = 1;
        Integer folderId = 123;
        Archive archive = mockArchive();
        Folder folder = mockFolder(archive, folderId, "default?");
        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.of(folder));
        when(folderService.deleteFolder(archive, folderId))
                .thenThrow(new IllegalArgumentException("default 폴더는 삭제할 수 없습니다."));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> personalService.deleteFolder(memberId, folderId));
        verify(folderService).deleteFolder(eq(archive), eq(folderId));
    }

    // ---------------------- Update ----------------------
    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 성공")
    void updateFolderName_success() {
        // given
        Integer memberId = 1;
        Integer folderId = 300;
        Archive archive = mockArchive();
        Folder folder = mockFolder(archive, folderId, "old");
        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.of(folder));
        when(folderService.updateFolderName(archive, folderId, "new")).thenReturn("new");

        // when
        String updated = personalService.updateFolderName(memberId, folderId, "new");

        // then
        assertThat(updated).isEqualTo("new");
        verify(folderService).updateFolderName(eq(archive), eq(folderId), eq("new"));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 실패 - 폴더 없음")
    void updateFolderName_notFound() {
        // given
        Integer memberId = 1;
        Integer folderId = 301;
        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class,
                () -> personalService.updateFolderName(memberId, folderId, "new"));
        verify(folderService, never()).updateFolderName(any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 실패 - 중복 이름(공통 서비스 예외 전파)")
    void updateFolderName_conflict() {
        // given
        Integer memberId = 1;
        Integer folderId = 302;
        Archive archive = mockArchive();
        Folder folder = mockFolder(archive, folderId, "old");
        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.of(folder));
        when(folderService.updateFolderName(archive, folderId, "보고서"))
                .thenThrow(new IllegalArgumentException("이미 존재하는 폴더명입니다."));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> personalService.updateFolderName(memberId, folderId, "보고서"));
        verify(folderService).updateFolderName(eq(archive), eq(folderId), eq("보고서"));
    }

    // ---------------------- Read ----------------------
    @Test
    @DisplayName("개인 아카이브 폴더 목록 조회 성공")
    void getFolders_success() {
        // given
        Integer memberId = 1;
        Archive archive = mockArchive();
        PersonalArchive pa = mockPersonalArchive(archive);
        when(personalArchiveRepository.findByMemberId(memberId)).thenReturn(Optional.of(pa));
        when(folderService.getFolders(archive)).thenReturn(List.of(
                new FolderResponse("default", 1),
                new FolderResponse("docs", 2)
        ));

        // when
        List<FolderResponse> rs = personalService.getFolders(memberId);

        // then
        assertThat(rs).hasSize(2);
        assertThat(rs.getFirst().folderName()).isEqualTo("default");
        verify(folderService).getFolders(eq(archive));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 목록 조회 실패 - 개인 아카이브 없음")
    void getFolders_archiveNotFound() {
        // given
        Integer memberId = 1;
        when(personalArchiveRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class, () -> personalService.getFolders(memberId));
        verify(folderService, never()).getFolders(any());
    }

    @Test
    @DisplayName("개인 아카이브 폴더 내 파일 목록 조회 성공")
    void getFilesInFolder_success() {
        // given
        Integer memberId = 1;
        Integer folderId = 400;
        Archive archive = mockArchive();
        Folder folder = mockFolder(archive, folderId, "docs");
        FolderFilesDto dto = new FolderFilesDto(folderId, "docs", List.of());
        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.of(folder));
        when(folderService.getFilesInFolder(archive, folderId)).thenReturn(dto);

        // when
        FolderFilesDto rs = personalService.getFilesInFolder(memberId, folderId);

        // then
        assertThat(rs.folderId()).isEqualTo(folderId);
        assertThat(rs.folderName()).isEqualTo("docs");
        verify(folderService).getFilesInFolder(eq(archive), eq(folderId));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 내 파일 목록 조회 실패 - 폴더 없음")
    void getFilesInFolder_notFound() {
        // given
        Integer memberId = 1;
        Integer folderId = 401;
        when(folderRepository.findByIdAndMemberId(folderId, memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class,
                () -> personalService.getFilesInFolder(memberId, folderId));
        verify(folderService, never()).getFilesInFolder(any(), anyInt());
    }

    @Test
    @DisplayName("default 폴더 ID 조회 성공")
    void getDefaultFolderId_success() {
        // given
        Integer memberId = 1;
        Folder defaultFolder = mockFolder(mockArchive(), 42, "default");
        when(folderRepository.findDefaultFolderByMemberId(memberId)).thenReturn(Optional.of(defaultFolder));

        // when
        Integer id = personalService.getDefaultFolderId(memberId);

        // then
        assertThat(id).isEqualTo(42);
    }

    @Test
    @DisplayName("default 폴더 ID 조회 실패 - 없음")
    void getDefaultFolderId_notFound() {
        // given
        Integer memberId = 1;
        when(folderRepository.findDefaultFolderByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class, () -> personalService.getDefaultFolderId(memberId));
    }
}
