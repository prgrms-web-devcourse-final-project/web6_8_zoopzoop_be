package org.tuna.zoopzoop.backend.domain.space.archive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
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
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpaceArchiveDataSourceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;

    @Autowired SpaceService spaceService;
    @Autowired MembershipService membershipService;

    @Autowired FolderRepository folderRepository;
    @Autowired DataSourceRepository dataSourceRepository;

    private static final String OWNER_PK = "sc_owner_1111";
    private Integer ownerMemberId;

    private Integer spaceId;
    private Integer defaultFolderId;
    private Integer docsFolderId;
    private Integer ds1Id;
    private Integer personalDefaultFolderId;
    private Integer personalDs1Id, personalDs2Id, personalDs3Id;

    @BeforeAll
    void setUp() {
        // 사용자 생성 (있으면 무시)
        try { memberService.createMember("spaceOwner", "http://img/owner.png", OWNER_PK, Provider.KAKAO); } catch (Exception ignored) {}
        ownerMemberId = memberRepository.findByProviderAndProviderKey(Provider.KAKAO, OWNER_PK)
                .map(BaseEntity::getId).orElseThrow();
        // 스페이스 + 멤버십
        Space space = spaceService.createSpace("space-ds-test");
        spaceId = space.getId();
        membershipService.addMemberToSpace(memberRepository.findById(ownerMemberId).orElseThrow(), space, Authority.ADMIN);

        // 공유 아카이브의 기본/추가 폴더 확보
        var archive = space.getSharingArchive().getArchive();

        Folder defaultFolder = folderRepository.findByArchiveIdAndIsDefaultTrue(archive.getId())
                .orElseGet(() -> {
                    Folder f = new Folder("default");
                    f.setDefault(true);
                    f.setArchive(archive);
                    return folderRepository.saveAndFlush(f);
                });
        defaultFolderId = defaultFolder.getId();

        Folder docsFolder = folderRepository.findByArchiveIdAndName(archive.getId(), "docs")
                .orElseGet(() -> {
                    Folder f = new Folder("docs");
                    f.setDefault(false);
                    f.setArchive(archive);
                    return folderRepository.saveAndFlush(f);
                });
        docsFolderId = docsFolder.getId();

        // 자료 2~3개 심기
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
        ds1Id = dataSourceRepository.saveAndFlush(d1).getId();

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
        dataSourceRepository.saveAndFlush(d2);

        // === 개인(default) 폴더 찾고 개인 자료 시드 ===
        var personalDefaultFolder = folderRepository.findDefaultFolderByMemberId(ownerMemberId)
                .orElseThrow();

        personalDefaultFolderId = personalDefaultFolder.getId();

        DataSource p1 = new DataSource();
        p1.setFolder(personalDefaultFolder);
        p1.setTitle("p1");
        p1.setActive(true);
        personalDs1Id = dataSourceRepository.saveAndFlush(p1).getId();

        DataSource p2 = new DataSource();
        p2.setFolder(personalDefaultFolder);
        p2.setTitle("p2");
        p2.setActive(true);
        personalDs2Id = dataSourceRepository.saveAndFlush(p2).getId();

        DataSource p3 = new DataSource();
        p3.setFolder(personalDefaultFolder);
        p3.setTitle("p3");
        p3.setActive(true);
        personalDs3Id = dataSourceRepository.saveAndFlush(p3).getId();
    }

    // ----------------- 삭제 -----------------
    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 단건 삭제")
    void delete_one_ok() throws Exception {
        // 테스트를 위한 임시 자료 생성 후 삭제
        DataSource temp = new DataSource();
        temp.setFolder(folderRepository.findById(docsFolderId).orElseThrow());
        temp.setTitle("temp");
        temp.setActive(true);
        dataSourceRepository.saveAndFlush(temp);

        mockMvc.perform(delete("/api/v1/space/{spaceId}/archive/datasources/{dataSourceId}", spaceId, temp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.dataSourceId").value(temp.getId()));
    }

    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 다건 삭제")
    void delete_many_ok() throws Exception {
        // 임시 2건 생성
        var f = folderRepository.findById(docsFolderId).orElseThrow();
        DataSource a = new DataSource();
        a.setFolder(f);
        a.setTitle("bulk-a");
        a.setActive(true);
        dataSourceRepository.saveAndFlush(a);

        DataSource b = new DataSource();
        b.setFolder(f);
        b.setTitle("bulk-b");
        b.setActive(true);
        dataSourceRepository.saveAndFlush(b);

        String body = om.writeValueAsString(Map.of(
                "dataSourceId", List.of(a.getId(), b.getId())
        ));

        mockMvc.perform(post("/api/v1/space/{spaceId}/archive/datasources/delete", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    // ----------------- 소프트 삭제/복원 -----------------
    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 다건 임시 삭제")
    void soft_delete_ok() throws Exception {
        String body = om.writeValueAsString(Map.of("dataSourceId", List.of(ds1Id)));
        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/datasources/soft-delete", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 다건 복원")
    void restore_ok() throws Exception {
        String body = om.writeValueAsString(Map.of("dataSourceId", List.of(ds1Id)));
        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/datasources/restore", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    // ----------------- 이동 -----------------
    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 단건 이동")
    void move_one_ok() throws Exception {
        // 대상 폴더를 default로
        String body = om.writeValueAsString(Map.of("folderId", defaultFolderId));
        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/datasources/{dataSourceId}/move", spaceId, ds1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.folderId").value(defaultFolderId));
    }

    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 다건 이동")
    void move_many_ok() throws Exception {
        var f = folderRepository.findById(docsFolderId).orElseThrow();

        DataSource a = new DataSource();
        a.setFolder(f); a.setTitle("mva"); a.setActive(true);
        a = dataSourceRepository.saveAndFlush(a);

        DataSource b = new DataSource();
        b.setFolder(f); b.setTitle("mvb"); b.setActive(true);
        b = dataSourceRepository.saveAndFlush(b);

        String body = om.writeValueAsString(Map.of(
                "folderId", defaultFolderId,
                "dataSourceId", List.of(a.getId(), b.getId())
        ));

        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/datasources/move", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    // ----------------- 수정 -----------------
    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 수정")
    void update_ok() throws Exception {
        String body = """
        {
          "title": "수정제목",
          "summary": "수정요약",
          "sourceUrl": "http://src/new",
          "imageUrl": "http://img/new",
          "source": "Edited",
          "category": "IT"
        }
        """;

//        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/datasources/{dataSourceId}", spaceId, ds1Id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("200"))
//                .andExpect(jsonPath("$.data.dataSourceId").value(ds1Id));

        mockMvc.perform(patch("/api/v1/space/{spaceId}/archive/datasources/{dataSourceId}", spaceId, ds1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ----------------- Import (개인 → 공유) -----------------
    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("개인 → 공유: 단건 불러오기")
    void import_one_ok() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "targetFolderId", defaultFolderId  // 0/null이면 default
        ));

        mockMvc.perform(post("/api/v1/space/{spaceId}/archive/datasources/{dataSourceId}/import", spaceId, personalDs1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("개인 → 공유: 다건 불러오기")
    void import_many_ok() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "dataSourceId", List.of(personalDs1Id, personalDs2Id, personalDs3Id),
                "targetFolderId", defaultFolderId
        ));
        mockMvc.perform(post("/api/v1/space/{spaceId}/archive/datasources/import/batch", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    // ----------------- 검색 -----------------
    @Test
    @WithUserDetails(value = "KAKAO:" + OWNER_PK, setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("공유 자료 검색")
    void search_ok() throws Exception {
        mockMvc.perform(get("/api/v1/space/{spaceId}/archive/datasources", spaceId)
                        .param("keyword", "spec")
                        .param("category", "IT")
                        .param("page", "0")
                        .param("size", "8")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(greaterThanOrEqualTo(1)));
    }
}
