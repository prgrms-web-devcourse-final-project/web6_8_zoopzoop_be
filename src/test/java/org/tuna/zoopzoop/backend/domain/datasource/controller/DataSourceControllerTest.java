package org.tuna.zoopzoop.backend.domain.datasource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.PersonalArchiveFolderService;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.dto.reqBodyForCreateDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.dto.reqBodyForMoveDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;
import org.tuna.zoopzoop.backend.domain.datasource.service.PersonalDataSourceService;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataSourceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired PersonalArchiveFolderService folderService;
    @Autowired FolderRepository folderRepository;
    @Autowired DataSourceRepository dataSourceRepository;

    @Mock PersonalDataSourceService personalApp;

    final String TEST_PROVIDER_KEY = "testUser_sc1111";

    Integer testMemberId;
    Integer docsFolderId;
    Integer dataSourceId1;
    Integer dataSourceId2;
    @Qualifier("dataProcessorService")
    @Autowired
    private DataProcessorService dataProcessorService;
    @Qualifier("tagRepository")
    @Autowired
    private TagRepository tagRepository;

    @TestConfiguration
    static class StubConfig {
        @Bean @Primary
        DataProcessorService stubDataProcessorService() {
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
                            List.of("ML", "Infra")
                    );
                }
            };
        }


        @Bean @Primary
        TagRepository stubTagRepository() {
            TagRepository mock = Mockito.mock(TagRepository.class);
            when(mock.findDistinctTagNamesByFolderId(anyInt()))
                    .thenReturn(List.of("AI","Spring"));
            return mock;
        }
    }

    @BeforeAll
    void setup() {
        try {
            memberService.createMember("testUser_sc1111", "http://img", TEST_PROVIDER_KEY, Provider.KAKAO);
        } catch (Exception ignored) {}

        var member = memberRepository.findByProviderAndProviderKey(Provider.KAKAO, TEST_PROVIDER_KEY)
                .orElseThrow();

        testMemberId = member.getId();

        // docs 폴더 생성 + default 폴더 보장
        FolderResponse fr = folderService.createFolder(testMemberId, "docs");
        docsFolderId = fr.folderId();

        Folder docsFolder = folderRepository.findById(docsFolderId).orElseThrow();
        Integer archiveId = docsFolder.getArchive().getId();

        folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId).orElseGet(() -> {
            Folder df = new Folder("default");
            df.setArchive(docsFolder.getArchive());
            df.setDefault(true);
            return folderRepository.save(df);
        });

        // seed 자료 2건 생성
        DataSource d1 = new DataSource();
        d1.setFolder(docsFolder);
        d1.setTitle("spec.pdf");
        d1.setSummary("요약 A");
        d1.setSourceUrl("http://src/a");
        d1.setImageUrl("http://img/a");
        d1.setDataCreatedDate(LocalDate.now());
        d1.setActive(true);
        d1.setCategory(Category.IT);
        d1.setTags(List.of(new Tag("tag1"), new Tag("tag2")));
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
        d2.setCategory(Category.SCIENCE);
        d2.setTags(List.of());
        dataSourceRepository.save(d2);
        dataSourceId2 = d2.getId();
    }

    // ===== 생성 =====

    @Test
    @DisplayName("[개인] 자료 생성: folderId=0 → default")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void create_default() throws Exception {
        var body = new reqBodyForCreateDataSource("https://example.com/a", 0);

        mockMvc.perform(post("/api/v1/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("새로운 자료가 등록됐습니다."))
                .andExpect(jsonPath("$.data.dataSourceId").isNumber());
    }

    @Test
    @DisplayName("[개인] 자료 생성: 지정 폴더")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void create_specificFolder() throws Exception {
        var body = new reqBodyForCreateDataSource("https://example.com/b", docsFolderId);

        mockMvc.perform(post("/api/v1/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").isNumber());
    }

    // ===== 삭제 =====

    @Test
    @DisplayName("[개인] 단건 삭제")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void delete_one() throws Exception {
        Folder f = folderRepository.findById(docsFolderId).orElseThrow();

        DataSource d = new DataSource();
        d.setFolder(f);
        d.setTitle("del");
        d.setSummary("x");
        d.setSourceUrl("s");
        d.setImageUrl("i");
        d.setDataCreatedDate(LocalDate.now());
        d.setActive(true);
        d.setCategory(Category.IT);

        dataSourceRepository.save(d);

        mockMvc.perform(delete("/api/v1/archive/{id}", d.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").value(d.getId()));
    }

    // ===== 이동 =====

    @Test
    @DisplayName("[개인] 단건 이동 → 지정 폴더")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void move_one() throws Exception {
        FolderResponse target = folderService.createFolder(testMemberId, "move-target");
        var body = new reqBodyForMoveDataSource(target.folderId());

        mockMvc.perform(patch("/api/v1/archive/{id}/move", dataSourceId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataSourceId").value(dataSourceId1))
                .andExpect(jsonPath("$.data.folderId").value(target.folderId()));
    }

    // ===== 수정 =====

    @Test
    @DisplayName("[개인] 부분 수정(title, summary)")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void update_partial() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "새 제목",
                "summary", "짧은 요약"
        );

        mockMvc.perform(patch("/api/v1/archive/{id}", dataSourceId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").value(dataSourceId1));
    }

    // ===== 검색 =====

    @Test
    @DisplayName("[개인] 검색: 기본 정렬 createdAt DESC")
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    void search_default() throws Exception {
        mockMvc.perform(get("/api/v1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }
}

