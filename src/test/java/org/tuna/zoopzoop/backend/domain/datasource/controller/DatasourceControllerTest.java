package org.tuna.zoopzoop.backend.domain.datasource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tuna.zoopzoop.backend.domain.datasource.dto.reqBodyForCreateDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.dto.reqBodyForDeleteMany;
import org.tuna.zoopzoop.backend.domain.datasource.dto.reqBodyForMoveDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.dto.resBodyForMoveDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.global.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DatasourceControllerTest {

    @Mock private DataSourceService dataSourceService;
    @InjectMocks private DatasourceController datasourceController;

    private MockMvc mockMvc;
    private ObjectMapper om;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        om = new ObjectMapper();

        // ✅ 한 번만 생성 + 전역 예외핸들러 등록
        mockMvc = MockMvcBuilders
                .standaloneSetup(datasourceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // create
    @Test
    @DisplayName("자료 생성 성공 - folderId=null → default 폴더에 등록")
    void create_defaultFolder() throws Exception {
        var rq = new reqBodyForCreateDataSource("https://example.com/a", null);

        when(dataSourceService.createDataSource(anyInt(), eq(rq.sourceUrl()), isNull()))
                .thenReturn(1001);

        mockMvc.perform(post("/api/v1/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(rq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("새로운 자료가 등록됐습니다."))
                .andExpect(jsonPath("$.data").value(1001));
    }

    @Test
    @DisplayName("자료 생성 성공 - folderId 지정 → 해당 폴더에 등록")
    void create_specificFolder() throws Exception {
        var rq = new reqBodyForCreateDataSource("https://example.com/b", 55);

        when(dataSourceService.createDataSource(anyInt(), eq(rq.sourceUrl()), eq(rq.folderId())))
                .thenReturn(2002);

        mockMvc.perform(post("/api/v1/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(rq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("새로운 자료가 등록됐습니다."))
                .andExpect(jsonPath("$.data").value(2002));
    }

    // delete
    @Test
    @DisplayName("단건 삭제 성공 -> 200")
    void delete_success() throws Exception {
        // given
        int id = 123;
        when(dataSourceService.deleteById(id)).thenReturn(id);

        // when & then
        mockMvc.perform(delete("/api/v1/archive/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(id + "번 자료가 삭제됐습니다."))
                .andExpect(jsonPath("$.data.dataSourceId").value(id));
    }

    @Test
    @DisplayName("단건 삭제 실패: 자료 없음 → 404 Not Found")
    void delete_notFound() throws Exception {
        int id = 999;
        when(dataSourceService.deleteById(id))
                .thenThrow(new NoResultException("존재하지 않는 자료입니다."));

        mockMvc.perform(delete("/api/v1/archive/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 자료입니다."));
    }

    // deleteMany
    @Test
    @DisplayName("다건 삭제 성공 -> 200")
    void deleteMany_success() throws Exception {
        var body = new reqBodyForDeleteMany(List.of(10, 20, 30));
        doNothing().when(dataSourceService).deleteMany(anyList());

        mockMvc.perform(post("/api/v1/archive/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("복수개의 자료가 삭제됐습니다."))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("다건 삭제 실패: 배열 비어있음 → 400 Bad Request")
    void deleteMany_empty() throws Exception {
        // @NotEmpty로 잡히면 MethodArgumentNotValidException(400), 서비스에서 잡히면 IllegalArgumentException(400)
        var empty = new reqBodyForDeleteMany(List.of());

        mockMvc.perform(post("/api/v1/archive/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(empty)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    @DisplayName("다건 삭제 실패: 일부 ID 미존재 → 404 Not Found")
    void deleteMany_partialMissing() throws Exception {
        var body = new reqBodyForDeleteMany(List.of(1, 2, 3));
        doThrow(new NoResultException("존재하지 않는 자료 ID 포함: [2]"))
                .when(dataSourceService).deleteMany(anyList());

        mockMvc.perform(post("/api/v1/archive/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 자료 ID 포함: [2]"));
    }
}
