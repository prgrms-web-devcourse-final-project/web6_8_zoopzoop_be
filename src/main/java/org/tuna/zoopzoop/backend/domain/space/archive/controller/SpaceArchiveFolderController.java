package org.tuna.zoopzoop.backend.domain.space.archive.controller;

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
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.archive.service.SpaceArchiveFolderService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/space/{spaceId}/archive/folder")
@RequiredArgsConstructor
@Tag(name = "ApiV1SpaceArchiveFolder", description = "공유 아카이브의 폴더 CRUD")
public class SpaceArchiveFolderController {

    private final SpaceArchiveFolderService spaceArchiveFolderService;

    /**
     * 공유 아카이브 안에 새 폴더 생성
     */
    @Operation(summary = "폴더 생성", description = "해당 스페이스의 공유 아카이브에 새 폴더를 생성합니다.")
    @PostMapping
    public RsData<resBodyForCreateFolder> createFolder(
            @PathVariable Integer spaceId,
            @Valid @RequestBody reqBodyForCreateFolder rq,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member requester = userDetails.getMember();
        FolderResponse fr = spaceArchiveFolderService.createFolder(spaceId, requester, rq.folderName());
        resBodyForCreateFolder rs = new resBodyForCreateFolder(fr.folderName(), fr.folderId());
        return new RsData<>(
                "200",
                rq.folderName() + " 폴더가 생성되었습니다.",
                rs
        );
    }

    /**
     * 공유 아카이브의 폴더 삭제
     */
    @Operation(summary = "폴더 삭제", description = "해당 스페이스의 공유 아카이브에서 폴더를 삭제합니다.")
    @DeleteMapping("/{folderId}")
    public ResponseEntity<RsData<?>> deleteFolder(
            @PathVariable Integer spaceId,
            @PathVariable Integer folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (folderId == 0)
            throw new IllegalArgumentException("default 폴더는 삭제할 수 없습니다.");


        String deletedFolderName = spaceArchiveFolderService.deleteFolder(spaceId, userDetails.getMember(), folderId);
        return ResponseEntity.ok().body(
                new RsData<>("200", deletedFolderName + " 폴더가 삭제됐습니다.", null)
        );
    }

    /**
     * 공유 아카이브의 폴더 이름 수정
     */
    @Operation(summary = "폴더 이름 수정", description = "해당 스페이스의 공유 아카이브에서 폴더 이름을 변경합니다.")
    @PatchMapping("/{folderId}")
    public ResponseEntity<RsData<?>> updateFolderName(
            @PathVariable Integer spaceId,
            @PathVariable Integer folderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (folderId == 0)
            throw new IllegalArgumentException("default 폴더는 이름을 변경할 수 없습니다.");

        String updatename = spaceArchiveFolderService.updateFolderName(spaceId, principal.getMember(), folderId, body.get("folderName"));
        return ResponseEntity.ok().body(
                new RsData<>("200", "폴더 이름이 " + updatename + "(으)로 변경됐습니다.", new FolderResponse(updatename, folderId))
        );
    }

    /**
     * 공유 아카이브의 폴더 목록 조회
     */
    @Operation(summary = "폴더 이름 조회", description = "해당 스페이스의 공유 아카이브 폴더 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<RsData<List<FolderResponse>>> listFolders(
            @PathVariable Integer spaceId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FolderResponse> folders = spaceArchiveFolderService.getFolders(spaceId, userDetails.getMember());
        return ResponseEntity.ok().body(
                new RsData<>("200", "공유 아카이브의 폴더 목록이 조회되었습니다.", folders)
        );
    }

    /**
     * 공유 아카이브의 특정 폴더 내 파일 목록 조회
     */
    @Operation(summary = "폴더 내 파일 조회", description = "해당 스페이스의 공유 아카이브에서 특정 폴더 내 파일 목록을 조회합니다.")
    @GetMapping("/{folderId}/files")
    public ResponseEntity<RsData<FolderFilesDto>> filesInFolder(
            @PathVariable Integer spaceId,
            @PathVariable Integer folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer target = (folderId == 0)
                ? spaceArchiveFolderService.getDefaultFolderId(spaceId, userDetails.getMember())
                : folderId;

        FolderFilesDto rs = spaceArchiveFolderService.getFilesInFolder(spaceId, userDetails.getMember(), target);

        return ResponseEntity.ok().body(
                new RsData<>("200", "폴더 안의 파일 목록을 불러왔습니다.", rs)
        );
    }
}

