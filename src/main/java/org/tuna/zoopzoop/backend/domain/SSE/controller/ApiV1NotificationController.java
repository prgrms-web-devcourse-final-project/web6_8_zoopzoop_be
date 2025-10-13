package org.tuna.zoopzoop.backend.domain.SSE.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tuna.zoopzoop.backend.domain.SSE.service.EmitterService;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class ApiV1NotificationController {
    private final EmitterService emitterService;

    /**
     * SSE 구독 엔드포인트
     * @param userDetails - 현재 인증된 사용자 정보
     * @return SseEmitter - 클라이언트와의 SSE 연결을 관리하는 객체
     */
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 1. 현재 로그인한 사용자의 ID를 가져옴
        Long memberId = (long) userDetails.getMember().getId();

        // 2. EmitterService를 통해 Emitter를 생성하고 반환
        return emitterService.addEmitter(memberId);
    }
}
