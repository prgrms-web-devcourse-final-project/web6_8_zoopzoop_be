package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock private DataSourceRepository dataSourceRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private DataSourceQRepository dataSourceQRepository;

    @InjectMocks private DataSourceService service;

    private Folder folder(int id) {
        Folder f = new Folder();
        ReflectionTestUtils.setField(f, "id", id);
        return f;
    }

    private DataSource ds(int id, int folderId) {
        DataSource d = new DataSource();
        ReflectionTestUtils.setField(d, "id", id);
        d.setFolder(folder(folderId));
        d.setActive(true);
        return d;
    }

    // ---------------------- Create ----------------------
    @Test
    @DisplayName("create: 폴더 존재 → 성공, 태그도 저장")
    void create_ok() {
        when(folderRepository.findById(10)).thenReturn(Optional.of(folder(10)));
        ArgumentCaptor<DataSource> cap = ArgumentCaptor.forClass(DataSource.class);
        DataSource saved = ds(777, 10);
        when(dataSourceRepository.save(any(DataSource.class))).thenReturn(saved);

        var cmd = DataSourceService.CreateCmd.builder()
                .title("T").summary("S").source("src").sourceUrl("url")
                .imageUrl("img").category(Category.IT).dataCreatedDate(LocalDate.of(2024,1,1))
                .tags(List.of("a","b"))
                .build();

        int id = service.create(10, cmd);

        assertThat(id).isEqualTo(777);
        verify(dataSourceRepository).save(cap.capture());
        DataSource toSave = cap.getValue();
        assertThat(toSave.getTitle()).isEqualTo("T");
        assertThat(toSave.getTags()).hasSize(2);
    }

    @Test
    @DisplayName("create: 폴더 없음 → NoResultException")
    void create_folderNotFound() {
        when(folderRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(NoResultException.class, () -> service.create(99, DataSourceService.CreateCmd.builder().build()));
    }

    // ---------------------- Update ----------------------
    @Test
    @DisplayName("update: 부분 수정 + 태그 교체")
    void update_partial() {
        DataSource entity = ds(5, 1);
        when(dataSourceRepository.findById(5)).thenReturn(Optional.of(entity));

        var cmd = DataSourceService.UpdateCmd.builder()
                .title(JsonNullable.of("NEW"))
                .summary(JsonNullable.undefined()) // untouched
                .source(JsonNullable.of("NEWSRC"))
                .sourceUrl(JsonNullable.of("NEWURL"))
                .imageUrl(JsonNullable.of("IMG"))
                .category(JsonNullable.of(Category.IT))
                .tags(JsonNullable.of(List.of("x","y")))
                .build();

        int id = service.update(5, cmd);

        assertThat(id).isEqualTo(5);
        assertThat(entity.getTitle()).isEqualTo("NEW");
        assertThat(entity.getSource()).isEqualTo("NEWSRC");
        assertThat(entity.getCategory()).isEqualTo(Category.IT);
        assertThat(entity.getTags()).extracting(Tag::getTagName).containsExactlyInAnyOrder("x","y");
    }

    @Test
    @DisplayName("update: 자료 없음 → NoResultException")
    void update_notFound() {
        when(dataSourceRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(NoResultException.class, () -> service.update(1, DataSourceService.UpdateCmd.builder().build()));
    }

    // ---------------------- Move ----------------------
    @Test
    @DisplayName("moveOne: 다른 폴더로 이동")
    void moveOne_ok() {
        DataSource entity = ds(7, 1);
        when(dataSourceRepository.findById(7)).thenReturn(Optional.of(entity));
        when(folderRepository.findById(2)).thenReturn(Optional.of(folder(2)));

        var rs = service.moveOne(7, 2);

        assertThat(rs.dataSourceId()).isEqualTo(7);
        assertThat(rs.folderId()).isEqualTo(2);
        assertThat(entity.getFolder().getId()).isEqualTo(2);
    }

    @Test
    @DisplayName("moveMany: 일부 존재하지 않으면 예외")
    void moveMany_missing() {
        when(folderRepository.findById(10)).thenReturn(Optional.of(folder(10)));
        when(dataSourceRepository.findAllById(List.of(1,2,3)))
                .thenReturn(List.of(ds(1,1), ds(2,1))); // 3 누락
        assertThrows(NoResultException.class, () -> service.moveMany(List.of(1,2,3), 10));
    }

    // ---------------------- Hard Delete ----------------------
    @Test
    @DisplayName("hardDeleteOne: 삭제 성공")
    void hardDeleteOne_ok() {
        DataSource entity = ds(9, 1);
        when(dataSourceRepository.findById(9)).thenReturn(Optional.of(entity));
        service.hardDeleteOne(9);
        verify(dataSourceRepository).delete(entity);
    }

    @Test
    @DisplayName("hardDeleteMany: 일부 누락 → 예외")
    void hardDeleteMany_missing() {
        when(dataSourceRepository.findAllById(List.of(4,5)))
                .thenReturn(List.of(ds(4,1)));
        assertThrows(NoResultException.class, () -> service.hardDeleteMany(List.of(4,5)));
    }

    // ---------------------- Soft Delete / Restore ----------------------
    @Test
    @DisplayName("softDeleteMany: 활성인 것만 비활성 처리")
    void softDeleteMany_ok() {
        DataSource a = ds(1,1); a.setActive(true);
        DataSource b = ds(2,1); b.setActive(false);
        when(dataSourceRepository.findAllById(List.of(1,2))).thenReturn(List.of(a,b));

        int affected = service.softDeleteMany(List.of(1,2));

        assertThat(affected).isEqualTo(1);
        assertThat(a.isActive()).isFalse();
        assertThat(b.isActive()).isFalse();
    }

    @Test
    @DisplayName("restoreMany: 비활성인 것만 복원")
    void restoreMany_ok() {
        DataSource a = ds(1,1); a.setActive(false);
        DataSource b = ds(2,1); b.setActive(true);
        when(dataSourceRepository.findAllById(List.of(1,2))).thenReturn(List.of(a,b));

        int affected = service.restoreMany(List.of(1,2));

        assertThat(affected).isEqualTo(1);
        assertThat(a.isActive()).isTrue();
        assertThat(b.isActive()).isTrue();
    }

    // ---------------------- Search In Archive ----------------------
    @Test
    @DisplayName("searchByMember: folderId=0 → 개인 기본 폴더로 치환 + keyword/isActive 보존 + QRepo 위임")
    void searchByMember_folderZero_normalizeAndDelegate() {
        // given
        int memberId = 10;
        when(folderRepository.findDefaultFolderByMemberId(memberId))
                .thenReturn(Optional.of(folder(111)));

        var condIn = DataSourceSearchCondition.builder()
                .folderId(0)                    // normalize 대상
                .keyword("kw")                  // 보존되어야 함
                .isActive(null)                 // null이면 repo단에서 true로 처리(방어 로직)
                .build();

        // capture
        ArgumentCaptor<DataSourceSearchCondition> condCap = ArgumentCaptor.forClass(DataSourceSearchCondition.class);
        when(dataSourceQRepository.search(eq(memberId), condCap.capture(), any(Pageable.class)))
                .thenReturn(Page.empty());

        // when
        Page<DataSourceSearchItem> page = service.searchByMember(memberId, condIn, Pageable.ofSize(8));

        // then
        assertThat(page).isEmpty();
        DataSourceSearchCondition passed = condCap.getValue();
        assertThat(passed.getFolderId()).isEqualTo(111);   // 기본 폴더로 치환됨
        assertThat(passed.getKeyword()).isEqualTo("kw");   // 보존됨
        assertThat(passed.getIsActive()).isNull();         // 그대로 전달(Repo에서 true로 방어)
        verify(dataSourceQRepository).search(eq(memberId), any(), any());
    }

    @Test
    @DisplayName("searchByMember: folderId 지정 시 치환 없이 그대로 전달")
    void searchByMember_folderGiven_noNormalize() {
        int memberId = 10;

        var condIn = DataSourceSearchCondition.builder()
                .folderId(222)                  // 그대로 가야 함
                .isActive(Boolean.FALSE)        // 비활성만
                .build();

        ArgumentCaptor<DataSourceSearchCondition> condCap = ArgumentCaptor.forClass(DataSourceSearchCondition.class);
        when(dataSourceQRepository.search(eq(memberId), condCap.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<DataSourceSearchItem> page = service.searchByMember(memberId, condIn, Pageable.ofSize(8));

        assertThat(page.getContent()).isEmpty();
        DataSourceSearchCondition passed = condCap.getValue();
        assertThat(passed.getFolderId()).isEqualTo(222);   // 치환 없음
        assertThat(passed.getIsActive()).isFalse();        // 보존
    }

    @Test
    @DisplayName("searchByMember: 기본 폴더 없음 → NoResultException")
    void searchByMember_defaultMissing_throws() {
        int memberId = 10;
        when(folderRepository.findDefaultFolderByMemberId(memberId))
                .thenReturn(Optional.empty());

        var condIn = DataSourceSearchCondition.builder().folderId(0).build();

        assertThrows(NoResultException.class,
                () -> service.searchByMember(memberId, condIn, Pageable.ofSize(8)));
    }

    // ---------------------- searchByArchive ----------------------

    @Test
    @DisplayName("searchByArchive: folderId=0 → 공유 기본 폴더로 치환 + 위임")
    void searchByArchive_folderZero_normalizeAndDelegate() {
        int archiveId = 999;
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId))
                .thenReturn(Optional.of(folder(333)));

        var condIn = DataSourceSearchCondition.builder()
                .folderId(0)
                .keyword("shared-kw")
                .isActive(true)
                .build();

        ArgumentCaptor<DataSourceSearchCondition> condCap = ArgumentCaptor.forClass(DataSourceSearchCondition.class);
        when(dataSourceQRepository.searchInArchive(eq(archiveId), condCap.capture(), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<DataSourceSearchItem> page = service.searchByArchive(archiveId, condIn, Pageable.ofSize(8));

        assertThat(page).isEmpty();
        DataSourceSearchCondition passed = condCap.getValue();
        assertThat(passed.getFolderId()).isEqualTo(333);       // 기본 폴더로 치환됨
        assertThat(passed.getKeyword()).isEqualTo("shared-kw"); // 보존됨
        assertThat(passed.getIsActive()).isTrue();             // 보존됨
        verify(dataSourceQRepository).searchInArchive(eq(archiveId), any(), any());
    }

    @Test
    @DisplayName("searchByArchive: 기본 폴더 없음 → NoResultException")
    void searchByArchive_defaultMissing_throws() {
        int archiveId = 999;
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId))
                .thenReturn(Optional.empty());

        var condIn = DataSourceSearchCondition.builder().folderId(0).build();

        assertThrows(NoResultException.class,
                () -> service.searchByArchive(archiveId, condIn, Pageable.ofSize(8)));
    }
}
