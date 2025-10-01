package org.tuna.zoopzoop.backend.domain.space.archive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SpaceArchiveDataSourceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    // 필요 시 @BeforeEach에서 space/seed 생성

    @Test
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("[공유] 등록(수동/AI): POST /api/v1/spaces/{spaceId}/archive/ai → 201")
    void create_ai_ok() throws Exception {
        int spaceId = 100;
        String body = """
        {
          "sourceUrl": "https://example.com/post-1",
          "folderId":  null,
          "mode":      "AI"   // 또는 "MANUAL"
        }
        """;

        mockMvc.perform(post("/api/v1/spaces/{spaceId}/archive/ai", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.dataSourceId").isNumber());
    }

    @Test
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("[공유] 단건 불러오기: POST /api/v1/spaces/{spaceId}/archive/{dataSourceId} → 200")
    void fetch_one_ok() throws Exception {
        int spaceId = 100, dataSourceId = 1;

        mockMvc.perform(post("/api/v1/spaces/{spaceId}/archive/{dataSourceId}", spaceId, dataSourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").value(dataSourceId));
    }

    @Test
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("[공유] 다건 불러오기: POST /api/v1/spaces/{spaceId}/archive → 200")
    void fetch_many_ok() throws Exception {
        int spaceId = 100;
        String body = """
        { "ids": [1,2,3] }
        """;
        mockMvc.perform(post("/api/v1/spaces/{spaceId}/archive", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items.length()").value(3));
    }

    // 🔹 공유 CRUD 스모크 1~2개 권장

    @Test
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("[공유] 삭제: DELETE /api/v1/spaces/{spaceId}/archive/{id} → 200")
    void delete_one_ok() throws Exception {
        int spaceId = 100, id = 10;
        mockMvc.perform(delete("/api/v1/spaces/{spaceId}/archive/{id}", spaceId, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("[공유] 이동: PATCH /api/v1/spaces/{spaceId}/archive/move → 200")
    void move_many_ok() throws Exception {
        int spaceId = 100;
        String body = """
        { "folderId": 999, "dataSourceId": [1,2] }
        """;
        mockMvc.perform(patch("/api/v1/spaces/{spaceId}/archive/move", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("[공유] 수정: PATCH /api/v1/spaces/{spaceId}/archive/{id} → 200")
    void update_ok() throws Exception {
        int spaceId = 100, id = 1;
        String body = """
        { "title": "새 제목", "summary": "요약 변경" }
        """;
        mockMvc.perform(patch("/api/v1/spaces/{spaceId}/archive/{id}", spaceId, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").value(id));
    }

    @Test
    @WithUserDetails(value = "KAKAO:testUser_sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("[공유] 검색: GET /api/v1/spaces/{spaceId}/archive/search → 200")
    void search_ok() throws Exception {
        int spaceId = 100;
        mockMvc.perform(get("/api/v1/spaces/{spaceId}/archive/search", spaceId)
                        .param("q", "AI").param("category", "IT")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }
}
