package org.tuna.zoopzoop.backend.global.aws;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

@RestController
@RequiredArgsConstructor
public class S3TestController {
    private final S3Service s3Service;

    @PostMapping(value = "/test/upload-file", produces = TEXT_HTML_VALUE)
    @Operation(summary = "S3 파일 업로드 테스트")
    public String uploadFile(@RequestParam("fileName") String fileName,
                             @RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = s3Service.upload(file, fileName);
            return """
                   <h1>업로드 성공! ✅</h1>
                   <p>파일명: %s</p>
                   <p>업로드된 URL: <a href="%s" target="_blank">%s</a></p>
                   <br>
                   <a href="/">메인으로 돌아가기</a>
                   """.formatted(fileName, fileUrl, fileUrl);
        } catch (IOException e) {
            // e.printStackTrace(); // 실제 운영 환경에서는 로그를 남기는 것이 좋습니다.
            return """
                   <h1>업로드 실패 ❌</h1>
                   <p>오류: %s</p>
                   <br>
                   <a href="/">메인으로 돌아가기</a>
                   """.formatted(e.getMessage());
        }
    }
}
