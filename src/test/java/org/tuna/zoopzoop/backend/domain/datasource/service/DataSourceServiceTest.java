package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock private DataSourceRepository dataSourceRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private PersonalArchiveRepository personalArchiveRepository;
    @Mock private TagRepository tagRepository;
    @Mock private DataProcessorService dataProcessorService;

    @InjectMocks private DataSourceService dataSourceService;

    private DataSourceDto dataSourceDto(String title, String summary, LocalDate date, String url,
                              String img, String source, Category cat, List<String> tags) {
        return new DataSourceDto(title, summary, date, url, img, source, cat, tags);
    }

    // create
    @Test
    @DisplayName("폴더 생성 성공- folderId=null 이면 default 폴더에 자료 생성")
    void createDataSource_defaultFolder() throws IOException {
        int currentMemberId = 10;
        String sourceUrl = "https://example.com/a";

        // PersonalArchive 생성 시 Archive + default folder 자동 생성됨
        Member member = new Member("u1", "k-1", Provider.KAKAO, null);
        PersonalArchive pa = new PersonalArchive(member);

        when(personalArchiveRepository.findByMemberId(eq(currentMemberId)))
                .thenReturn(Optional.of(pa));

        Folder defaultFolder = new Folder("default");
        ReflectionTestUtils.setField(defaultFolder, "id", 321);

        when(folderRepository.findByArchiveIdAndIsDefaultTrue(anyInt()))
                .thenReturn(Optional.of(defaultFolder));

        when(tagRepository.findDistinctTagNamesByFolderId(eq(321)))
                .thenReturn(List.of("AI", "Spring"));

        DataSourceDto returnedDto = dataSourceDto(
                "제목A", "요약A", LocalDate.of(2025, 9, 1), sourceUrl,
                "https://img.example/a.png", "example.com", Category.IT,
                List.of("ML", "Infra")
        );
        doReturn(returnedDto)
                .when(dataProcessorService)
                .process(eq(sourceUrl), anyList());

        when(dataSourceRepository.save(any(DataSource.class)))
                .thenAnswer(inv -> {
                    DataSource ds = inv.getArgument(0);
                    ReflectionTestUtils.setField(ds, "id", 123);
                    return ds;
                });

        int id = dataSourceService.createDataSource(currentMemberId, sourceUrl, 0);
        assertThat(id).isEqualTo(123);
    }

    @Test
    @DisplayName("폴더 생성 성공- folderId가 주어지면 해당 폴더에 자료 생성")
    void createDataSource_specificFolder() throws IOException {
        // given
        int currentMemberId = 10;
        String sourceUrl = "https://example.com/b";
        Integer folderId = 77;

        Folder target = new Folder("target");
        ReflectionTestUtils.setField(target, "id", folderId);

        when(folderRepository.findById(eq(folderId))).thenReturn(Optional.of(target));

        when(tagRepository.findDistinctTagNamesByFolderId(eq(folderId)))
                .thenReturn(List.of("News", "Kotlin"));

        DataSourceDto returnedDto = dataSourceDto(
                "제목B", "요약B", LocalDate.of(2025, 9, 2), sourceUrl,
                "https://img.example/2.png", "tistory", Category.SCIENCE,
                List.of("ML", "Infra")
        );
        doReturn(returnedDto)
                .when(dataProcessorService)
                .process(eq(sourceUrl), anyList());

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
                dataSourceService.createDataSource(currentMemberId, "https://x", 0)
        );
    }

    //dataprocess 호출 테스트
    @Test
    @DisplayName("자료 생성 성공 - 지정 폴더 + 컨텍스트 태그 수집 + process 호출 + DTO 매핑/태그 영속화")
    void createDataSource_specificFolder_process_and_tags() throws Exception{
        // given
        int currentMemberId = 10;
        String sourceUrl = "https://example.com/b";
        Integer folderId = 77;

        Folder target = new Folder("target");
        ReflectionTestUtils.setField(target, "id", folderId);

        // 폴더 조회
        when(folderRepository.findById(eq(folderId))).thenReturn(Optional.of(target));
        // 컨텍스트 태그(distinct)
        when(tagRepository.findDistinctTagNamesByFolderId(eq(folderId)))
                .thenReturn(List.of("News", "Kotlin"));
        // process 결과 DTO
        DataSourceDto returnedDto = dataSourceDto(
                "제목B", "요약B", LocalDate.of(2025, 9, 2), sourceUrl,
                "https://img.example/2.png", "tistory", Category.SCIENCE,
                List.of("ML", "Infra")
        );
        when(dataProcessorService.process(eq(sourceUrl), anyList())).thenReturn(returnedDto);

        // save 캡처
        ArgumentCaptor<DataSource> dsCaptor = ArgumentCaptor.forClass(DataSource.class);
        when(dataSourceRepository.save(dsCaptor.capture()))
                .thenAnswer(inv -> {
                    DataSource ds = dsCaptor.getValue();
                    ReflectionTestUtils.setField(ds, "id", 456);
                    return ds;
                });

        // when
        int id = dataSourceService.createDataSource(currentMemberId, sourceUrl, folderId);

        // then
        assertThat(id).isEqualTo(456);

        DataSource saved = dsCaptor.getValue();
        assertThat(saved.getFolder().getId()).isEqualTo(folderId);
        assertThat(saved.getTitle()).isEqualTo("제목B");
        assertThat(saved.getSummary()).isEqualTo("요약B");
        assertThat(saved.getSourceUrl()).isEqualTo(sourceUrl);
        assertThat(saved.getImageUrl()).isEqualTo("https://img.example/2.png");
        assertThat(saved.getSource()).isEqualTo("tistory");
        assertThat(saved.getCategory()).isEqualTo(Category.SCIENCE);
        assertThat(saved.isActive()).isTrue();

        // 태그 매핑 검증
        assertThat(saved.getTags()).hasSize(2);
        assertThat(saved.getTags().stream().map(Tag::getTagName).toList())
                .containsExactlyInAnyOrder("ML", "Infra");
        assertThat(saved.getTags().stream().allMatch(t -> t.getDataSource() == saved)).isTrue();

        // 컨텍스트 태그가 process 에 전달되었는지 검증
        ArgumentCaptor<List<Tag>> ctxTagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataProcessorService).process(eq(sourceUrl), ctxTagsCaptor.capture());
        assertThat(ctxTagsCaptor.getValue().stream().map(Tag::getTagName).toList())
                .containsExactlyInAnyOrder("News", "Kotlin");

        verify(tagRepository).findDistinctTagNamesByFolderId(folderId);
        verifyNoInteractions(personalArchiveRepository); // 지정 폴더 경로이므로 호출 X
    }

    // collectDistinctTagsOfFolder - tag 추출 단위 테스트

    @Test
    @DisplayName("태그 컨텍스트 수집 성공 - 폴더 하위 자료 태그명 distinct → Tag 리스트 변환")
    void collectDistinctTagsOfFolder_success() {
        // given
        Integer folderId = 321;
        when(tagRepository.findDistinctTagNamesByFolderId(eq(folderId)))
                .thenReturn(List.of("AI", "Spring", "JPA"));

        @SuppressWarnings("unchecked")
        List<Tag> ctxTags = ReflectionTestUtils.invokeMethod(
                dataSourceService, "collectDistinctTagsOfFolder", folderId
        );

        // then
        assertThat(ctxTags).hasSize(3);
        assertThat(ctxTags.stream().map(Tag::getTagName).toList())
                .containsExactlyInAnyOrder("AI", "Spring", "JPA");
        assertThat(ctxTags.stream().allMatch(t -> t.getDataSource() == null)).isTrue();

        verify(tagRepository).findDistinctTagNamesByFolderId(folderId);
    }

    // buildDataSource 단위 테스트
    @Test
    @DisplayName("엔티티 빌드 성공 - process 호출 결과 DTO를 DataSource에 매핑 + 태그 양방향 세팅")
    void buildDataSource_maps_dto_and_tags() throws Exception{
        // given
        Folder folder = new Folder("f");
        ReflectionTestUtils.setField(folder, "id", 77);
        String url = "https://example.com/x";

        // 컨텍스트 태그(폴더 하위) - process 인자로만 사용됨
        List<Tag> context = List.of(new Tag("Ctx1"), new Tag("Ctx2"));

        // process 결과 DTO
        DataSourceDto returnedDto = dataSourceDto(
                "T", "S", LocalDate.of(2025, 9, 1), url,
                "https://img", "example.com", Category.IT,
                List.of("A", "B") // DTO 태그
        );
        when(dataProcessorService.process(eq(url), anyList())).thenReturn(returnedDto);

        // when (private 메서드 호출)
        DataSource ds = ReflectionTestUtils.invokeMethod(
                dataSourceService, "buildDataSource", folder, url, context
        );

        // then
        assertThat(ds).isNotNull();
        assertThat(ds.getFolder().getId()).isEqualTo(77);
        assertThat(ds.getTitle()).isEqualTo("T");
        assertThat(ds.getSummary()).isEqualTo("S");
        assertThat(ds.getSourceUrl()).isEqualTo(url);
        assertThat(ds.getImageUrl()).isEqualTo("https://img");
        assertThat(ds.getSource()).isEqualTo("example.com");
        assertThat(ds.getCategory()).isEqualTo(Category.IT);
        assertThat(ds.isActive()).isTrue();

        // 태그 매핑 검증
        assertThat(ds.getTags()).hasSize(2);
        assertThat(ds.getTags().stream().map(Tag::getTagName).toList())
                .containsExactlyInAnyOrder("A", "B");
        assertThat(ds.getTags().stream().allMatch(t -> t.getDataSource() == ds)).isTrue();

        // process 호출시 컨텍스트 태그 전달 검증
        ArgumentCaptor<List<Tag>> ctxTagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(dataProcessorService).process(eq(url), ctxTagsCaptor.capture());
        assertThat(ctxTagsCaptor.getValue().stream().map(Tag::getTagName).toList())
                .containsExactlyInAnyOrder("Ctx1", "Ctx2");
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

    // soft delete
    // soft delete
    @Test
    @DisplayName("소프트삭제 성공 - 전부 존재하면 isActive=false, deletedAt 업데이트")
    void softDelete_success() {
        Integer memberId = 10;
        List<Integer> ids = List.of(1, 2, 3);

        // 소유자 검증: 모두 존재한다고 가정
        when(dataSourceRepository.findExistingIdsInMember(memberId, ids)).thenReturn(ids);
        // 배치 업데이트 결과 개수 리턴
        when(dataSourceRepository.softDeleteAllByIds(eq(ids), any())).thenReturn(ids.size());

        int changed = dataSourceService.softDelete(memberId, ids);

        assertThat(changed).isEqualTo(3);
        verify(dataSourceRepository).findExistingIdsInMember(memberId, ids);
        verify(dataSourceRepository).softDeleteAllByIds(eq(ids), any());
    }

    @Test
    @DisplayName("소프트삭제 실패 - 요청 배열이 비어있으면 400")
    void softDelete_emptyIds_badRequest_service() {
        Integer memberId = 10;

        assertThrows(IllegalArgumentException.class, () ->
                dataSourceService.softDelete(memberId, List.of()));

        verifyNoInteractions(dataSourceRepository);
    }

    @Test
    @DisplayName("소프트삭제 실패 - 일부/전부 미존재 → 404")
    void softDelete_someNotFound() {
        Integer memberId = 10;
        List<Integer> ids = List.of(1, 2, 3);

        // 1,3만 존재한다고 가정 → 일부 누락
        when(dataSourceRepository.findExistingIdsInMember(memberId, ids)).thenReturn(List.of(1, 3));

        assertThrows(jakarta.persistence.NoResultException.class, () ->
                dataSourceService.softDelete(memberId, ids));

        verify(dataSourceRepository).findExistingIdsInMember(memberId, ids);
        verify(dataSourceRepository, never()).softDeleteAllByIds(anyList(), any());
    }



    // 복구
    @Test
    @DisplayName("복구 성공 - 전부 존재하면 isActive=true, deletedAt=null 업데이트")
    void restore_success() {
        Integer memberId = 7;
        List<Integer> ids = List.of(10, 20);

        when(dataSourceRepository.findExistingIdsInMember(memberId, ids)).thenReturn(ids);
        when(dataSourceRepository.restoreAllByIds(ids)).thenReturn(ids.size());

        int changed = dataSourceService.restore(memberId, ids);

        assertThat(changed).isEqualTo(2);
        verify(dataSourceRepository).findExistingIdsInMember(memberId, ids);
        verify(dataSourceRepository).restoreAllByIds(ids);
    }

    @Test
    @DisplayName("복구 실패 - 요청 배열이 비어있음 → 400")
    void restore_empty_badRequest_service() {
        Integer memberId = 7;

        assertThrows(IllegalArgumentException.class, () ->
                dataSourceService.restore(memberId, List.of()));

        verifyNoInteractions(dataSourceRepository);
    }

    @Test
    @DisplayName("복구 실패 - 일부/전부 미존재 → 404")
    void restore_someNotFound_service() {
        Integer memberId = 7;
        List<Integer> ids = List.of(10, 20);

        when(dataSourceRepository.findExistingIdsInMember(memberId, ids)).thenReturn(List.of(10));

        assertThrows(jakarta.persistence.NoResultException.class, () ->
                dataSourceService.restore(memberId, ids));

        verify(dataSourceRepository).findExistingIdsInMember(memberId, ids);
        verify(dataSourceRepository, never()).restoreAllByIds(anyList());
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
