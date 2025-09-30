package org.tuna.zoopzoop.backend.domain.archive.folder.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.BeforeEach;
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
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FileSummary;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PersonalArchiveFolderService 단위 테스트
 * - 오케스트레이션 서비스만 검증 (Archive 조회/권한 컨텍스트)
 * - 공통 도메인 서비스(FolderService)는 mock 으로 스텁
 */
@ExtendWith(MockitoExtension.class)
class PersonalArchiveFolderServiceTest {

    @Mock private PersonalArchiveRepository personalArchiveRepository;
    @Mock private FolderService folderService; // 공통 도메인 서비스 (Archive 스코프)

    @InjectMocks private PersonalArchiveFolderService personalService;

    private Member member;
    private Archive archive;
    private PersonalArchive personalArchive;

    @BeforeEach
    void setUp() {
        member = new Member();
        ReflectionTestUtils.setField(member, "id", 1);

        archive = new Archive();
        ReflectionTestUtils.setField(archive, "id", 10);

        personalArchive = new PersonalArchive();
        ReflectionTestUtils.setField(personalArchive, "id", 100);
        personalArchive.setMember(member);
        personalArchive.setArchive(archive);
    }

    // ---------- Create ----------
    @Test
    @DisplayName("폴더 생성 성공(중복 없음)")
    void createFolder_success() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.createFolder(archive, "보고서"))
                .thenReturn(new FolderResponse(999, "보고서"));

        FolderResponse result = personalService.createFolder(1, "보고서");

        assertThat(result.folderId()).isEqualTo(999);
        assertThat(result.folderName()).isEqualTo("보고서");
        verify(folderService).createFolder(archive, "보고서");
    }

    @Test
    @DisplayName("폴더 이름 중복 시 '(1)' 붙여 생성")
    void createFolder_withConflict() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.createFolder(archive, "보고서"))
                .thenReturn(new FolderResponse(1000, "보고서 (1)"));

        FolderResponse result = personalService.createFolder(1, "보고서");

        assertThat(result.folderName()).isEqualTo("보고서 (1)");
        assertThat(result.folderId()).isEqualTo(1000);
        verify(folderService).createFolder(archive, "보고서");
    }

    @Test
    @DisplayName("개인 아카이브가 없으면 예외 발생")
    void createFolder_personalArchiveNotFound() {
        when(personalArchiveRepository.findByMemberId(2))
                .thenReturn(Optional.empty());

        assertThrows(NoResultException.class,
                () -> personalService.createFolder(2, "보고서"));

        verify(folderService, never()).createFolder(any(), anyString());
    }

    // ---------- Delete ----------
    @Test
    @DisplayName("폴더 삭제 성공 - 공통 서비스 호출 위임")
    void deleteFolder_success() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.deleteFolder(archive, 500))
                .thenReturn("보고서");

        String deletedName = personalService.deleteFolder(1, 500);

        assertThat(deletedName).isEqualTo("보고서");
        verify(folderService).deleteFolder(archive, 500);
    }

    @Test
    @DisplayName("폴더 삭제 실패 - 존재하지 않는 폴더")
    void deleteFolder_notFound() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.deleteFolder(archive, 999))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        assertThrows(NoResultException.class,
                () -> personalService.deleteFolder(1, 999));

        verify(folderService).deleteFolder(archive, 999);
    }

    @Test
    @DisplayName("default 폴더는 삭제할 수 없다")
    void deleteFolder_default_forbidden() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.deleteFolder(archive, 42))
                .thenThrow(new IllegalArgumentException("default 폴더는 삭제할 수 없습니다."));

        assertThrows(IllegalArgumentException.class,
                () -> personalService.deleteFolder(1, 42));

        verify(folderService).deleteFolder(archive, 42);
    }

    // ---------- Update ----------
    @Test
    @DisplayName("폴더 이름 변경 성공")
    void updateFolderName_ok() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.updateFolderName(archive, 5, "새이름"))
                .thenReturn("새이름");

        String result = personalService.updateFolderName(1, 5, "새이름");

        assertEquals("새이름", result);
        verify(folderService).updateFolderName(archive, 5, "새이름");
    }

    @Test
    @DisplayName("폴더 이름 변경 실패 - 존재하지 않음")
    void updateFolderName_notFound() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.updateFolderName(archive, 701, "아무거나"))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        assertThrows(NoResultException.class,
                () -> personalService.updateFolderName(1, 701, "아무거나"));

        verify(folderService).updateFolderName(archive, 701, "아무거나");
    }

    @Test
    @DisplayName("폴더 이름 변경 실패 - 중복 이름 존재")
    void updateFolderName_conflict() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.updateFolderName(archive, 700, "보고서"))
                .thenThrow(new IllegalArgumentException("이미 존재하는 폴더명입니다."));

        assertThrows(IllegalArgumentException.class,
                () -> personalService.updateFolderName(1, 700, "보고서"));

        verify(folderService).updateFolderName(archive, 700, "보고서");
    }

    // ---------- Read: 목록 ----------
    @Test
    @DisplayName("개인 아카이브 폴더 목록 조회 - 성공")
    void listFolders_success() {
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.listFolders(archive))
                .thenReturn(List.of(
                        new FolderResponse(1, "default"),
                        new FolderResponse(2, "docs")
                ));

        List<FolderResponse> rs = personalService.listFolders(1);

        assertThat(rs).hasSize(2);
        assertThat(rs.get(0).folderId()).isEqualTo(1);
        assertThat(rs.get(0).folderName()).isEqualTo("default");
        assertThat(rs.get(1).folderName()).isEqualTo("docs");

        verify(folderService).listFolders(archive);
    }

    // ---------- Read: 폴더 내 파일 ----------
    @Test
    @DisplayName("폴더 내 파일 목록 조회 - 성공")
    void getFilesInFolder_success() {
        Integer folderId = 2;

        FolderFilesDto stub = new FolderFilesDto(
                folderId,
                "docs",
                List.of(
                        new FileSummary(10, "spec.pdf", LocalDate.now(),
                                "요약 A", "http://src/a", "http://img/a",
                                List.of("tag1", "tag2"), "IT"),
                        new FileSummary(11, "notes.txt", LocalDate.now(),
                                "요약 B", "http://src/b", "http://img/b",
                                List.of(), "SCIENCE")
                )
        );
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.getFilesInFolder(archive, folderId))
                .thenReturn(stub);

        FolderFilesDto dto = personalService.getFilesInFolder(1, folderId);

        assertThat(dto.files()).hasSize(2);
        assertThat(dto.files().getFirst().title()).isEqualTo("spec.pdf");
        verify(folderService).getFilesInFolder(archive, folderId);
    }

    @Test
    @DisplayName("폴더 내 파일 목록 조회 - 폴더가 없으면 예외 발생")
    void getFilesInFolder_notFound() {
        Integer folderId = 999;
        when(personalArchiveRepository.findByMemberId(1))
                .thenReturn(Optional.of(personalArchive));
        when(folderService.getFilesInFolder(archive, folderId))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        assertThrows(NoResultException.class,
                () -> personalService.getFilesInFolder(1, folderId));

        verify(folderService).getFilesInFolder(archive, folderId);
    }
}
