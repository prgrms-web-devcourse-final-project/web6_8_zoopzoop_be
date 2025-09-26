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

        when(personalArchiveRepository.findByMemberId(eq(currentMemberId)))
                .thenReturn(Optional.of(pa));

        Folder defaultFolder = new Folder("default");
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
        ReflectionTestUtils.setField(target, "id", folderId);

        when(folderRepository.findById(eq(folderId))).thenReturn(Optional.of(target));

        when(dataSourceRepository.save(any(DataSource.class)))
                .thenAnswer(inv -> {
                    DataSource ds = inv.getArgument(0);
                    ReflectionTestUtils.setField(ds, "id", 456);
                    return ds;
                });

        // when
        int id = dataSourceService.createDataSource(currentMemberId, sourceUrl, folderId);

        // then
        assertThat(id).isEqualTo(456);
    }

    @Test
    @DisplayName("폴더 생성 실패 - folderId가 주어졌는데 대상 폴더가 없으면 예외")
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
    @DisplayName("단건 삭제 성공 - 존재하는 자료 삭제 시 ID 반환 (member 소유 확인)")
    void deleteById_success() {
        // given
        int memberId = 5;
        int id = 123;
        DataSource mockData = new DataSource();

        // when
        when(dataSourceRepository.findByIdAndMemberId(id, memberId)).thenReturn(Optional.of(mockData));

        int deletedId = dataSourceService.deleteById(memberId, id);

        // then
        assertThat(deletedId).isEqualTo(id);
        verify(dataSourceRepository).delete(mockData);
    }

    @Test
    @DisplayName("단건 삭제 실패 - 자료가 존재하지 않으면 예외 발생")
    void deleteById_notFound() {
        // given
        int memberId = 5;
        int id = 999;
        when(dataSourceRepository.findByIdAndMemberId(id, memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoResultException.class, () -> dataSourceService.deleteById(memberId, id));
        verify(dataSourceRepository, never()).delete(any());
    }

    // deleteMany
    @Test
    @DisplayName("다건 삭제 성공 - 일괄 삭제")
    void deleteMany_success() {
        Integer memberId = 2;
        List<Integer> ids = List.of(1, 2, 3);

        when(dataSourceRepository.findExistingIdsInMember(memberId, ids)).thenReturn(ids);

        dataSourceService.deleteMany(memberId, ids);

        verify(dataSourceRepository).deleteAllByIdInBatch(ids);
    }

    @Test
    @DisplayName("다건 삭제 실패 - 요청 배열이 비어있음 → 400")
    void deleteMany_empty() {
        Integer memberId = 2;
        assertThrows(IllegalArgumentException.class, () -> dataSourceService.deleteMany(memberId, List.of()));
        verifyNoInteractions(dataSourceRepository);
    }

    @Test
    @DisplayName("다건 삭제 실패 - 일부 ID 미존재 → 404")
    void deleteMany_partialMissing() {
        Integer memberId = 2;
        List<Integer> ids = List.of(1, 2, 3);
        when(dataSourceRepository.findExistingIdsInMember(memberId, ids)).thenReturn(List.of(1, 3));

        assertThrows(NoResultException.class, () -> dataSourceService.deleteMany(memberId, ids));

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

        when(dataSourceRepository.findByIdAndMemberId(dsId, memberId)).thenReturn(Optional.of(ds));
        when(folderRepository.findById(toId)).thenReturn(Optional.of(to));

        DataSourceService.MoveResult rs = dataSourceService.moveDataSource(memberId, dsId, toId);

        assertThat(rs.datasourceId()).isEqualTo(dsId);
        assertThat(rs.folderId()).isEqualTo(toId);
        assertThat(ds.getFolder().getId()).isEqualTo(toId);
    }

    @Test
    @DisplayName("단건 이동 성공: 기본 폴더(null) -> 200")
    void moveOne_default_ok() {
        Integer memberId = 7, dsId = 1, fromId = 100, defaultId = 999;

        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", fromId);
        Folder defaultFolder = new Folder(); ReflectionTestUtils.setField(defaultFolder, "id", defaultId);

        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", dsId);
        ds.setTitle("문서A"); ds.setFolder(from);

        when(dataSourceRepository.findByIdAndMemberId(dsId, memberId)).thenReturn(Optional.of(ds));
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

        when(dataSourceRepository.findByIdAndMemberId(dsId, memberId)).thenReturn(Optional.of(ds));
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(same));

        DataSourceService.MoveResult rs = dataSourceService.moveDataSource(memberId, dsId, folderId);

        assertThat(rs.folderId()).isEqualTo(folderId);
        assertThat(ds.getFolder().getId()).isEqualTo(folderId);
    }

    @Test
    @DisplayName("단건 이동 실패: 자료 없음 → NoResultException (소유자 검증)")
    void moveOne_notFound_data() {
        Integer memberId = 1, dsId = 1;
        when(dataSourceRepository.findByIdAndMemberId(dsId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.moveDataSource(memberId, dsId, 200))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 자료");
    }

    @Test
    @DisplayName("단건 이동 실패: 폴더 없음 → NoResultException")
    void moveOne_notFound_folder() {
        Integer memberId = 1;
        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", 100);

        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", 1);
        ds.setTitle("A"); ds.setFolder(from);

        when(dataSourceRepository.findByIdAndMemberId(1, memberId)).thenReturn(Optional.of(ds));
        when(folderRepository.findById(200)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.moveDataSource(memberId, 1, 200))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 폴더");
    }

    // 자료 다건 이동
    @Test
    @DisplayName("다건 이동 성공: 지정 폴더로 이동")
    void moveMany_ok() {
        Integer memberId = 1;
        Integer toId = 200;
        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", 100);
        Folder to   = new Folder(); ReflectionTestUtils.setField(to, "id", toId);

        DataSource a = new DataSource(); ReflectionTestUtils.setField(a, "id", 1); a.setTitle("A"); a.setFolder(from);
        DataSource b = new DataSource(); ReflectionTestUtils.setField(b, "id", 2); b.setTitle("B"); b.setFolder(from);

        when(folderRepository.findById(toId)).thenReturn(Optional.of(to));
        // 소유자 검증: member 소유인 id들 반환
        when(dataSourceRepository.findExistingIdsInMember(memberId, List.of(1,2))).thenReturn(List.of(1,2));
        when(dataSourceRepository.findAllByIdIn(List.of(1,2))).thenReturn(List.of(a,b));

        dataSourceService.moveDataSources(memberId, toId, List.of(1,2));

        assertThat(a.getFolder().getId()).isEqualTo(toId);
        assertThat(b.getFolder().getId()).isEqualTo(toId);
    }

    @Test
    @DisplayName("다건 이동 성공: folderId=null → 기본 폴더로 이동")
    void moveMany_default_ok() {
        Integer memberId = 7, defaultId = 999;

        Folder from = new Folder(); ReflectionTestUtils.setField(from, "id", 100);
        Folder defaultFolder = new Folder(); ReflectionTestUtils.setField(defaultFolder, "id", defaultId);

        DataSource a = new DataSource(); ReflectionTestUtils.setField(a, "id", 1); a.setTitle("A"); a.setFolder(from);
        DataSource b = new DataSource(); ReflectionTestUtils.setField(b, "id", 2); b.setTitle("B"); b.setFolder(from);

        when(folderRepository.findDefaultFolderByMemberId(memberId)).thenReturn(Optional.of(defaultFolder));
        when(dataSourceRepository.findExistingIdsInMember(memberId, List.of(1,2))).thenReturn(List.of(1,2));
        when(dataSourceRepository.findAllByIdIn(List.of(1,2))).thenReturn(List.of(a,b));

        dataSourceService.moveDataSources(memberId, null, List.of(1,2));

        assertThat(a.getFolder().getId()).isEqualTo(defaultId);
        assertThat(b.getFolder().getId()).isEqualTo(defaultId);
        verify(folderRepository).findDefaultFolderByMemberId(memberId);
    }

    @Test
    @DisplayName("다건 이동 실패: folderId=null & 기본 폴더 없음 → NoResultException")
    void moveMany_default_missing() {
        when(folderRepository.findDefaultFolderByMemberId(7)).thenReturn(Optional.empty());
        // 멤버 소유 검증 전이라도 default 조회에서 예외 발생
        assertThatThrownBy(() -> dataSourceService.moveDataSources(7, null, List.of(1)))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("기본 폴더");
    }

    @Test
    @DisplayName("다건 이동 실패: 일부 미존재 → NoResultException (소유자 검증 실패)")
    void moveMany_someNotFound() {
        Integer memberId = 1;
        Integer toId = 200;
        Folder to = new Folder(); ReflectionTestUtils.setField(to, "id", toId);

        DataSource a = new DataSource(); ReflectionTestUtils.setField(a, "id", 1); a.setTitle("A"); a.setFolder(new Folder());

        when(folderRepository.findById(toId)).thenReturn(Optional.of(to));
        // 소유자 검증에서 일부만 리턴
        when(dataSourceRepository.findExistingIdsInMember(memberId, List.of(1,2))).thenReturn(List.of(1));

        assertThatThrownBy(() -> dataSourceService.moveDataSources(memberId, toId, List.of(1,2)))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않거나 소유자가 다른 자료 ID 포함");
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
        List<Integer> ids = Arrays.asList(1, null, 3);

        assertThatThrownBy(() -> dataSourceService.moveDataSources(1, 200, ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("다건: 요청에 중복된 자료 ID 포함 → IllegalArgumentException")
    void moveMany_duplicatedIds_illegalArgument() {
        List<Integer> ids = List.of(1, 2, 2, 3); // 2가 중복

        assertThatThrownBy(() -> dataSourceService.moveDataSources(7, 200, ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 자료를 두 번 선택했습니다")
                .hasMessageContaining("2");

        verifyNoInteractions(folderRepository, dataSourceRepository);
    }

    @Test
    @DisplayName("다건: folderId=null + 중복된 자료 ID 포함 → IllegalArgumentException (default 조회 전 차단)")
    void moveMany_default_withDuplicatedIds_illegalArgument() {
        List<Integer> ids = List.of(5, 5); // 중복

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
        Integer memberId = 3;
        DataSource ds = new DataSource();
        ReflectionTestUtils.setField(ds, "id", 7);
        ds.setTitle("old");
        ds.setSummary("old sum");

        when(dataSourceRepository.findByIdAndMemberId(eq(7), eq(memberId)))
                .thenReturn(Optional.of(ds));

        Integer id = dataSourceService.updateDataSource(memberId, 7, "new", null);

        assertThat(id).isEqualTo(7);
        assertThat(ds.getTitle()).isEqualTo("new");
        assertThat(ds.getSummary()).isEqualTo("old sum"); // summary 미전달 → 유지
    }

    @Test
    @DisplayName("수정 실패: 존재하지 않는 자료")
    void update_notFound() {
        Integer memberId = 3;
        when(dataSourceRepository.findByIdAndMemberId(anyInt(), eq(memberId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataSourceService.updateDataSource(memberId, 1, "t", "s"))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("존재하지 않는 자료");
    }
}
