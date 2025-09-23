package org.tuna.zoopzoop.backend.domain.archive.folder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.reqBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FileSummary;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.global.exception.GlobalExceptionHandler;
import org.tuna.zoopzoop.backend.global.security.StubAuthUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
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

    // DeleteFile
    @Test
    @DisplayName("개인 아카이브 폴더 삭제 - 성공 시 200과 삭제 메시지 반환")
    void deleteFolder_ok() throws Exception {
        // given
        when(folderService.deleteFolder(7)).thenReturn("보고서");

        // when & then
        mockMvc.perform(delete("/api/v1/archive/folder/{folderId}", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("보고서 폴더가 삭제됐습니다."));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 삭제 - 존재하지 않으면 404")
    void deleteFolder_notFound() throws Exception {
        // given
        when(folderService.deleteFolder(404))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

        // when & then
        mockMvc.perform(delete("/api/v1/archive/folder/{folderId}", 404))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
    }

    // UpdateFile
    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 - 성공 시 200과 변경된 이름 반환")
    void updateFolder_ok() throws Exception {
        // given
        when(folderService.updateFolderName(10, "회의록")).thenReturn("회의록");

        Map<String, String> body = new HashMap<>();
        body.put("folderName", "회의록");

        // when & then
        mockMvc.perform(patch("/api/v1/archive/folder/{folderId}", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("폴더 이름이 회의록 으로 변경됐습니다."))
                .andExpect(jsonPath("$.data.folderName").value("회의록"));
    }

    @Test
    @DisplayName("개인 아카이브 폴더 이름 변경 - 존재하지 않는 폴더면 404")
    void updateFolder_notFound() throws Exception {
        // given
        when(folderService.updateFolderName(99, "회의록"))
                .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));


        Map<String, String> body = new HashMap<>();
        body.put("folderName", "회의록");

        // when & then
        mockMvc.perform(patch("/api/v1/archive/folder/{folderId}", 99)
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
    void getFolders_success() throws Exception {
        List<FolderResponse> folders = List.of(
                new FolderResponse(1, "default"),
                new FolderResponse(2, "docs")
        );

        try (MockedStatic<StubAuthUtil> mocked = mockStatic(StubAuthUtil.class)) {
            mocked.when(StubAuthUtil::currentMemberId).thenReturn(100);
            when(folderService.getFoldersForPersonal(100)).thenReturn(folders);

            mockMvc.perform(get("/api/v1/archive/folder")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.msg").value("개인 아카이브의 폴더 목록을 불러왔습니다."))
                    .andExpect(jsonPath("$.data.folders.length()").value(2))
                    .andExpect(jsonPath("$.data.folders[0].folderId").value(1))
                    .andExpect(jsonPath("$.data.folders[0].folderName").value("default"))
                    .andExpect(jsonPath("$.data.folders[1].folderName").value("docs"));
        }
    }

    // Read: 폴더 내 파일 목록
    @Test
    @DisplayName("폴더 내 파일 목록 조회 - 성공")
    void getFilesInFolder_success() throws Exception {
        // given
        FolderFilesDto rs = new FolderFilesDto(
                2, "docs",
                List.of(
                        new FileSummary(10, "spec.pdf", null, "요약 A", "http://src/a", "http://img/a",
                                List.of(new Tag("tag1"), new Tag("tag2"))),
                        new FileSummary(11, "notes.txt", null, "요약 B", "http://src/b", "http://img/b",
                                List.of())
                )
        );

        try (MockedStatic<StubAuthUtil> mocked = mockStatic(StubAuthUtil.class)) {
            mocked.when(StubAuthUtil::currentMemberId).thenReturn(100);
            when(folderService.getFilesInFolderForPersonal(100, 2)).thenReturn(rs);

            // when & then
            mockMvc.perform(get("/api/v1/archive/folder/{folderId}/files", 2)
                            .accept(MediaType.APPLICATION_JSON))
                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.msg").value("해당 폴더의 파일 목록을 불러왔습니다."))
                    .andExpect(jsonPath("$.data.files").isArray())
                    .andExpect(jsonPath("$.data.files.length()").value(2))
                    .andExpect(jsonPath("$.data.files[0].dataSourceId").value(10))
                    .andExpect(jsonPath("$.data.files[0].title").value("spec.pdf"))
                    .andExpect(jsonPath("$.data.files[0].summary").value("요약 A"))
                    .andExpect(jsonPath("$.data.files[0].sourceUrl").value("http://src/a"))
                    .andExpect(jsonPath("$.data.files[0].imageUrl").value("http://img/a"))
                    .andExpect(jsonPath("$.data.files[0].tags[0].tagName").value("tag1"));
        }
    }

    @Test
    @DisplayName("폴더 내 파일 목록 조회 - 폴더가 없으면 404")
    void getFilesInFolder_notFound() throws Exception {
        // given
        try (MockedStatic<StubAuthUtil> mocked = mockStatic(StubAuthUtil.class)) {
            mocked.when(StubAuthUtil::currentMemberId).thenReturn(100);
            when(folderService.getFilesInFolderForPersonal(100, 999))
                    .thenThrow(new NoResultException("존재하지 않는 폴더입니다."));

            // when & then
            mockMvc.perform(get("/api/v1/archive/folder/{folderId}/files", 999)
                            .accept(MediaType.APPLICATION_JSON))
                    // then
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("404"))
                    .andExpect(jsonPath("$.msg").value("존재하지 않는 폴더입니다."));
        }
    }

}
