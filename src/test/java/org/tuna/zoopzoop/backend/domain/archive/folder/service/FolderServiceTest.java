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
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private PersonalArchiveRepository personalArchiveRepository;
    @Mock private FolderRepository folderRepository;

    @InjectMocks private FolderService folderService;

    private Member member;
    private Archive archive;
    private PersonalArchive personalArchive;

    @BeforeEach
    void setUp() {
        this.member = new Member();
        ReflectionTestUtils.setField(member, "id", 1);

        this.archive = new Archive();
        ReflectionTestUtils.setField(archive, "id", 10);

        this.personalArchive = new PersonalArchive();
        ReflectionTestUtils.setField(personalArchive, "id", 100);
        personalArchive.setMember(member);
        personalArchive.setArchive(archive);
    }

    // ---------- Create ----------
    @Test
    @DisplayName("폴더 생성 성공(중복 없음)")
    void createFolder_success() {
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(personalArchiveRepository.findByMemberId(1)).thenReturn(Optional.of(personalArchive));
        when(folderRepository.findNamesForConflictCheck(eq(archive.getId()), anyString(), anyString()))
                .thenReturn(List.of()); // 충돌 없음

        Folder saved = new Folder();
        saved.setName("보고서");
        saved.setArchive(archive);
        ReflectionTestUtils.setField(saved, "id", 999);

        when(folderRepository.save(any(Folder.class))).thenReturn(saved);

        FolderResponse result = folderService.createFolderForPersonal(1, "보고서");

        assertThat(result.folderId()).isEqualTo(999);
        assertThat(result.folderName()).isEqualTo("보고서");
    }

    @Test
    @DisplayName("폴더 이름 중복 시 '(1)' 붙여 생성")
    void createFolder_withConflict() {
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(personalArchiveRepository.findByMemberId(1)).thenReturn(Optional.of(personalArchive));
        when(folderRepository.findNamesForConflictCheck(eq(archive.getId()), eq("보고서"), anyString()))
                .thenReturn(List.of("보고서"));

        Folder saved = new Folder();
        saved.setName("보고서(1)");
        saved.setArchive(archive);
        ReflectionTestUtils.setField(saved, "id", 1000);

        when(folderRepository.save(any(Folder.class))).thenReturn(saved);

        FolderResponse result = folderService.createFolderForPersonal(1, "보고서");

        assertThat(result.folderName()).isEqualTo("보고서(1)");
        assertThat(result.folderId()).isEqualTo(1000);
    }

    @Test
    @DisplayName("멤버가 없으면 예외 발생")
    void createFolder_memberNotFound() {
        when(memberRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> folderService.createFolderForPersonal(2, "보고서"));
    }

    // ---------- Delete ----------
    @Test
    @DisplayName("폴더 삭제 성공")
    void deleteFolder_success() {
        Folder folder = new Folder();
        folder.setName("보고서");
        folder.setArchive(archive);
        ReflectionTestUtils.setField(folder, "id", 500);

        when(folderRepository.findById(500)).thenReturn(Optional.of(folder));

        String deletedName = folderService.deleteFolder(500);

        assertThat(deletedName).isEqualTo("보고서");
        verify(folderRepository, times(1)).delete(folder);
    }

    @Test
    @DisplayName("폴더 삭제 실패 - 존재하지 않는 폴더")
    void deleteFolder_notFound() {
        when(folderRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NoResultException.class, () -> folderService.deleteFolder(999));
        verify(folderRepository, never()).delete(any(Folder.class));
    }

    // ---------- Update ----------
    @Test
    @DisplayName("폴더 이름 변경 성공")
    void updateFolderName_success() {
        Folder folder = new Folder();
        folder.setName("기존이름");
        folder.setArchive(archive);
        ReflectionTestUtils.setField(folder, "id", 700);

        when(folderRepository.findById(700)).thenReturn(Optional.of(folder));
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String updated = folderService.updateFolderName(700, "새이름");

        assertThat(updated).isEqualTo("새이름");
        assertThat(folder.getName()).isEqualTo("새이름");
        verify(folderRepository, times(1)).save(folder);
    }

    @Test
    @DisplayName("폴더 이름 변경 실패 - 존재하지 않음")
    void updateFolderName_notFound() {
        when(folderRepository.findById(701)).thenReturn(Optional.empty());

        assertThrows(NoResultException.class, () -> folderService.updateFolderName(701, "아무거나"));
        verify(folderRepository, never()).save(any(Folder.class));
    }

    @Test
    @DisplayName("폴더 이름 변경 실패 - 중복 이름 존재")
    void updateFolderName_conflict() {
        Folder folder = new Folder();
        folder.setName("기존이름");
        folder.setArchive(archive);
        ReflectionTestUtils.setField(folder, "id", 700);

        when(folderRepository.findById(700)).thenReturn(Optional.of(folder));
        when(folderRepository.findNamesForConflictCheck(archive.getId(), "보고서", "기존이름"))
                .thenReturn(List.of("보고서"));

        assertThrows(IllegalArgumentException.class,
                () -> folderService.updateFolderName(700, "보고서"));

        verify(folderRepository, never()).save(any(Folder.class));
    }
}
