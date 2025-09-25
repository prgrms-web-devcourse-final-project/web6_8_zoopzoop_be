package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock private DataSourceRepository dataSourceRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private PersonalArchiveRepository personalArchiveRepository;

    @InjectMocks private DataSourceService dataSourceService;

    // create
    @Test
    @DisplayName("폴더 생성 성공- folderId=null 이면 default 폴더에 자료 생성")
    void createDataSource_defaultFolder() {
        int currentMemberId = 10;
        String sourceUrl = "https://example.com/a";

        // PersonalArchive 생성 시 Archive + default folder 자동 생성됨
        Member member = new Member("u1", "k-1", Provider.KAKAO, null);
        PersonalArchive pa = new PersonalArchive(member);
        Integer archiveId = pa.getArchive().getId(); // 실제 id는 없지만, 아래 anyInt()로 받게 스텁함

        when(personalArchiveRepository.findByMemberId(eq(currentMemberId)))
                .thenReturn(Optional.of(pa));

        Folder defaultFolder = new Folder("default");
        // 리얼 구현은 archiveId 기준으로 찾으니 시그니처 맞추기
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(anyInt()))
                .thenReturn(Optional.of(defaultFolder));

        when(dataSourceRepository.save(any(DataSource.class)))
                .thenAnswer(inv -> {
                    DataSource ds = inv.getArgument(0);
                    ReflectionTestUtils.setField(ds, "id", 123);
                    return ds;
                });

        int id = dataSourceService.createDataSource(currentMemberId, sourceUrl, null);
        assertThat(id).isEqualTo(123);
    }


    @Test
    @DisplayName("폴더 생성 성공- folderId가 주어지면 해당 폴더에 자료 생성")
    void createDataSource_specificFolder() {
        // given
        int currentMemberId = 10;
        String sourceUrl = "https://example.com/b";
        Integer folderId = 77;

        Folder target = new Folder("target");
        // BaseEntity.id 는 protected setter → 리플렉션으로 주입
        org.springframework.test.util.ReflectionTestUtils.setField(target, "id", folderId);

        when(folderRepository.findById(eq(folderId))).thenReturn(Optional.of(target));

        // save(...) 시에 PK가 채워진 것처럼 반환
        when(dataSourceRepository.save(any(DataSource.class)))
                .thenAnswer(inv -> {
                    DataSource ds = inv.getArgument(0);
                    org.springframework.test.util.ReflectionTestUtils.setField(ds, "id", 456);
                    return ds;
                });

        // when
        int id = dataSourceService.createDataSource(currentMemberId, sourceUrl, folderId);

        // then
        assertThat(id).isEqualTo(456);
    }

    @Test
    @DisplayName("폴대 생성 실패 - folderId가 주어졌는데 대상 폴더가 없으면 예외")
    void createDataSource_folderNotFound() {
        // given
        Integer folderId = 999;

        when(folderRepository.findById(eq(folderId))).thenReturn(Optional.empty());

        // when / then
        assertThrows(NoResultException.class, () ->
                dataSourceService.createDataSource(1, "https://x", folderId)
        );
    }

    @Test
    @DisplayName("폴더 생성 실패 - folderId=null이고 default 폴더를 못 찾으면 예외")
    void createDataSource_defaultFolderNotFound() {
        // given
        int currentMemberId = 10;

        PersonalArchive pa = new PersonalArchive(new Member("u1","p", Provider.KAKAO,null));
        when(personalArchiveRepository.findByMemberId(eq(currentMemberId)))
                .thenReturn(Optional.of(pa));
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(anyInt()))
                .thenReturn(Optional.empty());

        // when / then
        assertThrows(NoResultException.class, () ->
                dataSourceService.createDataSource(currentMemberId, "https://x", null)
        );
    }

    // delete
    @Test
    @DisplayName("단건 삭제 성공 - 존재하는 자료 삭제 시 ID 반환")
    void deleteById_success() {
        // given
        int id = 123;
        DataSource mockData = new DataSource();
        when(dataSourceRepository.findById(id)).thenReturn(Optional.of(mockData));

        // when
        int deletedId = dataSourceService.deleteById(id);

        // then
        assertThat(deletedId).isEqualTo(id);
        verify(dataSourceRepository).delete(mockData);
    }

    @Test
    @DisplayName("단건 삭제 실패 - 자료가 존재하지 않으면 예외 발생")
    void deleteById_notFound() {
        // given
        int id = 999;
        when(dataSourceRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class, () -> dataSourceService.deleteById(id));
        verify(dataSourceRepository, never()).delete(any());
    }

    // deleteMany
    @Test
    @DisplayName("다건 삭제 성공 - 일괄 삭제")
    void deleteMany_success() {
        List<Integer> ids = List.of(1, 2, 3);
        when(dataSourceRepository.findExistingIds(ids)).thenReturn(ids);

        dataSourceService.deleteMany(ids);

        verify(dataSourceRepository).deleteAllByIdInBatch(ids);
    }

    @Test
    @DisplayName("다건 삭제 실패 - 요청 배열이 비어있음 → 400")
    void deleteMany_empty() {
        assertThrows(IllegalArgumentException.class, () -> dataSourceService.deleteMany(List.of()));
        verifyNoInteractions(dataSourceRepository);
    }

    @Test
    @DisplayName("다건 삭제 실패 - 일부 ID 미존재 → 404")
    void deleteMany_partialMissing() {
        List<Integer> ids = List.of(1, 2, 3);
        when(dataSourceRepository.findExistingIds(ids)).thenReturn(List.of(1, 3));

        assertThrows(NoResultException.class, () -> dataSourceService.deleteMany(ids));

        verify(dataSourceRepository, never()).deleteAllByIdInBatch(any());
    }


    // 자료 단건 이동
    @Test
    @DisplayName("단건 이동 성공: 지정 폴더로 이동")
    void moveOne_ok() {
        Integer memberId = 1, dsId = 10, fromId = 100, toId = 200;

        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", fromId);
        Folder to   = new Folder(); ReflectionTestUtils.setField(to, "id", toId);

        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", dsId);
        ds.setTitle("A"); ds.setFolder(from);

        when(dataSourceRepository.findById(dsId)).thenReturn(Optional.of(ds));
        when(folderRepository.findById(toId)).thenReturn(Optional.of(to));

        DataSourceService.MoveResult rs = dataSourceService.moveDataSource(memberId, dsId, toId);

        assertThat(rs.datasourceId()).isEqualTo(dsId);
        assertThat(rs.folderId()).isEqualTo(toId);
        assertThat(ds.getFolder().getId()).isEqualTo(toId);
    }

    @Test
    @DisplayName("단건이동 성공: 기본 폴더(null)로 이동")
    void moveOne_default_ok() {
        Integer memberId = 7, dsId = 1, fromId = 100, defaultId = 999;

        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", fromId);
        Folder defaultFolder = new Folder(); ReflectionTestUtils.setField(defaultFolder, "id", defaultId);

        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", dsId);
        ds.setTitle("문서A"); ds.setFolder(from);

        when(dataSourceRepository.findById(dsId)).thenReturn(Optional.of(ds));
        when(folderRepository.findDefaultFolderByMemberId(memberId))
                .thenReturn(Optional.of(defaultFolder));

        DataSourceService.MoveResult rs = dataSourceService.moveDataSource(memberId, dsId, null);

        assertThat(rs.folderId()).isEqualTo(defaultId);
        assertThat(ds.getFolder().getId()).isEqualTo(defaultId);
        verify(folderRepository).findDefaultFolderByMemberId(memberId);
    }

    @Test
    @DisplayName("단건 이동 성공: 동일 폴더(멱등)")
    void moveOne_idempotent() {
        Integer memberId = 1, dsId = 10, folderId = 100;

        Folder same = new Folder(); ReflectionTestUtils.setField(same, "id", folderId);

        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", dsId);
        ds.setTitle("A"); ds.setFolder(same);

        when(dataSourceRepository.findById(dsId)).thenReturn(Optional.of(ds));
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(same));

        DataSourceService.MoveResult rs = dataSourceService.moveDataSource(memberId, dsId, folderId);

        assertThat(rs.folderId()).isEqualTo(folderId);
        assertThat(ds.getFolder().getId()).isEqualTo(folderId);
    }

    @Test
    @DisplayName("단건 이동 실패: 자료 없음 → NoResultException")
    void moveOne_notFound_data() {
        when(dataSourceRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.moveDataSource(1, 1, 200))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 자료");
    }

    @Test
    @DisplayName("단건 이동 실패: 폴더 없음 → NoResultException")
    void moveOne_notFound_folder() {
        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", 100);

        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", 1);
        ds.setTitle("A"); ds.setFolder(from);

        when(dataSourceRepository.findById(1)).thenReturn(Optional.of(ds));
        when(folderRepository.findById(200)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.moveDataSource(1, 1, 200))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 폴더");
    }

    // 자료 다건 이동
    @Test
    @DisplayName("다건: folderId=null → 기본 폴더로 이동")
    void moveMany_default_ok() {
        Integer memberId = 7, defaultId = 999;

        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", 100);
        Folder defaultFolder = new Folder(); ReflectionTestUtils.setField(defaultFolder, "id", defaultId);

        DataSource a = new DataSource(); ReflectionTestUtils.setField(a, "id", 1); a.setTitle("A"); a.setFolder(from);
        DataSource b = new DataSource(); ReflectionTestUtils.setField(b, "id", 2); b.setTitle("B"); b.setFolder(from);

        when(folderRepository.findDefaultFolderByMemberId(memberId)).thenReturn(Optional.of(defaultFolder));
        when(dataSourceRepository.findAllByIdIn(List.of(1,2))).thenReturn(List.of(a,b));

        dataSourceService.moveDataSources(memberId, null, List.of(1,2));

        assertThat(a.getFolder().getId()).isEqualTo(defaultId);
        assertThat(b.getFolder().getId()).isEqualTo(defaultId);
        verify(folderRepository).findDefaultFolderByMemberId(memberId);
    }

    @Test
    @DisplayName("다건: folderId=null & 기본 폴더 없음 → NoResultException")
    void moveMany_default_missing() {
        when(folderRepository.findDefaultFolderByMemberId(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.moveDataSources(7, null, List.of(1)))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("기본 폴더");
    }

    @Test
    @DisplayName("다건: 지정 폴더로 이동")
    void moveMany_ok() {
        Integer toId = 200;
        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", 100);
        Folder to   = new Folder(); ReflectionTestUtils.setField(to, "id", toId);

        DataSource a = new DataSource(); ReflectionTestUtils.setField(a, "id", 1); a.setTitle("A"); a.setFolder(from);
        DataSource b = new DataSource(); ReflectionTestUtils.setField(b, "id", 2); b.setTitle("B"); b.setFolder(from);

        when(folderRepository.findById(toId)).thenReturn(Optional.of(to));
        when(dataSourceRepository.findAllByIdIn(List.of(1,2))).thenReturn(List.of(a,b));

        dataSourceService.moveDataSources(1, toId, List.of(1,2));

        assertThat(a.getFolder().getId()).isEqualTo(toId);
        assertThat(b.getFolder().getId()).isEqualTo(toId);
    }

    @Test
    @DisplayName("다건: 모두 동일 폴더 → 멱등")
    void moveMany_idempotent() {
        Integer toId = 200;
        Folder to = new Folder(); ReflectionTestUtils.setField(to, "id", toId);

        DataSource a = new DataSource(); ReflectionTestUtils.setField(a, "id", 1); a.setTitle("A"); a.setFolder(to);
        DataSource b = new DataSource(); ReflectionTestUtils.setField(b, "id", 2); b.setTitle("B"); b.setFolder(to);

        when(folderRepository.findById(toId)).thenReturn(Optional.of(to));
        when(dataSourceRepository.findAllByIdIn(List.of(1,2))).thenReturn(List.of(a,b));

        dataSourceService.moveDataSources(1, toId, List.of(1,2));

        verify(folderRepository).findById(toId);
        verify(dataSourceRepository).findAllByIdIn(List.of(1,2));
        verifyNoMoreInteractions(folderRepository, dataSourceRepository);
    }

    @Test
    @DisplayName("다건: 일부 미존재 → NoResultException")
    void moveMany_someNotFound() {
        Integer toId = 200;
        Folder to = new Folder(); ReflectionTestUtils.setField(to, "id", toId);

        DataSource a = new DataSource(); ReflectionTestUtils.setField(a, "id", 1); a.setTitle("A"); a.setFolder(new Folder());

        when(folderRepository.findById(toId)).thenReturn(Optional.of(to));
        when(dataSourceRepository.findAllByIdIn(List.of(1,2))).thenReturn(List.of(a)); // 2 없음

        assertThatThrownBy(() -> dataSourceService.moveDataSources(1, toId, List.of(1,2)))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 항목");
    }

    @Test
    @DisplayName("다건: 폴더 없음 → NoResultException")
    void moveMany_notFound_folder() {
        when(folderRepository.findById(200)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.moveDataSources(1, 200, List.of(1,2)))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 폴더");
    }

    @Test
    @DisplayName("다건: 요소 null → IllegalArgumentException")
    void moveMany_elementNull() {
        List<Integer> ids = Arrays.asList(1, null, 3); // ← null 허용

        assertThatThrownBy(() -> dataSourceService.moveDataSources(1, 200, ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("다건: 요청에 중복된 자료 ID 포함 → IllegalArgumentException")
    void moveMany_duplicatedIds_illegalArgument() {
        // given
        List<Integer> ids = List.of(1, 2, 2, 3); // 2가 중복

        // when & then
        assertThatThrownBy(() -> dataSourceService.moveDataSources(7, 200, ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 자료를 두 번 선택했습니다")
                .hasMessageContaining("2");

        // 리포지토리 호출 전 단계에서 막혀야 함
        verifyNoInteractions(folderRepository, dataSourceRepository);
    }

    @Test
    @DisplayName("다건: folderId=null + 중복된 자료 ID 포함 → IllegalArgumentException (default 조회 전 차단)")
    void moveMany_default_withDuplicatedIds_illegalArgument() {
        // given
        List<Integer> ids = List.of(5, 5); // 중복
        // when & then
        assertThatThrownBy(() -> dataSourceService.moveDataSources(7, null, ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 자료를 두 번 선택했습니다")
                .hasMessageContaining("5");

        verifyNoInteractions(folderRepository, dataSourceRepository);
    }

    // 자료 수정
    @Test
    @DisplayName("수정 성공: 제목과 요약 일부/전체 변경")
    void update_ok() {
        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", 7);
        ds.setTitle("old");
        ds.setSummary("old sum");

        when(dataSourceRepository.findById(anyInt()))
                .thenReturn(Optional.of(ds));

        Integer id = dataSourceService.updateDataSource(7, "new", null);

        assertThat(id).isEqualTo(7);
        assertThat(ds.getTitle()).isEqualTo("new");
        assertThat(ds.getSummary()).isEqualTo("old sum"); // summary 미전달 → 유지
    }

    @Test
    @DisplayName("수정 실패: 존재하지 않는 자료")
    void update_notFound() {
        when(dataSourceRepository.findById(anyInt()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.updateDataSource(1, "t", "s"))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 자료");
    }
}
