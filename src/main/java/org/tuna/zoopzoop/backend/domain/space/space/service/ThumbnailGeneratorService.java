package org.tuna.zoopzoop.backend.domain.space.space.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.global.aws.S3Service;

@Service
@RequiredArgsConstructor
public class ThumbnailGeneratorService {
    private final S3Service s3Service;
    private final SpaceService spaceService;

    @Async // 비동기 실행
    public void generateAndUploadThumbnail(Integer workspaceId) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(); // Chromium 브라우저 실행
            Page page = browser.newPage();

            // 1. 썸네일 생성용 내부 URL로 이동
            // (인증을 위한 임시 토큰 등을 쿼리 파라미터로 추가할 수 있습니다.)
            //String thumbnailUrl = "http://localhost:8080/internal/render/workspace/" + workspaceId + "?auth_token=TEMP_TOKEN";
            String thumbnailUrl = "https://www.naver.com"; // 테스트용 URL
            page.navigate(thumbnailUrl);

            // 2. 대시보드 컨텐츠가 모두 로드될 때까지 대기
            page.waitForSelector("#dashboard-container"); // 대시보드 컨테이너의 CSS 선택자

            // 3. 특정 요소만 스크린샷으로 찍기
            Locator dashboardElement = page.locator("#dashboard-container");
            byte[] screenshotBytes = dashboardElement.screenshot();

            // 4. S3에 업로드
            // 파일 이름은 유니크하게 설정 (e.g., workspace_1_thumbnail.png)
            //String fileName = "thumbnails/workspace_" + workspaceId + ".png";
            String fileName = "thumbnails/test_thumbnail.png"; // 테스트용 파일 이름
            String s3Url = s3Service.upload(screenshotBytes, fileName, "image/png");

            // 5. 워크스페이스 정보에 썸네일 URL 업데이트
            //spaceService.updateThumbnailUrl(workspaceId, s3Url);

            browser.close();
        } catch (Exception e) {
            // 에러 처리 로직
            e.printStackTrace();
        }
    }
}
