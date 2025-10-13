package org.tuna.zoopzoop.backend.global.test;

import io.sentry.Sentry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/test")
@Tag(name = "ApiV1TestController", description = "사용자 REST API 컨트롤러")
public class ApiV1TestController {
    @GetMapping
    @Operation(summary = "사용자 정보 조회")
    public ResponseEntity<RsData<Void>> test(
    ) {
        try {
            throw new Exception("Sentry 모니터링 테스트 예외입니다.");
        } catch (Exception e) {
            Sentry.captureException(e);
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "테스트 메소드를 실행합니다.",
                                null
                        )
                );
    }
}