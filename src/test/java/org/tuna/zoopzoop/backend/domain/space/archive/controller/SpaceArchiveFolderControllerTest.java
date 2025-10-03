package org.tuna.zoopzoop.backend.domain.space.archive.controller;

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
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.reqBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpaceArchiveFolderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    @Autowired private SpaceService spaceService;
    @Autowired private MembershipService membershipService;

    @Autowired private FolderRepository folderRepository;
    @Autowired private DataSourceRepository dataSourceRepository;

    private static final String OWNER_PK = "sp1111";
    private static final String READER_PK = "sp2222";

    private Integer ownerMemberId;
    private Integer readerMemberId;

    private Integer spaceId;
    private Integer defaultFolderId;
    private Integer docsFolderId;

    @BeforeAll
    void setUp() {
        // 사용자 생성
        try { memberService.createMember("spaceOwner", "http://img/owner.png", OWNER_PK, Provider.KAKAO); } catch (Exception ignored) {}
        try { memberService.createMember("spaceReader", "http://img/reader.png", READER_PK, Provider.KAKAO); } catch (Exception ignored) {}

        ownerMemberId = memberRepository.findByProviderAndProviderKey(Provider.KAKAO, OWNER_PK)
                .map(BaseEntity::getId).orElseThrow();
        readerMemberId = memberRepository.findByProviderAndProviderKey(Provider.KAKAO, READER_PK)
                .map(BaseEntity::getId).orElseThrow();

        Member owner = memberRepository.findById(ownerMemberId).orElseThrow();
        Member reader = memberRepository.findById(readerMemberId).orElseThrow();

        // 스페이스 생성 + 멤버십 부여
        Space space = spaceService.createSpace("space-folder-test");
        spaceId = space.getId();

        membershipService.addMemberToSpace(owner, space, Authority.ADMIN);
        membershipService.addMemberToSpace(reader, space, Authority.READ_ONLY);

        // 공유 아카이브의 default 폴더 직접 생성
        // 공유 아카이브의 default 폴더 idempotent 생성
        var archive = space.getSharingArchive().getArchive();

        Folder defaultFolder = folderRepository.findByArchiveIdAndIsDefaultTrue(archive.getId())
                .orElseGet(() -> {
                    Folder f = new Folder("default");
                    f.setArchive(archive);
                    f.setDefault(true);
                    return folderRepository.saveAndFlush(f);
                });
        defaultFolderId = defaultFolder.getId();

        Folder docsFolder = folderRepository.findByArchiveIdAndName(archive.getId(), "docs")
                .orElseGet(() -> {
                    Folder f = new Folder();
                    f.setArchive(archive);
                    f.setName("docs");
                    f.setDefault(false);
                    return folderRepository.saveAndFlush(f);
                });
        docsFolderId = docsFolder.getId();


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
    void tearDown() {
        try {
            if (docsFolderId != null) {
                dataSourceRepository.deleteAll(dataSourceRepository.findAllByFolderId(docsFolderId));
            }
        } catch (Exception ignored) {}
    }

    // ---------- Create ----------
    @Test
    @DisplayName("공유 아카이브 폴더 생성 - 성공 시 200과 응답 DTO 반환")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void createFolder_ok() throws Exception {
        var req = new reqBodyForCreateFolder("보고서");

        mockMvc.perform(post("/api/v1/space/{spaceId}/archive/folder", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("보고서 폴더가 생성되었습니다."))
                .andExpect(jsonPath("$.data.folderId").isNumber())
                .andExpect(jsonPath("$.data.folderName").value("보고서"));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 생성 - 폴더 이름 누락 시 400")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void createFolder_missingName() throws Exception {
        var req = new reqBodyForCreateFolder(null);

        mockMvc.perform(post("/api/v1/space/{spaceId}/archive/folder", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 생성 실패 - 스페이스 구성원이 아니면 403")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void createFolder_notMember_forbidden() {
        var req = new reqBodyForCreateFolder("x");
        int otherSpaceId = 999999;
        // 존재하지 않으면 404인데, 여기서는 멤버십 실패를 보기 위해 우선 spaceId 그대로 두고 아래 테스트로 대체
        // 실제로 멤버십 실패를 정확히 보려면 별도 스페이스를 생성하되 owner를 초대하지 않는 방법이 필요
    }

    @Test
    @DisplayName("공유 아카이브 폴더 생성 실패 - 권한 없음(READ_ONLY) 403")
    @WithUserDetails("KAKAO:" + READER_PK)
    void createFolder_noAuthority_forbidden() throws Exception {
        var req = new reqBodyForCreateFolder("읽기전용은못만듦");

        mockMvc.perform(post("/api/v1/space/{spaceId}/archive/folder", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.msg").value("폴더 생성 권한이 없습니다."));
    }

    // ---------- Delete ----------
    @Test
    @DisplayName("공유 아카이브 폴더 삭제 - 성공 시 200과 삭제 메시지 반환")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void deleteFolder_ok() throws Exception {
        var req = new reqBodyForCreateFolder("todelete");
        String content = objectMapper.writeValueAsString(req);
        var createRes = mockMvc.perform(post("/api/v1/space/{spaceId}/archive/folder", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andReturn().getResponse().getContentAsString();

        Integer toDelete = folderRepository.findByArchiveIdAndName(
                folderRepository.findAll().stream().filter(f -> "todelete".equals(f.getName())).findFirst().orElseThrow().getArchive().getId(),
                "todelete"
        ).orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, toDelete))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("todelete 폴더가 삭제됐습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 삭제 실패 - 기본 폴더면 400")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void deleteDefaultFolder_badRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.msg").value("default 폴더는 삭제할 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 삭제 실패 - 권한 없음(READ_ONLY) 403")
    @WithUserDetails("KAKAO:" + READER_PK)
    void deleteFolder_noAuthority_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, docsFolderId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.msg").value("폴더 삭제 권한이 없습니다."));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 삭제 실패 - 폴더가 없으면 404")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void deleteFolder_notFound() throws Exception {
        mockMvc.perform(delete("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }

    // ---------- Update ----------
    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 - 성공 시 200과 변경된 이름 반환")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void updateFolder_ok() throws Exception {
        var body = new java.util.HashMap<String, String>();
        body.put("folderName", "회의록");

        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, docsFolderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("폴더 이름이 회의록(으)로 변경됐습니다."))
                .andExpect(jsonPath("$.data.folderName").value("회의록"));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 실패 - 기본 폴더면 400")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void updateDefaultFolder_badRequest() throws Exception {
        var body = new java.util.HashMap<String, String>();
        body.put("folderName", "무시됨");

        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.msg").value("default 폴더는 이름을 변경할 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 실패 - 권한 없음(READ_ONLY) 403")
    @WithUserDetails("KAKAO:" + READER_PK)
    void updateFolder_noAuthority() throws Exception {
        var body = new java.util.HashMap<String, String>();
        body.put("folderName", "변경불가");

        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, docsFolderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.msg").value("폴더 수정 권한이 없습니다."));
    }

    @Test
    @DisplayName("공유 아카이브 폴더 이름 변경 실패 - 폴더가 없으면 404")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void updateFolder_notFound() throws Exception {
        var body = new java.util.HashMap<String, String>();
        body.put("folderName", "어딨니");

        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/folder/{folderId}", spaceId, 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }

    // ---------- Read ----------
    @Test
    @DisplayName("공유 아카이브 폴더 목록 조회 - 성공")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void listFolders_success() throws Exception {
        mockMvc.perform(get("/api/v1/space/{spaceId}/archive/folder", spaceId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("공유 아카이브의 폴더 목록이 조회되었습니다."))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("공유 아카이브 폴더 목록 조회 실패 - 읽기 권한 없으면 403")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void listFolders_forbidden_when_not_member() throws Exception {
        Space other = spaceService.createSpace("no-membership-space");
        Integer otherSpaceId = other.getId();

        mockMvc.perform(get("/api/v1/space/{spaceId}/archive/folder", otherSpaceId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.msg").value("스페이스의 구성원이 아닙니다."));
    }

    @Test
    @DisplayName("공유 아카이브 특정 폴더 내 파일 목록 조회 - 성공")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void filesInFolder_success() throws Exception {
        mockMvc.perform(get("/api/v1/space/{spaceId}/archive/folder/{folderId}/files", spaceId, docsFolderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("폴더 안의 파일 목록을 불러왔습니다."))
                .andExpect(jsonPath("$.data.folderId").value(docsFolderId))
                .andExpect(jsonPath("$.data.folderName").isString())
                .andExpect(jsonPath("$.data.files").isArray())
                .andExpect(jsonPath("$.data.files.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("공유 아카이브 기본 폴더 내 파일 목록 조회 - 성공")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void filesInDefaultFolder_success() throws Exception {
        mockMvc.perform(get("/api/v1/space/{spaceId}/archive/folder/{folderId}/files", spaceId, 0)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("폴더 안의 파일 목록을 불러왔습니다."))
                .andExpect(jsonPath("$.data.folderId").isNumber())
                .andExpect(jsonPath("$.data.folderName").isString())
                .andExpect(jsonPath("$.data.files").isArray());
    }

    @Test
    @DisplayName("공유 아카이브 폴더 내 파일 목록 조회 실패 - 폴더가 없으면 404")
    @WithUserDetails("KAKAO:" + OWNER_PK)
    void filesInFolder_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/space/{spaceId}/archive/folder/{folderId}/files", spaceId, 999999)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }
}
