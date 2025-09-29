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
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FolderController 통합 테스트 (Given / When / Then 주석 유지)
 *
 * - @SpringBootTest + @AutoConfigureMockMvc 로 전체 컨텍스트에서 테스트
 * - @WithUserDetails 를 사용해 인증 principal 을 주입
 * - 테스트용 멤버는 BeforeAll에서 생성 (UserDetailsService 가 해당 username 으로 로드 가능해야 함)
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

    @Autowired private FolderService folderService;
    @Autowired private FolderRepository folderRepository;

    @Autowired private DataSourceRepository dataSourceRepository;

    private final String TEST_PROVIDER_KEY = "sc1111"; // WithUserDetails 에서 사용되는 provider key ("KAKAO:sc1111")
    private Integer testMemberId;
    private Integer docsFolderId;

    @BeforeAll
    void beforeAll() {
        // WithUserDetails가 SecurityContext 생성 시 DB에서 사용자를 조회하므로 미리 생성
        try {
            memberService.createMember("folderTester", "http://example.com/profile.png", TEST_PROVIDER_KEY, Provider.KAKAO);
        } catch (Exception ignored) {}

        // 준비된 멤버 ID
        testMemberId = memberRepository.findByProviderAndProviderKey(Provider.KAKAO, TEST_PROVIDER_KEY)
                .map(m -> m.getId())
                .orElseThrow();

        // GIVEN: 테스트용 폴더 및 샘플 자료 준비 (docs 폴더 + 2개 자료)
        FolderResponse fr = folderService.createFolderForPersonal(testMemberId, "docs");
        docsFolderId = fr.folderId();

        Folder docsFolder = folderRepository.findById(docsFolderId).orElseThrow();

        // 자료 2건 생성 — **category는 NOT NULL enum** 이므로 반드시 설정
        DataSource d1 = new DataSource();
        d1.setFolder(docsFolder);
        d1.setTitle("spec.pdf");
        d1.setSummary("요약 A");
        d1.setSourceUrl("http://src/a");
        d1.setImageUrl("http://img/a");
        d1.setDataCreatedDate(LocalDate.now());
        d1.setActive(true);
        d1.setTags(List.of(new Tag("tag1"), new Tag("tag2")));
        d1.setCategory(Category.IT); // enum 타입 반영
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
        // 테스트용 회원 삭제 (cascade에 따라 연결된 엔티티 정리)
        memberRepository.findByProviderAndProviderKey(Provider.KAKAO, TEST_PROVIDER_KEY)
                .ifPresent(memberRepository::delete);
    }

    // CreateFile
    @Test
    @DisplayName("개인 아카이브 폴더 생성 - 성공 시 200과 응답 DTO 반환")
    @WithUserDetails("KAKAO:sc1111")
    void createFolder_ok() throws Exception {
        // Given
        var req = new reqBodyForCreateFolder("보고서");

        // When & Then
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
        // Given
        var req = new reqBodyForCreateFolder(null);

        // When & Then
        mockMvc.perform(post("/api/v1/archive/folder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("개인 아카이브 폴더 삭제 - 성공 시 200과 삭제 메시지 반환")
    @WithUserDetails("KAKAO:sc1111")
    void deleteFolder_ok() throws Exception {
        // Given: 새 폴더 생성 후 삭제 준비
        FolderResponse fr = folderService.createFolderForPersonal(testMemberId, "todelete");
        Integer idToDelete = fr.folderId();

        // When & Then
        mockMvc.perform(delete("/api/v1/archive/folder/{folderId}", idToDelete))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("todelete 폴더가 삭제됐습니다."));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 삭제 - 존재하지 않으면 404")
    @WithUserDetails("KAKAO:sc1111")
    void deleteFolder_notFound() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/archive/folder/{folderId}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }

    // UpdateFile
    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 - 성공 시 200과 변경된 이름 반환")
    @WithUserDetails("KAKAO:sc1111")
    void updateFolder_ok() throws Exception {
        // Given: rename 대상 폴더 생성
        FolderResponse fr = folderService.createFolderForPersonal(testMemberId, "toRename");
        Integer id = fr.folderId();

        var body = new java.util.HashMap<String,String>();
        body.put("folderName","회의록");

        // When & Then
        mockMvc.perform(patch("/api/v1/archive/folder/{folderId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("폴더 이름이 회의록 으로 변경됐습니다."))
                .andExpect(jsonPath("$.data.folderName").value("회의록"));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 - 존재하지 않는 폴더면 404")
    @WithUserDetails("KAKAO:sc1111")
    void updateFolder_notFound() throws Exception {
        // Given
        var body = new java.util.HashMap<String,String>();
        body.put("folderName","회의록");

        // When & Then
        mockMvc.perform(patch("/api/v1/archive/folder/{folderId}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }

    // ReadFolder
    // Read: 내 폴더 목록
    @Test
    @DisplayName("개인 아카이브 폴더 목록 조회 - 성공")
    @WithUserDetails("KAKAO:sc1111")
    void getFolders_success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/archive/folder")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("개인 아카이브의 폴더 목록을 불러왔습니다."))
                .andExpect(jsonPath("$.data.folders").isArray());
    }

    // Read: 폴더 내 파일 목록
    @Test
    @DisplayName("폴더 내 파일 목록 조회 - 성공")
    @WithUserDetails("KAKAO:sc1111")
    void getFilesInFolder_success() throws Exception {
        // Given : @BeforeAll: docsFolderId 및 샘플 파일 준비됨

        // When & Then
        mockMvc.perform(get("/api/v1/archive/folder/{folderId}/files", docsFolderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
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
    @DisplayName("폴더 내 파일 목록 조회 - 폴더가 없으면 404")
    @WithUserDetails("KAKAO:sc1111")
    void getFilesInFolder_notFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/archive/folder/{folderId}/files", 999999)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }
}
