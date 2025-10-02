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
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FileSummary;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock private FolderRepository folderRepository;
    @Mock private DataSourceRepository dataSourceRepository;

    @InjectMocks private FolderService folderService;

    private Archive archive;

    @BeforeEach
    void setUp() {
        this.archive = new Archive();
        ReflectionTestUtils.setField(archive, "id", 10);
    }

    // ---------- Create ----------
    @Test
    @DisplayName("폴더 이름 중복 없음 → 그대로 생성")
    void generateUniqueName_noConflict() {
        when(folderRepository.findNamesForConflictCheck(eq(archive.getId()), anyString(), anyString()))
                .thenReturn(List.of());

        Folder folder = new Folder();
        folder.setArchive(archive);
        folder.setName("보고서");
        ReflectionTestUtils.setField(folder, "id", 1);

        when(folderRepository.save(any(Folder.class))).thenReturn(folder);

        FolderResponse rs = folderService.createFolder(archive, "보고서");

        assertThat(rs.folderName()).isEqualTo("보고서");
    }

    @Test
    @DisplayName("폴더 이름 중복 시 (1) 붙여 생성")
    void generateUniqueName_withConflict() {
        when(folderRepository.findNamesForConflictCheck(eq(archive.getId()), eq("보고서"), anyString()))
                .thenReturn(List.of("보고서"));

        Folder folder = new Folder();
        folder.setArchive(archive);
        folder.setName("보고서 (1)");
        ReflectionTestUtils.setField(folder, "id", 2);

        when(folderRepository.save(any(Folder.class))).thenReturn(folder);

        FolderResponse rs = folderService.createFolder(archive, "보고서");

        assertThat(rs.folderName()).isEqualTo("보고서 (1)");
    }

    // ---------- Delete ----------
    @Test
    @DisplayName("폴더 삭제 시 자료는 default 폴더로 이관 + soft delete")
    void deleteFolder_softDeleteAndMove() {
        Folder target = new Folder();
        target.setArchive(archive);
        target.setName("docs");
        ReflectionTestUtils.setField(target, "id", 100);

        Folder defaultFolder = new Folder();
        defaultFolder.setArchive(archive);
        defaultFolder.setName("default");
        defaultFolder.setDefault(true);
        ReflectionTestUtils.setField(defaultFolder, "id", 200);

        DataSource d1 = new DataSource(); ReflectionTestUtils.setField(d1, "id", 1); d1.setFolder(target); d1.setActive(true);
        DataSource d2 = new DataSource(); ReflectionTestUtils.setField(d2, "id", 2); d2.setFolder(target); d2.setActive(true);

        when(folderRepository.findByIdAndArchiveId(100, 10))
                .thenReturn(Optional.of(target));
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(10))
                .thenReturn(Optional.of(defaultFolder));
        when(dataSourceRepository.findAllByFolderId(100))
                .thenReturn(List.of(d1, d2));

        String deleted = folderService.deleteFolder(archive, 100);

        assertThat(deleted).isEqualTo("docs");
        assertThat(d1.isActive()).isFalse();
        assertThat(d1.getDeletedAt()).isNotNull();
        assertThat(d1.getFolder()).isEqualTo(defaultFolder);
        verify(folderRepository, times(1)).delete(target);
    }

    @Test
    @DisplayName("폴더 삭제 실패 - 존재하지 않는 폴더")
    void deleteFolder_notFound() {
        when(folderRepository.findByIdAndArchiveId(999, archive.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NoResultException.class, () -> folderService.deleteFolder(archive, 999));
    }

    // ---------- Update ----------
    @Test
    @DisplayName("폴더 이름 변경 성공")
    void updateFolderName_success() {
        Folder folder = new Folder();
        folder.setArchive(archive);
        folder.setName("기존");
        ReflectionTestUtils.setField(folder, "id", 300);

        when(folderRepository.findByIdAndArchiveId(300, archive.getId()))
                .thenReturn(Optional.of(folder));
        when(folderRepository.existsNameInArchiveExceptSelf(archive.getId(), "새이름", folder.getId()))
                .thenReturn(List.of());
        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

        String updated = folderService.updateFolderName(archive, 300, "새이름");

        assertThat(updated).isEqualTo("새이름");
        assertThat(folder.getName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("폴더 이름 변경 실패 - 중복 이름 존재")
    void updateFolderName_conflict() {
        Folder folder = new Folder();
        folder.setArchive(archive);
        folder.setName("기존");
        ReflectionTestUtils.setField(folder, "id", 301);

        when(folderRepository.findByIdAndArchiveId(301, archive.getId()))
                .thenReturn(Optional.of(folder));
        when(folderRepository.existsNameInArchiveExceptSelf(archive.getId(), "보고서", folder.getId()))
                .thenReturn(List.of("보고서"));

        assertThrows(IllegalArgumentException.class,
                () -> folderService.updateFolderName(archive, 301, "보고서"));
    }

    @Test
    @DisplayName("폴더 이름 변경 실패 - 폴더 없음")
    void updateFolderName_notFound() {
        when(folderRepository.findByIdAndArchiveId(302, archive.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NoResultException.class,
                () -> folderService.updateFolderName(archive, 302, "새이름"));
    }

    // ---------- Read ----------
    @Test
    @DisplayName("Archive 단위 폴더 목록 조회 성공")
    void getFolders_success() {
        Folder f1 = new Folder(); f1.setArchive(archive); f1.setName("default"); ReflectionTestUtils.setField(f1, "id", 1);
        Folder f2 = new Folder(); f2.setArchive(archive); f2.setName("docs");    ReflectionTestUtils.setField(f2, "id", 2);

        when(folderRepository.findByArchive(archive)).thenReturn(List.of(f1, f2));

        List<FolderResponse> rs = folderService.getFolders(archive);

        assertThat(rs).hasSize(2);
        assertThat(rs.get(0).folderName()).isEqualTo("default");
        assertThat(rs.get(1).folderName()).isEqualTo("docs");
    }

    @Test
    @DisplayName("Archive 단위 폴더 내 파일 목록 조회 성공")
    void getFilesInFolder_success() {
        Folder folder = new Folder();
        folder.setArchive(archive);
        folder.setName("docs");
        ReflectionTestUtils.setField(folder, "id", 400);

        DataSource d1 = new DataSource(); ReflectionTestUtils.setField(d1, "id", 1);
        d1.setTitle("spec.pdf"); d1.setSummary("요약"); d1.setFolder(folder);
        d1.setSourceUrl("http://src/a"); d1.setImageUrl("http://img/a");
        d1.setDataCreatedDate(LocalDate.now());
        d1.setTags(List.of(new Tag("tag1")));

        when(folderRepository.findByIdAndArchiveId(eq(400), eq(archive.getId())))
                .thenReturn(Optional.of(folder));
        when(dataSourceRepository.findAllByFolder(folder)).thenReturn(List.of(d1));

        FolderFilesDto dto = folderService.getFilesInFolder(archive, 400);

        assertThat(dto.files()).hasSize(1);
        FileSummary f = dto.files().getFirst();
        assertThat(f.title()).isEqualTo("spec.pdf");
    }

    @Test
    @DisplayName("Archive 단위 폴더 내 파일 목록 조회 실패 - 폴더 없음")
    void getFilesInFolder_notFound() {
        when(folderRepository.findByIdAndArchiveId(eq(999), eq(archive.getId())))
                .thenReturn(Optional.empty());

        assertThrows(NoResultException.class,
                () -> folderService.getFilesInFolder(archive, 999));
    }
}
