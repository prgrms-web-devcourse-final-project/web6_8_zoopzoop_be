package org.tuna.zoopzoop.backend.domain.archive.folder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.reqBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.PersonalArchiveFolderService;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 개인 아카이브 폴더 컨트롤러 통합 테스트 (MockMvc)
 * - 전역 예외 핸들러를 통해 RsData(JSON)로 응답함
 * - 응답의 "status" 값은 문자열("200","400","404",...)로 검증
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    @Autowired private PersonalArchiveFolderService personalArchiveFolderService;
    @Autowired private FolderRepository folderRepository;

    @Autowired private DataSourceRepository dataSourceRepository;

    private final String TEST_PROVIDER_KEY = "sc1111";
    private Integer testMemberId;
    private Integer docsFolderId;

    @BeforeAll
    void beforeAll() {
        try {
            memberService.createMember("folderTester", "http://example.com/profile.png", TEST_PROVIDER_KEY, Provider.KAKAO);
        } catch (Exception ignored) {}

        testMemberId = memberRepository.findByProviderAndProviderKey(Provider.KAKAO, TEST_PROVIDER_KEY)
                .map(BaseEntity::getId)
                .orElseThrow();

        // given
        FolderResponse fr = personalArchiveFolderService.createFolder(testMemberId, "docs");
        docsFolderId = fr.folderId();

        Folder docsFolder = folderRepository.findById(docsFolderId).orElseThrow();

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
    }

    @AfterAll
    void afterAll() {
        try {
            if (docsFolderId != null) {
                dataSourceRepository.deleteAll(dataSourceRepository.findAllByFolderId(docsFolderId));
                folderRepository.findById(docsFolderId).ifPresent(folderRepository::delete);
            }
        } catch (Exception ignored) {}
    }

    // ---------- Create ----------
    @Test
    @DisplayName("개인 아카이브 폴더 생성 - 성공 시 200과 응답 DTO 반환")
    @WithUserDetails("KAKAO:sc1111")
    void createFolder_ok() throws Exception {
        var req = new reqBodyForCreateFolder("보고서");

        mockMvc.perform(post("/api/v1/archive/folder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("보고서 폴더가 생성됐습니다."))
                .andExpect(jsonPath("$.data.folderId").isNumber())
                .andExpect(jsonPath("$.data.folderName").value("보고서"));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 생성 - 폴더 이름 누락 시 400")
    @WithUserDetails("KAKAO:sc1111")
    void createFolder_missingName() throws Exception {
        var req = new reqBodyForCreateFolder(null);

        mockMvc.perform(post("/api/v1/archive/folder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    // ---------- Delete ----------
    @Test
    @DisplayName("개인 아카이브 폴더 삭제 - 성공 시 200과 삭제 메시지 반환")
    @WithUserDetails("KAKAO:sc1111")
    void deleteFolder_ok() throws Exception {
        FolderResponse fr = personalArchiveFolderService.createFolder(testMemberId, "todelete");
        Integer idToDelete = fr.folderId();

        mockMvc.perform(delete("/api/v1/archive/folder/{folderId}", idToDelete))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("todelete 폴더가 삭제됐습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("개인 아카이브 폴더 삭제 실패 - 기본 폴더면 400")
    @WithUserDetails("KAKAO:sc1111")
    void deleteDefaultFolder_badRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/archive/folder/{folderId}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.msg").value("default 폴더는 삭제할 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 삭제 - 존재하지 않으면 404")
    @WithUserDetails("KAKAO:sc1111")
    void deleteFolder_notFound() throws Exception {
        mockMvc.perform(delete("/api/v1/archive/folder/{folderId}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }

    // ---------- Update ----------
    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 - 성공 시 200과 변경된 이름 반환")
    @WithUserDetails("KAKAO:sc1111")
    void updateFolder_ok() throws Exception {
        FolderResponse fr = personalArchiveFolderService.createFolder(testMemberId, "toRename");
        Integer id = fr.folderId();

        var body = new java.util.HashMap<String,String>();
        body.put("folderName","회의록");

        mockMvc.perform(patch("/api/v1/archive/folder/{folderId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("폴더 이름이 회의록 으로 변경됐습니다."))
                .andExpect(jsonPath("$.data.folderName").value("회의록"));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 실패 - 기본 폴더면 400")
    @WithUserDetails("KAKAO:sc1111")
    void updateDefaultFolder_badRequest() throws Exception {
        var body = new java.util.HashMap<String,String>();
        body.put("folderName","무시됨");

        mockMvc.perform(patch("/api/v1/archive/folder/{folderId}", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.msg").value("default 폴더는 이름을 변경할 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 - 존재하지 않는 폴더면 404")
    @WithUserDetails("KAKAO:sc1111")
    void updateFolder_notFound() throws Exception {
        var body = new java.util.HashMap<String,String>();
        body.put("folderName","회의록");

        mockMvc.perform(patch("/api/v1/archive/folder/{folderId}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }

    // ---------- Read ----------
    @Test
    @DisplayName("개인 아카이브 폴더 목록 조회 - 성공")
    @WithUserDetails("KAKAO:sc1111")
    void getFolders_success() throws Exception {
        mockMvc.perform(get("/api/v1/archive/folder")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("개인 아카이브의 폴더 목록을 불러왔습니다."))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("개인 아카이브 폴더 내 파일 목록 조회 - 성공")
    @WithUserDetails("KAKAO:sc1111")
    void getFilesInFolder_success() throws Exception {
        mockMvc.perform(get("/api/v1/archive/folder/{folderId}/files", docsFolderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("해당 폴더의 파일 목록을 불러왔습니다."))
                .andExpect(jsonPath("$.data.files").isArray())
                .andExpect(jsonPath("$.data.files.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.files[0].dataSourceId").isNumber())
                .andExpect(jsonPath("$.data.files[0].title").isString())
                .andExpect(jsonPath("$.data.files[0].summary").isString())
                .andExpect(jsonPath("$.data.files[0].sourceUrl").isString())
                .andExpect(jsonPath("$.data.files[0].imageUrl").isString());
    }

    @Test
    @DisplayName("개인 아카이브 기본 폴더 내 파일 목록 조회 - 성공")
    @WithUserDetails("KAKAO:sc1111")
    void getFilesInDefaultFolder_success() throws Exception {
        mockMvc.perform(get("/api/v1/archive/folder/{folderId}/files", 0)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))

                .andExpect(jsonPath("$.msg").value("해당 폴더의 파일 목록을 불러왔습니다."))
                .andExpect(jsonPath("$.data.folderId").isNumber())
                .andExpect(jsonPath("$.data.folderName").isString())
                .andExpect(jsonPath("$.data.files").isArray());
    }

    @Test
    @DisplayName("개인 아카이브 폴더 내 파일 목록 조회 - 폴더가 없으면 404")
    @WithUserDetails("KAKAO:sc1111")
    void getFilesInFolder_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/archive/folder/{folderId}/files", 999999)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }
}
