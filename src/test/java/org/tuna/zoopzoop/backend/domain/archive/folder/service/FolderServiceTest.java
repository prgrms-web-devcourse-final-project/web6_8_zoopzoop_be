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
import org.tuna.zoopzoop.backend.domain.datasource.dto.FileSummary;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
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
    @Mock private DataSourceRepository dataSourceRepository;

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

    @Test
    @DisplayName("default 폴더는 삭제할 수 없다")
    void deleteFolder_default_forbidden() {
        Folder defaultFolder = new Folder("default"); // isDefault=true
        ReflectionTestUtils.setField(defaultFolder, "id", 42);

        when(folderRepository.findById(42)).thenReturn(Optional.of(defaultFolder));

        assertThrows(IllegalArgumentException.class, () -> folderService.deleteFolder(42));
        verify(folderRepository, never()).delete(any());
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

    // Read: Personal Archive 내 폴더 목록
    @Test
    @DisplayName("개인 아카이브 폴더 목록 조회 - 성공")
    void getFoldersForPersonal_success() {
        // given
        Folder f1 = new Folder(); f1.setName("default"); f1.setArchive(archive); ReflectionTestUtils.setField(f1, "id", 1);
        Folder f2 = new Folder(); f2.setName("docs");    f2.setArchive(archive); ReflectionTestUtils.setField(f2, "id", 2);

        when(personalArchiveRepository.findByMemberId(1)).thenReturn(Optional.of(personalArchive));
        when(folderRepository.findByArchive(archive)).thenReturn(List.of(f1, f2));

        // when
        List<FolderResponse> rs = folderService.getFoldersForPersonal(1);

        // then
        assertThat(rs).hasSize(2);
        assertThat(rs.get(0).folderId()).isEqualTo(1);
        assertThat(rs.get(0).folderName()).isEqualTo("default");
        assertThat(rs.get(1).folderName()).isEqualTo("docs");
        verify(folderRepository, times(1)).findByArchive(archive);
    }

    // Read: 폴더 내 파일 목록
    @Test
    @DisplayName("폴더 내 파일 목록 조회")
    void getFilesInFolderForPersonal_success() {
        // given
        Integer folderId = 2;

        Folder folder = new Folder();
        folder.setName("docs");
        folder.setArchive(archive);
        ReflectionTestUtils.setField(folder, "id", folderId);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        DataSource d1 = new DataSource();
        ReflectionTestUtils.setField(d1, "id", 10);
        d1.setTitle("spec.pdf");
        d1.setFolder(folder);
        d1.setSummary("요약 A");
        d1.setSourceUrl("http://src/a");
        d1.setImageUrl("http://img/a");
        d1.setTags(List.of(new Tag("tag1"), new Tag("tag2")));

        DataSource d2 = new DataSource();
        ReflectionTestUtils.setField(d2, "id", 11);
        d2.setTitle("notes.txt");
        d2.setFolder(folder);
        d2.setSummary("요약 B");
        d2.setSourceUrl("http://src/b");
        d2.setImageUrl("http://img/b");
        d2.setTags(List.of());

        when(dataSourceRepository.findAllByFolder(folder)).thenReturn(List.of(d1, d2));

        // when
        FolderFilesDto dto = folderService.getFilesInFolderForPersonal(1, folderId);

        // then
        assertThat(dto.files()).hasSize(2);
        FileSummary f0 = dto.files().get(0);
        assertThat(f0.dataSourceId()).isEqualTo(10);
        assertThat(f0.title()).isEqualTo("spec.pdf");
        assertThat(f0.summary()).isEqualTo("요약 A");
        assertThat(f0.sourceUrl()).isEqualTo("http://src/a");
        assertThat(f0.imageUrl()).isEqualTo("http://img/a");
        assertThat(f0.tags()).extracting(Tag::getTagName).containsExactly("tag1", "tag2");
    }

    @Test
    @DisplayName("폴더 내 파일 목록 조회 - 폴더가 없으면 예외 발생")
    void getFilesInFolderForPersonal_notFound() {
        // given
        Integer folderId = 999;
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class,
                () -> folderService.getFilesInFolderForPersonal(1, folderId));

        // then(verify)
        verify(dataSourceRepository, never()).findAllByFolder(any());
    }

}
