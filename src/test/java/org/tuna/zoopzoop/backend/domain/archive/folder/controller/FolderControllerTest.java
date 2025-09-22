package org.tuna.zoopzoop.backend.domain.archive.folder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.reqBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.global.exception.GlobalExceptionHandler;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FolderControllerTest {

    @Mock private FolderService folderService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        FolderController controller = new FolderController(folderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    // CreateFile
    @Test
    @DisplayName("개인 아카이브 폴더 생성 - 성공 시 200과 응답 DTO 반환")
    void createFolder_ok() throws Exception {
        // given
        when(folderService.createFolderForPersonal(anyInt(), eq("보고서")))
                .thenReturn(new FolderResponse(123, "보고서"));
        var req = new reqBodyForCreateFolder("보고서");

        // when & then
        mockMvc.perform(post("/api/v1/archive/folder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("보고서 폴더가 생성됐습니다."))
                .andExpect(jsonPath("$.data.folderId").value(123))
                .andExpect(jsonPath("$.data.folderName").value("보고서"));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 생성 - 폴더 이름 누락 시 400")
    void createFolder_missingName() throws Exception {
        // given
        var req = new reqBodyForCreateFolder(null);

        // when & then
        mockMvc.perform(post("/api/v1/archive/folder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }


}
