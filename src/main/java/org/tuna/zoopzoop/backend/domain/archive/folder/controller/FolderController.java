package org.tuna.zoopzoop.backend.domain.archive.folder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.reqBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.resBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive/folder")
@RequiredArgsConstructor
@Tag(name = "ApiV1Folder", description = "개인 아카이브의 폴더 CRUD")
public class FolderController {

    private final FolderService folderService;

    /**
     * 내 PersonalArchive 안에 새 폴더 생성
     * @param rq reqBodyForCreateFolder
     * @return resBodyForCreateFolder
     */
    @Operation(summary = "폴더 생성", description = "내 PersonalArchive 안에 새 폴더를 생성합니다.")
    @PostMapping
    public RsData<resBodyForCreateFolder> createFolder(
            @Valid @RequestBody reqBodyForCreateFolder rq,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        FolderResponse createFile = folderService.createFolderForPersonal(member.getId(), rq.folderName());
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
    public ResponseEntity<Map<String, Object>> deleteFolder(
            @PathVariable Integer folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (folderId == 0) {
            var body = new java.util.HashMap<String, Object>();
            body.put("status", 400);
            body.put("msg", "default 폴더는 삭제할 수 없습니다.");
            body.put("data", null); // HashMap은 null 허용
            return ResponseEntity.badRequest().body(body);
        }

        Member member = userDetails.getMember();
        String deletedFolderName = folderService.deleteFolder(member.getId(), folderId);

        var body = new java.util.HashMap<String, Object>();
        body.put("status", 200);
        body.put("msg", deletedFolderName + " 폴더가 삭제됐습니다.");
        body.put("data", null); // <- 여기도 Map.of 쓰면 NPE 납니다
        return ResponseEntity.ok(body);
    }

    /**
     * 폴더 이름 수정
     * @param folderId 수정할 폴더 Id
     * @param body  수정할 폴더 값
     */
    @PatchMapping("/{folderId}")
    public ResponseEntity<Map<String, Object>> updateFolderName(
            @PathVariable Integer folderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (folderId == 0) {
            var res = new java.util.HashMap<String, Object>();
            res.put("status", 400);
            res.put("msg", "default 폴더는 이름을 변경할 수 없습니다.");
            res.put("data", null);
            return ResponseEntity.badRequest().body(res);
        }

        Member member = userDetails.getMember();
        String newName = body.get("folderName");
        String updatedName = folderService.updateFolderName(member.getId(), folderId, newName);

        return ResponseEntity.ok(java.util.Map.of(
                "status", 200,
                "msg", "폴더 이름이 " + updatedName + " 으로 변경됐습니다.",
                "data", java.util.Map.of("folderName", updatedName)
        ));
    }

    /**
     *  개인 아카이브의 폴더 이름 전부 조회
     *  "default", "폴더1", "폴더2"
     */
    @Operation(summary = "폴더 이름 조회", description = "내 PersonalArchive 안에 이름을 전부 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        List<FolderResponse> folders = folderService.getFoldersForPersonal(member.getId());

        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "msg", "개인 아카이브의 폴더 목록을 불러왔습니다.",
                        "data", Map.of("folders", folders)
                )
        );
    }

    /**
     * 폴더 안의 파일 목록 조회
     */
    @GetMapping("/{folderId}/files")
    public ResponseEntity<?> getFilesInFolder(
            @PathVariable Integer folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int memberId = userDetails.getMember().getId();

        Integer targetFolderId = (folderId == 0)
                ? folderService.getDefaultFolderId(memberId)
                : folderId;

        FolderFilesDto rs = folderService.getFilesInFolderForPersonal(memberId, targetFolderId);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "msg", folderId == 0 ? "기본 폴더의 파일 목록을 불러왔습니다." : "해당 폴더의 파일 목록을 불러왔습니다.",
                "data", Map.of(
                        "folder", Map.of(
                                "folderId", rs.folderId(),
                                "folderName", rs.folderName()
                        ),
                        "files", rs.files()
                )
        ));
    }

}
