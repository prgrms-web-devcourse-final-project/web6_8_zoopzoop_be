package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSourceServiceTest {

    @Mock private DataSourceService domain;
    @Mock private DataSourceRepository dataSourceRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private DataProcessorService dataProcessorService;

    @InjectMocks private PersonalDataSourceService app;

    private Archive archive(int id) { Archive a = new Archive(); ReflectionTestUtils.setField(a,"id",id); return a; }
    private Folder folder(int id, Archive a, boolean def) {
        Folder f = new Folder(); ReflectionTestUtils.setField(f, "id", id);
        f.setArchive(a); f.setDefault(def); return f;
    }
//    private PersonalArchive pa(Archive a) { PersonalArchive p = new PersonalArchive(); p.setArchive(a); return p; }

    // ---------------------- Create ----------------------
    @Test
    @DisplayName("create: folderId=0 → default 폴더로 위임")
    void create_defaultFolder() throws IOException {
        int memberId = 7;
        Archive a = archive(100);
        Folder defaultFolder = folder(55, a, true);
        when(folderRepository.findDefaultFolderByMemberId(memberId)).thenReturn(Optional.of(defaultFolder));

        String url = "https://m.sports.naver.com/wfootball/article/109/0005404750";

        when(dataProcessorService.process(eq(url), anyList()))
                .thenReturn(new DataSourceDto(
                        "제목", "요약",
                        LocalDate.of(2024, 9, 1),
                        url,
                        "http://img.jpg",
                        "NAVER",
                        Category.SPORTS,
                        List.of("tag1","tag2")
                ));

        // domain.create 검증 시에는 enum으로 비교
        when(domain.create(eq(55), argThat(c ->
                c.sourceUrl().equals(url)
                        && c.title().equals("제목")
                        && c.category() == Category.SPORTS   // enum 비교!
        ))).thenReturn(999);

        int id = app.create(memberId, url, 0, DataSourceService.CreateCmd.builder().build());

        assertThat(id).isEqualTo(999);
        verify(dataProcessorService).process(eq(url), anyList());
        verify(domain).create(eq(55), any());
    }




    // ---------------------- Delete ----------------------
    @Test
    @DisplayName("deleteOne: 소유 검증 후 hardDelete 위임")
    void deleteOne_ok() {
        int memberId = 7;
        when(dataSourceRepository.findByIdAndMemberId(3, memberId)).thenReturn(Optional.of(new DataSource()));

        int rs = app.deleteOne(memberId, 3);

        assertThat(rs).isEqualTo(3);
        verify(domain).hardDeleteOne(3);
    }

    @Test
    @DisplayName("deleteOne: 소유 아님/없음 → NoResultException")
    void deleteOne_notOwned() {
        when(dataSourceRepository.findByIdAndMemberId(9, 7)).thenReturn(Optional.empty());
        assertThrows(NoResultException.class, () -> app.deleteOne(7, 9));
        verify(domain, never()).hardDeleteOne(anyInt());
    }

    // ---------------------- Move ----------------------
    @Test
    @DisplayName("moveOne: targetFolderId=0 → default로 치환 후 위임")
    void moveOne_default() {
        int memberId = 7;
        Archive a = archive(100);
        Folder df = folder(55, a, true);
        when(dataSourceRepository.findByIdAndMemberId(1, memberId)).thenReturn(Optional.of(new DataSource()));
        when(folderRepository.findDefaultFolderByMemberId(memberId)).thenReturn(Optional.of(df));
        when(domain.moveOne(1, 55)).thenReturn(DataSourceService.MoveResult.builder().dataSourceId(1).folderId(55).build());

        var rs = app.moveOne(memberId, 1, 0);

        assertThat(rs.folderId()).isEqualTo(55);
        verify(domain).moveOne(1, 55);
    }

    // ---------------------- Update ----------------------
    @Test
    @DisplayName("update: 소유 검증 후 domain.update 위임")
    void update_ok() {
        int memberId = 7;
        when(dataSourceRepository.findByIdAndMemberId(10, memberId)).thenReturn(Optional.of(new DataSource()));
        when(domain.update(eq(10), any(DataSourceService.UpdateCmd.class))).thenReturn(10);

        int rs = app.update(memberId, 10, DataSourceService.UpdateCmd.builder().title(JsonNullable.of("X")).build());

        assertThat(rs).isEqualTo(10);
        verify(domain).update(eq(10), any(DataSourceService.UpdateCmd.class));
    }

    // ---------------------- Soft/Restore ----------------------
    @Test
    @DisplayName("softDelete: 일부 소유 아님 → 예외")
    void softDelete_mismatch() {
        int memberId = 7;
        when(dataSourceRepository.findExistingIdsInMember(memberId, List.of(1,2,3))).thenReturn(List.of(1,3));
        assertThrows(NoResultException.class, () -> app.softDelete(memberId, List.of(1,2,3)));
    }

    @Test
    @DisplayName("restore: 정상 위임")
    void restore_ok() {
        int memberId = 7;
        when(dataSourceRepository.findExistingIdsInMember(memberId, List.of(4,5))).thenReturn(List.of(4,5));
        when(domain.restoreMany(List.of(4,5))).thenReturn(2);

        int affected = app.restore(memberId, List.of(4,5));

        assertThat(affected).isEqualTo(2);
        verify(domain).restoreMany(List.of(4,5));
    }

    // ---------------------- Search ----------------------
    @Test
    @DisplayName("search: folderId=0 → default 치환 후 QRepo.search 호출")
//    @DisplayName("search: Personal 서비스는 도메인으로 위임한다")
    void search_delegateToDomain() {
        int memberId = 7;

        // given: 위임 결과를 미리 스텁
        when(domain.searchByMember(eq(memberId), any(DataSourceSearchCondition.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        var cond = DataSourceSearchCondition.builder()
                .folderId(0)
                .keyword("kw")
                .build();

        // when
        Page<DataSourceSearchItem> page = app.search(memberId, cond, Pageable.unpaged());

        // then
        assertThat(page).isEmpty();
        verify(domain).searchByMember(eq(memberId), any(DataSourceSearchCondition.class), any(Pageable.class));
    }

}
