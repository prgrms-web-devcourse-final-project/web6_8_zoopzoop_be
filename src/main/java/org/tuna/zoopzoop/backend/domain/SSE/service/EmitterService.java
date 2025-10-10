package org.tuna.zoopzoop.backend.domain.SSE.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmitterService {
    // 1. 모든 Emitter를 저장하는 ConcurrentHashMap
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새로운 Emitter 생성 및 저장
     * @param memberId - 사용자 ID
     * @return SseEmitter - 생성된 Emitter 객체
     */
    public SseEmitter addEmitter(Long memberId) {
        // 1시간 타임아웃 설정
        SseEmitter emitter = new SseEmitter(3600L * 1000);
        this.emitters.put(memberId, emitter);

        // Emitter 완료 또는 타임아웃 시 Map에서 삭제
        emitter.onCompletion(() -> this.emitters.remove(memberId));
        emitter.onTimeout(() -> this.emitters.remove(memberId));

        // 503 에러 방지를 위한 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE connected!"));
        } catch (IOException e) {
            // 예외 처리
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 이벤트 전송
     * @param memberId - 사용자 ID
     * @param eventName - 이벤트 이름
     * @param data - 전송할 데이터 객체
     */
    public void sendNotification(Long memberId, String eventName, Object data) {
        SseEmitter emitter = this.emitters.get(memberId);
        if (emitter != null) {
            try {
                // data 객체를 JSON 문자열로 변환하여 전송해야 함 (Controller에서는 자동 변환)
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                this.emitters.remove(memberId);
            }
        }
    }

    /**
     * 20초마다 모든 Emitter에 하트비트 전송
     * 클라이언트와의 연결 유지를 위해 주기적으로 빈 이벤트를 전송
     */
    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        // 모든 Emitter에 하트비트 전송
        emitters.forEach((userId, emitter) -> {
            try {
                // SSE 주석(comment)을 사용하여 클라이언트에서 별도 이벤트를 발생시키지 않음
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException e) {
                // 전송 실패 시, 클라이언트 연결이 끊어진 것으로 간주하고 Map에서 제거
                emitters.remove(userId);
            }
        });
    }
}
