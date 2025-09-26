package org.tuna.zoopzoop.backend.domain.datasource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private final String TEST_PROVIDER_KEY = "testUser_sc1111"; // WithUserDetails username -> "KAKAO:testUser_sc1111"

    private Integer testMemberId;
    private Integer docsFolderId;
    private Integer dataSourceId1;
    private Integer dataSourceId2;

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
        // 생성한 자료/폴더/멤버 삭제
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
    @DisplayName("자료 생성 성공 - folderId=null → default 폴더에 등록")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void create_defaultFolder() throws Exception {
        var rq = new reqBodyForCreateDataSource("https://example.com/a", null);

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
}
