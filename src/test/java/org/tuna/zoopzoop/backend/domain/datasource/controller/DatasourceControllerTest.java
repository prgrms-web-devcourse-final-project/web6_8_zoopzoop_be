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
}
