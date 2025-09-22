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
}
