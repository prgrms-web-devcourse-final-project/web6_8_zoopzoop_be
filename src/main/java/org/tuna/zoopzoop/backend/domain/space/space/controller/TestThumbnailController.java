package org.tuna.zoopzoop.backend.domain.space.space.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.space.space.service.ThumbnailGeneratorService;

@RestController
@RequestMapping("/test") // "/test" 라는 공통 경로를 사용
@RequiredArgsConstructor
public class TestThumbnailController {
    private final ThumbnailGeneratorService thumbnailGeneratorService;

    @GetMapping("/generate-thumbnail") // GET /test/generate-thumbnail 요청을 처리
    public String testGenerateThumbnail() {
        // 테스트 목적으로 workspaceId는 임의의 값(예: 1)을 사용합니다.
        thumbnailGeneratorService.generateAndUploadThumbnail(1);

        return "썸네일 생성 및 업로드 요청을 보냈습니다. 서버 로그와 S3를 확인해주세요.";
    }
}
