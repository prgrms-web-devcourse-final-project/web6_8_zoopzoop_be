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
import org.tuna.zoopzoop.backend.domain.archive.folder.service.PersonalArchiveFolderService;
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

    private final PersonalArchiveFolderService personalArchiveFolderService;

    /**
     * 내 PersonalArchive 안에 새 폴더 생성
     */
    @Operation(summary = "폴더 생성", description = "내 PersonalArchive 안에 새 폴더를 생성합니다.")
    @PostMapping
    public RsData<resBodyForCreateFolder> createFolder(
            @Valid @RequestBody reqBodyForCreateFolder rq,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        FolderResponse createFile = personalArchiveFolderService.createFolder(member.getId(), rq.folderName());
        resBodyForCreateFolder rs = new resBodyForCreateFolder(createFile.folderName(), createFile.folderId());

        return new RsData<>("200",rq.folderName() + " 폴더가 생성됐습니다.", rs);
    }

    /**
     * 내 PersonalArchive 안의 folder 삭제
     */
    @DeleteMapping("/{folderId}")
    public ResponseEntity<RsData<?>> deleteFolder(
            @PathVariable Integer folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (folderId == 0)
            throw new IllegalArgumentException("default 폴더는 삭제할 수 없습니다.");


        Member member = userDetails.getMember();
        String deletedFolderName = personalArchiveFolderService.deleteFolder(member.getId(), folderId);

        return ResponseEntity.ok(
                new RsData<>("200", deletedFolderName + " 폴더가 삭제됐습니다.", null)
        );
    }

    /**
     * 폴더 이름 수정
     * @param folderId 수정할 폴더 Id
     * @param body  수정할 폴더 값
     */
    @PatchMapping("/{folderId}")
    public ResponseEntity<RsData<Map<String, String>>> updateFolderName(
            @PathVariable Integer folderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (folderId == 0)
            throw new IllegalArgumentException("default 폴더는 이름을 변경할 수 없습니다.");


        Member member = userDetails.getMember();
        String newName = body.get("folderName");
        String updatedName = personalArchiveFolderService.updateFolderName(member.getId(), folderId, newName);

        return ResponseEntity.ok(
                new RsData<>("200", "폴더 이름이 " + updatedName + " 으로 변경됐습니다.",
                        Map.of("folderName", updatedName))
        );
    }

    /**
     *  개인 아카이브의 폴더 이름 전부 조회
     *  "default", "폴더1", "폴더2"
     */
    @Operation(summary = "폴더 이름 조회", description = "내 PersonalArchive 안에 이름을 전부 조회합니다.")
    @GetMapping
    public ResponseEntity<RsData<List<FolderResponse>>> getFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        List<FolderResponse> folders = personalArchiveFolderService.getFolders(member.getId());

        return ResponseEntity.ok(
                new RsData<>("200", "개인 아카이브의 폴더 목록을 불러왔습니다.", folders)
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
                ? personalArchiveFolderService.getDefaultFolderId(memberId)
                : folderId;

        FolderFilesDto rs = personalArchiveFolderService.getFilesInFolder(memberId, targetFolderId);

        return ResponseEntity.ok(
                new RsData<>("200","해당 폴더의 파일 목록을 불러왔습니다.", rs)
        );
    }

}
