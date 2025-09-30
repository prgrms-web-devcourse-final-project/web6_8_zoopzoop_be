package org.tuna.zoopzoop.backend.domain.space.archive.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.reqBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.resBodyForCreateFolder;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.archive.service.SpaceArchiveFolderService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spaces/{spaceId}/archive/folder")
@RequiredArgsConstructor
@Tag(name = "SpaceArchiveFolder", description = "공유 아카이브의 폴더 CRUD")
public class SpaceArchiveFolderController {

    private final SpaceArchiveFolderService spaceService;

    /**
     * 공유 아카이브 안에 새 폴더 생성
     */
    @Operation(summary = "폴더 생성", description = "해당 스페이스의 공유 아카이브에 새 폴더를 생성합니다.")
    @PostMapping
    public RsData<resBodyForCreateFolder> createFolder(
            @PathVariable Integer spaceId,
            @Valid @RequestBody reqBodyForCreateFolder rq,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Member requester = principal.getMember();
        FolderResponse r = spaceService.createFolder(spaceId, requester, rq.folderName());
        return new RsData<>(
                "200",
                rq.folderName() + " 폴더가 생성되었습니다.",
                new resBodyForCreateFolder(r.folderName(), r.folderId())
        );
    }

    /**
     * 공유 아카이브의 폴더 삭제
     */
    @Operation(summary = "폴더 삭제", description = "해당 스페이스의 공유 아카이브에서 폴더를 삭제합니다.")
    @DeleteMapping("/{folderId}")
    public Map<String, Object> deleteFolder(
            @PathVariable Integer spaceId,
            @PathVariable Integer folderId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (folderId == 0) {
            return Map.of("status", 409, "msg", "default 폴더는 삭제할 수 없습니다.", "data", null);
        }

        String deletedFolderName = spaceService.deleteFolder(spaceId, principal.getMember(), folderId);
        String msg = (deletedFolderName != null) ? deletedFolderName + " 폴더가 삭제됐습니다." : "폴더가 삭제됐습니다.";

        var body = new HashMap<String, Object>();
        body.put("status", 200);
        body.put("msg", msg);
        body.put("data", null);
        return body;
    }

    /**
     * 공유 아카이브의 폴더 이름 수정
     */
    @Operation(summary = "폴더 이름 수정", description = "해당 스페이스의 공유 아카이브에서 폴더 이름을 변경합니다.")
    @PatchMapping("/{folderId}")
    public Map<String, Object> updateFolderName(
            @PathVariable Integer spaceId,
            @PathVariable Integer folderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (folderId == 0) {
            return Map.of("status", 400, "msg", "default 폴더는 이름을 변경할 수 없습니다.", "data", null);
        }
        String updated = spaceService.updateFolderName(spaceId, principal.getMember(), folderId, body.get("folderName"));
        return Map.of(
                "status", 200,
                "msg", "폴더 이름이 " + updated + " 으로 변경됐습니다.",
                "data", Map.of("folderName", updated)
        );
    }

    /**
     * 공유 아카이브의 폴더 목록 조회
     */
    @Operation(summary = "폴더 이름 조회", description = "해당 스페이스의 공유 아카이브 폴더 목록을 조회합니다.")
    @GetMapping
    public Map<String, Object> listFolders(
            @PathVariable Integer spaceId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<FolderResponse> folders = spaceService.listFolders(spaceId, principal.getMember());
        return Map.of(
                "status", 200,
                "msg", "공유 아카이브의 폴더 목록을 불러왔습니다.",
                "data", Map.of("folders", folders)
        );
    }

    /**
     * 공유 아카이브의 특정 폴더 내 파일 목록 조회
     */
    @Operation(summary = "폴더 내 파일 조회", description = "해당 스페이스의 공유 아카이브에서 특정 폴더 내 파일 목록을 조회합니다.")
    @GetMapping("/{folderId}/files")
    public Map<String, Object> filesInFolder(
            @PathVariable Integer spaceId,
            @PathVariable Integer folderId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Integer target = (folderId == 0)
                ? spaceService.getDefaultFolderId(spaceId, principal.getMember())
                : folderId;

        FolderFilesDto rs = spaceService.getFilesInFolder(spaceId, principal.getMember(), target);

        return Map.of(
                "status", 200,
                "msg", folderId == 0 ? "기본 폴더의 파일 목록을 불러왔습니다." : "해당 폴더의 파일 목록을 불러왔습니다.",
                "data", Map.of(
                        "folder", Map.of("folderId", rs.folderId(), "folderName", rs.folderName()),
                        "files", rs.files()
                )
        );
    }
}

