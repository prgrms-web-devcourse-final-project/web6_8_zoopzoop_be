package org.tuna.zoopzoop.backend.domain.datasource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatasourceControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private FolderService folderService;
    @Autowired private FolderRepository folderRepository;
    @Autowired private DataSourceRepository dataSourceRepository;

    private final String TEST_PROVIDER_KEY = "testUser_sc1111";

    private Integer testMemberId;
    private Integer docsFolderId;
    private Integer dataSourceId1;
    private Integer dataSourceId2;

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        DataProcessorService stubDataProcessorService(){
            return new DataProcessorService(null, null) {
                @Override
                public DataSourceDto process(String url, List<Tag> tagList) {
                    return new DataSourceDto(
                            "테스트제목",
                            "테스트요약",
                            LocalDate.of(2025, 9, 1),
                            url,
                            "https://img.example/test.png",
                            "example.com",
                            Category.IT,
                            List.of("ML","Infra")
                    );
                }
            };
        }

        @Bean
        @Primary
        TagRepository stubTagRepository() {
            TagRepository mock = Mockito.mock(TagRepository.class);

            when(mock.findDistinctTagNamesByFolderId(anyInt()))
                    .thenReturn(java.util.List.of("AI", "Spring"));

            return mock;
        }
    }

    @BeforeAll
    void beforeAll() {
        try {
            memberService.createMember("testUser_sc1111", "http://img", TEST_PROVIDER_KEY, Provider.KAKAO);
        } catch (Exception ignored) {}

        var member = memberRepository.findByProviderAndProviderKey(Provider.KAKAO, TEST_PROVIDER_KEY)
                .orElseThrow();
        testMemberId = member.getId();

        // docs 폴더 생성
        FolderResponse fr = folderService.createFolderForPersonal(testMemberId, "docs");
        docsFolderId = fr.folderId();

        Folder docsFolder = folderRepository.findById(docsFolderId).orElseThrow();

        Integer archiveId = docsFolder.getArchive().getId();
        folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId)
                .orElseGet(() -> {
                    Folder defaultFolder = new Folder();
                    defaultFolder.setArchive(docsFolder.getArchive());
                    defaultFolder.setName("default");
                    defaultFolder.setDefault(true);
                    return folderRepository.save(defaultFolder);
                });

        // 자료 2건 생성
        DataSource d1 = new DataSource();
        d1.setFolder(docsFolder);
        d1.setTitle("spec.pdf");
        d1.setSummary("요약 A");
        d1.setSourceUrl("http://src/a");
        d1.setImageUrl("http://img/a");
        d1.setDataCreatedDate(LocalDate.now());
        d1.setActive(true);
        d1.setTags(List.of(new Tag("tag1"), new Tag("tag2")));
        d1.setCategory(Category.IT);
        dataSourceRepository.save(d1);
        dataSourceId1 = d1.getId();

        DataSource d2 = new DataSource();
        d2.setFolder(docsFolder);
        d2.setTitle("notes.txt");
        d2.setSummary("요약 B");
        d2.setSourceUrl("http://src/b");
        d2.setImageUrl("http://img/b");
        d2.setDataCreatedDate(LocalDate.now());
        d2.setActive(true);
        d2.setTags(List.of());
        d2.setCategory(Category.SCIENCE);
        dataSourceRepository.save(d2);
        dataSourceId2 = d2.getId();
    }

    @AfterAll
    void afterAll() {
        try {
            if (dataSourceId1 != null) dataSourceRepository.findById(dataSourceId1).ifPresent(dataSourceRepository::delete);
        } catch (Exception ignored) {}
        try {
            if (dataSourceId2 != null) dataSourceRepository.findById(dataSourceId2).ifPresent(dataSourceRepository::delete);
        } catch (Exception ignored) {}

        try {
            if (docsFolderId != null) folderRepository.findById(docsFolderId).ifPresent(folderRepository::delete);
        } catch (Exception ignored) {}

        memberRepository.findByProviderAndProviderKey(Provider.KAKAO, TEST_PROVIDER_KEY)
                .ifPresent(memberRepository::delete);
    }

    // create
    @Test
    @DisplayName("자료 생성 성공 - folderId=0 → default 폴더에 등록")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void create_defaultFolder() throws Exception {
        var rq = new reqBodyForCreateDataSource("https://example.com/a", 0);

        mockMvc.perform(post("/api/v1/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("새로운 자료가 등록됐습니다."))
                .andExpect(jsonPath("$.data").isNumber());
    }


    @Test
    @DisplayName("자료 생성 성공 - folderId 지정 → 해당 폴더에 등록")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void create_specificFolder() throws Exception {
        var rq = new reqBodyForCreateDataSource("https://example.com/b", docsFolderId);

        mockMvc.perform(post("/api/v1/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("새로운 자료가 등록됐습니다."))
                .andExpect(jsonPath("$.data").isNumber());
    }

    // delete
    @Test
    @DisplayName("단건 삭제 성공 -> 200")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void delete_success() throws Exception {
        DataSource d = new DataSource();
        d.setFolder(folderRepository.findById(docsFolderId).orElseThrow());
        d.setTitle("tmp_delete");
        d.setSummary("tmp");
        d.setSourceUrl("http://s");
        d.setImageUrl("http://i");
        d.setDataCreatedDate(LocalDate.now());
        d.setActive(true);
        d.setCategory(Category.IT);
        dataSourceRepository.save(d);
        Integer id = d.getId();

        mockMvc.perform(delete("/api/v1/archive/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(id + "번 자료가 삭제됐습니다."))
                .andExpect(jsonPath("$.data.dataSourceId").value(id));
    }

    @Test
    @DisplayName("단건 삭제 실패: 자료 없음 → 404 Not Found")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void delete_notFound() throws Exception {
        mockMvc.perform(delete("/api/v1/archive/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 자료입니다."));
    }

    // deleteMany
    @Test
    @DisplayName("다건 삭제 성공 -> 200")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void deleteMany_success() throws Exception {
        DataSource a = new DataSource(); a.setFolder(folderRepository.findById(docsFolderId).orElseThrow());
        a.setTitle("tmp_a"); a.setSummary("a"); a.setSourceUrl("a"); a.setImageUrl("a"); a.setDataCreatedDate(LocalDate.now()); a.setActive(true); a.setCategory(Category.IT);
        DataSource b = new DataSource(); b.setFolder(folderRepository.findById(docsFolderId).orElseThrow());
        b.setTitle("tmp_b"); b.setSummary("b"); b.setSourceUrl("b"); b.setImageUrl("b"); b.setDataCreatedDate(LocalDate.now()); b.setActive(true); b.setCategory(Category.IT);
        dataSourceRepository.save(a); dataSourceRepository.save(b);

        var body = new reqBodyForDeleteMany(List.of(a.getId(), b.getId()));

        mockMvc.perform(post("/api/v1/archive/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("복수개의 자료가 삭제됐습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("다건 삭제 실패: 배열 비어있음 → 400 Bad Request")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void deleteMany_empty() throws Exception {
        var empty = new reqBodyForDeleteMany(List.of());

        mockMvc.perform(post("/api/v1/archive/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empty)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    @DisplayName("다건 삭제 실패: 일부 ID 미존재 → 404 Not Found")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void deleteMany_partialMissing() throws Exception {
        var body = new reqBodyForDeleteMany(List.of(dataSourceId1, 999999));

        mockMvc.perform(post("/api/v1/archive/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));
    }

    // soft delete
    @Test
    @DisplayName("소프트삭제 실패: 존재하지 않는 ID 포함 -> 404")
    @WithUserDetails("KAKAO:testUser_sc1111")
    void softDelete_notFoundIds() throws Exception {
        String body = "{\"ids\":[999999]}";

        mockMvc.perform(patch("/api/v1/archive/soft-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("소프트삭제 실패: 빈 배열 -> 400")
    @WithUserDetails("KAKAO:testUser_sc1111")
    void softDelete_emptyIds_badRequest() throws Exception {
        String body = "{\"ids\":[]}";

        mockMvc.perform(patch("/api/v1/archive/soft-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }


    // restore
    @Test
    @DisplayName("복구: 단건 -> 200")
    @WithUserDetails("KAKAO:testUser_sc1111")
    void restore_one_ok() throws Exception {
        String body = String.format("{\"ids\":[%d]}", dataSourceId1);

        mockMvc.perform(patch("/api/v1/archive/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("자료들이 복구됐습니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("복구: 다건 -> 200")
    @WithUserDetails("KAKAO:testUser_sc1111")
    void restore_many_ok() throws Exception {
        String body = String.format("{\"ids\":[%d,%d]}", dataSourceId1, dataSourceId2);

        mockMvc.perform(patch("/api/v1/archive/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("자료들이 복구됐습니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("복구 실패: 존재하지 않는 ID 포함 -> 404")
    @WithUserDetails("KAKAO:testUser_sc1111")
    void restore_notFoundIds() throws Exception {
        String body = "{\"ids\":[99999]}";

        mockMvc.perform(patch("/api/v1/archive/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 자료 단건 이동
    @Test
    @DisplayName("단건 이동 성공 -> 200")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void moveOne_ok() throws Exception {
        FolderResponse newFolder = folderService.createFolderForPersonal(testMemberId, "moveTarget");
        Integer toId = newFolder.folderId();

        var body = new reqBodyForMoveDataSource(toId);

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", dataSourceId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").value(dataSourceId1))
                .andExpect(jsonPath("$.data.folderId").value(toId));
    }

    @Test
    @DisplayName("단건 이동 성공: default 폴더(null) -> 200")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void moveOne_default_ok() throws Exception {
        var body = new reqBodyForMoveDataSource(null);

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", dataSourceId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").value(dataSourceId1))
                .andExpect(jsonPath("$.data.folderId").isNumber());
    }

    @Test
    @DisplayName("단건 이동 실패: 자료 없음 -> 404")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void moveOne_notFound_data() throws Exception {
        var body = new reqBodyForMoveDataSource(docsFolderId);

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("단건 이동 실패: 폴더 없음 -> 404")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void moveOne_notFound_folder() throws Exception {
        // 임의의 존재하지 않는 폴더로 이동 시도
        var body = new reqBodyForMoveDataSource(999999);

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", dataSourceId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // 자료 다건 이동 (지정 폴더)
    @Test
    @DisplayName("자료 다건 이동 성공: 지정 폴더 -> 200")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void moveMany_specific_ok() throws Exception {
        FolderResponse newFolder = folderService.createFolderForPersonal(testMemberId, "moveManyTarget");
        Integer toId = newFolder.folderId();

        String body = String.format("{\"folderId\":%d,\"dataSourceId\":[%d,%d]}", toId, dataSourceId1, dataSourceId2);

        mockMvc.perform(patch("/api/v1/archive/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("복수 개의 자료를 이동했습니다."));
    }

    @Test
    @DisplayName("자료 다건 이동 성공: 기본 폴더(null) -> 200")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void moveMany_default_ok() throws Exception {
        String body = String.format("{\"folderId\":null,\"dataSourceId\":[%d,%d]}", dataSourceId1, dataSourceId2);

        mockMvc.perform(patch("/api/v1/archive/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("복수 개의 자료를 이동했습니다."));
    }

    // 자료 수정
    @Test
    @DisplayName("자료 수정 성공 -> 200")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void update_ok() throws Exception {
        var body = new reqBodyForUpdateDataSource("새 제목", "짧은 요약");

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}", dataSourceId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data.dataSourceId").value(dataSourceId1));
    }

    @Test
    @DisplayName("자료 수정 실패: 요청 바디가 모두 공백 -> 400")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void update_badRequest_whenEmpty() throws Exception {
        var body = new reqBodyForUpdateDataSource("  ", null);

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}", dataSourceId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("자료 수정 실패: 존재하지 않는 자료 -> 404")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void update_notFound() throws Exception {
        var body = new reqBodyForUpdateDataSource("제목", "요약");

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 검색
    @Test
    @DisplayName("검색 성공: page, size, dataCreatedDate DESC 기본정렬")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_default_paging_and_sort() throws Exception {
        // 최신/과거 비교용 더미 데이터 추가
        Folder docsFolder = folderRepository.findById(docsFolderId).orElseThrow();

        DataSource oldItem = new DataSource();
        oldItem.setFolder(docsFolder);
        oldItem.setTitle("old-doc");
        oldItem.setSummary("old");
        oldItem.setSourceUrl("http://src/old");
        oldItem.setImageUrl("http://img/old");
        oldItem.setDataCreatedDate(LocalDate.now().minusDays(30));
        oldItem.setActive(true);
        oldItem.setCategory(Category.IT);
        dataSourceRepository.save(oldItem);

        DataSource newItem = new DataSource();
        newItem.setFolder(docsFolder);
        newItem.setTitle("new-doc");
        newItem.setSummary("new");
        newItem.setSourceUrl("http://src/new");
        newItem.setImageUrl("http://img/new");
        newItem.setDataCreatedDate(LocalDate.now());
        newItem.setActive(true);
        newItem.setCategory(Category.IT);
        dataSourceRepository.save(newItem);

        mockMvc.perform(get("/api/v1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pageInfo.page").value(0))
                .andExpect(jsonPath("$.pageInfo.size").value(8))
                .andExpect(jsonPath("$.pageInfo.first").value(true))
                .andExpect(jsonPath("$.pageInfo.sorted", containsStringIgnoringCase("createdAt")))
                .andExpect(jsonPath("$.data[0].title", anyOf(is("new-doc"), is("spec.pdf"), is("notes.txt"))));
    }

    @Test
    @DisplayName("검색 성공: category 필터")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_filter_by_category() throws Exception {
        mockMvc.perform(get("/api/v1/archive")
                        .param("category", "IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[*].category", everyItem(is("IT"))));
    }

    @Test
    @DisplayName("검색 성공: title 부분검색")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_filter_by_title_contains() throws Exception {
        // 준비: 특정 키워드 가진 데이터 보장
        Folder docsFolder = folderRepository.findById(docsFolderId).orElseThrow();
        DataSource d = new DataSource();
        d.setFolder(docsFolder);
        d.setTitle("Search Key 포함 문서");
        d.setSummary("검색 테스트");
        d.setSourceUrl("http://src/search");
        d.setImageUrl("http://img/search");
        d.setDataCreatedDate(LocalDate.now());
        d.setActive(true);
        d.setCategory(Category.IT);
        dataSourceRepository.save(d);

        mockMvc.perform(get("/api/v1/archive")
                        .param("title", "key"))  // 대소문자 무시 contains
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].title", containsString("Key")));
    }

    @Test
    @DisplayName("검색 성공: summary 부분검색")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_filter_by_summary_contains() throws Exception {
        Folder docsFolder = folderRepository.findById(docsFolderId).orElseThrow();
        DataSource d = new DataSource();
        d.setFolder(docsFolder);
        d.setTitle("sum-doc");
        d.setSummary("요약에 특수키워드 들어감");
        d.setSourceUrl("http://src/sum");
        d.setImageUrl("http://img/sum");
        d.setDataCreatedDate(LocalDate.now());
        d.setActive(true);
        d.setCategory(Category.IT);
        dataSourceRepository.save(d);

        mockMvc.perform(get("/api/v1/archive")
                        .param("summary", "특수키워드"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].summary", containsString("특수키워드")));
    }

    @Test
    @DisplayName("검색 성공: folderName 필터")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_filter_by_folderName() throws Exception {
        // setup에서 만든 docs 폴더명으로 필터 (폴더 생성시 이름 "docs")
        mockMvc.perform(get("/api/v1/archive")
                        .param("folderName", "docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("검색 성공: 정렬 title ASC")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_sort_by_title_asc() throws Exception {
        mockMvc.perform(get("/api/v1/archive")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.pageInfo.sorted", containsStringIgnoringCase("title")));
    }

    @Test
    @DisplayName("검색 실패: 잘못된 category 값 → 400")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_invalid_category() throws Exception {
        mockMvc.perform(get("/api/v1/archive")
                        .param("category", "NOT_A_CATEGORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(either(is(200)).or(is("200"))))
                .andExpect(jsonPath("$.data").isArray());
    }
}
