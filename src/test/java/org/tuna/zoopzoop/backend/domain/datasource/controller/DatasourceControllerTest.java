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
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
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

    // 자료 단건 이동
    @Test
    @DisplayName("단건 이동 성공 -> 200")
    void moveOne_ok() throws Exception {
        // given
        when(dataSourceService.moveDataSource(anyInt(), eq(1), eq(200)))
                .thenReturn(new DataSourceService.MoveResult(1, 200));

        String body = om.writeValueAsString(new reqBodyForMoveDataSource(200));

        // expect
        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dataSourceId").value(1))
                .andExpect(jsonPath("$.data.folderId").value(200));
    }

    @Test
    @DisplayName(" 단건 이동 성공: default 폴더(null) -> 200")
    void moveOne_default_ok() throws Exception {
        when(dataSourceService.moveDataSource(anyInt(), eq(1), isNull()))
                .thenReturn(new DataSourceService.MoveResult(1, 999)); // default folder id

        String body = om.writeValueAsString(new reqBodyForMoveDataSource(null));

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.folderId").value(999));
    }

    @Test
    @DisplayName("단건 이동 실패: 자료 없음 -> 400")
    void moveOne_notFound_data() throws Exception {
        when(dataSourceService.moveDataSource(anyInt(), eq(1), eq(200)))
                .thenThrow(new NoResultException("존재하지 않는 자료입니다."));

        String body = om.writeValueAsString(new reqBodyForMoveDataSource(200));

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("단건 이동 실패: 폴더 없음 -> 404")
    void moveOne_notFound_folder() throws Exception {
        when(dataSourceService.moveDataSource(anyInt(), eq(1), eq(200)))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        String body = om.writeValueAsString(new reqBodyForMoveDataSource(200));

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}/move", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // 자료 다건 이동
    @Test
    @DisplayName("다건 이동 성공: 지정 폴더 -> 200")
    void moveMany_specific_ok() throws Exception {
        String body = "{\"folderId\":200,\"dataSourceId\":[1,2,3]}";

        mockMvc.perform(patch("/api/v1/archive/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("복수 개의 자료를 이동했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
    @Test
    @DisplayName("다건 이동 성공: 기본 폴더(null) -> 200")
    void moveMany_default_ok() throws Exception {
        // 서비스는 void 리턴이라 스텁 불필요 (예외만 없으면 200)
        String body = "{\"folderId\":null,\"dataSourceId\":[1,2,3]}";

        mockMvc.perform(patch("/api/v1/archive/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("복수 개의 자료를 이동했습니다."));
    }

    @Test
    @DisplayName("다건 이동 실패: 기본 폴더 없음 -> 404")
    void moveMany_default_missing() throws Exception {
        String body = "{\"folderId\":null,\"dataSourceId\":[1,2]}";

        doThrow(new NoResultException("기본 폴더가 존재하지 않습니다."))
                .when(dataSourceService).moveDataSources(anyInt(), isNull(), eq(List.of(1,2)));

        mockMvc.perform(patch("/api/v1/archive/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // 자료 수정
    @Test
    @DisplayName("자료 수정 성공 -> 200")
    void update_ok() throws Exception {
        int id = 10;
        when(dataSourceService.updateDataSource(eq(id), eq("새 제목"), eq("짧은 요약")))
                .thenReturn(id);

        var body = new reqBodyForUpdateDataSource("새 제목", "짧은 요약");
        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(id + "번 자료가 수정됐습니다."))
                .andExpect(jsonPath("$.data.dataSourceId").value(id));
    }

    @Test
    @DisplayName("자료 수정 실패: 요청 바디가 모두 공백 -> 400")
    void update_badRequest_whenEmpty() throws Exception {
        var body = new reqBodyForUpdateDataSource("  ", null);

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @DisplayName("자료 수정 실패: 존재하지 않는 자료 -> 404")
    void update_notFound() throws Exception {
        int id = 999;
        when(dataSourceService.updateDataSource(eq(id), any(), any()))
                .thenThrow(new NoResultException("존재하지 않는 자료입니다."));

        var body = new reqBodyForUpdateDataSource("제목", "요약");

        mockMvc.perform(patch("/api/v1/archive/{dataSourceId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 자료입니다."));
    }


}
