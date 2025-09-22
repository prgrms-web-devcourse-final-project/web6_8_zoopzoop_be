package org.tuna.zoopzoop.backend.domain.archive.folder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.reqBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.resBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.StubAuthUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    /**
     * 내 PersonalArchive 안에 새 폴더 생성
     * @param rq reqBodyForCreateFolder
     * @return resBodyForCreateFolder
     */
    @PostMapping("")
    public RsData<resBodyForCreateFolder> createFolder(
            @Valid @RequestBody reqBodyForCreateFolder rq
    ) {
        // 임시 인증 정보
        Integer currentMemberId = StubAuthUtil.currentMemberId();
        FolderResponse createFile = folderService.createFolderForPersonal(currentMemberId, rq.folderName());

        resBodyForCreateFolder rs = new resBodyForCreateFolder(createFile.folderName(), createFile.folderId());

        return new RsData<>(
                "200",
                rq.folderName() + " 폴더가 생성됐습니다.",
                rs
        );

    }

    /**
     * 내 PersonalArchive 안의 folder 삭제
     * @param folderId  삭제할 folderId
     */
    @DeleteMapping("/{folderId}")
    public ResponseEntity<Map<String, Object>> deleteFolder(@PathVariable Integer folderId) {
        String deletedFolderName = folderService.deleteFolder(folderId);

        Map<String, Object> body = new HashMap<>();
        body.put("status", 200);
        body.put("msg", deletedFolderName + " 폴더가 삭제됐습니다.");
        body.put("data", null);

        return ResponseEntity.ok(body);
    }

    /**
     * 폴더 이름 주성
     * @param folderId 수정할 폴더 Id
     * @param body  수정할 폴더 값
     */
    @PatchMapping("/{folderId}")
    public ResponseEntity<Map<String, Object>> updateFolderName(
            @PathVariable Integer folderId,
            @RequestBody Map<String, String> body
    ) {
        String newName = body.get("folderName");
        String updatedName = folderService.updateFolderName(folderId, newName);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("msg", "폴더 이름이 " + updatedName + " 으로 변경됐습니다.");
        response.put("data", Map.of("folderName", updatedName));

        return ResponseEntity.ok(response);
    }

    /**
     *  개인 아카이브의 폴더 이름 전부 조회
     *  "default", "폴더1", "폴더2"
     */
    @GetMapping("")
    public ResponseEntity<?> getFolders() {
        // 로그인된 멤버 ID 가져오기
        Integer currentMemberId = StubAuthUtil.currentMemberId();

        // 내 personal archive 안의 폴더 조회
        List<FolderResponse> folders = folderService.getFoldersForPersonal(currentMemberId);

        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "msg", "개인 아카이브의 폴더 목록을 불러왔습니다.",
                        "data", Map.of("folders", folders)
                )
        );
    }

    /**
     * 폴더(내 PersonalArchive 소속) 안의 파일 목록 조회
     */
    @GetMapping("/{folderId}/files")
    public ResponseEntity<?> getFilesInFolder(@PathVariable Integer folderId) {
        Integer currentMemberId = StubAuthUtil.currentMemberId();

        FolderFilesDto rs = folderService.getFilesInFolderForPersonal(currentMemberId, folderId);

        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "msg", "해당 폴더의 파일 목록을 불러왔습니다.",
                        "data", Map.of(
                                "folder", Map.of(
                                        "folderId", rs.folderId(),
                                        "folderName", rs.folderName()
                                ),
                                "files", rs.files()
                        )
                )
        );
    }

}
