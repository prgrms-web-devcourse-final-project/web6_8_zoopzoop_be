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

        return new RsData<resBodyForCreateFolder>(
                "200",
                rq.folderName() + " 폴더가 생성됐습니다.",
                rs
        );

    }



}
