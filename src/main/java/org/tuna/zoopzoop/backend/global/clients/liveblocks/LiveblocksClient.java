package org.tuna.zoopzoop.backend.global.clients.liveblocks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveblocksClient {

    private final RestTemplate restTemplate;

    @Value("${liveblocks.secret-key}")
    private String secretKey;

    private static final String LIVEBLOCKS_API_URL = "https://api.liveblocks.io/v2/rooms";

    /**
     * Liveblocks 서버에 새로운 방을 생성합니다.
     * @param roomId 생성할 방의 고유 ID (워크스페이스 ID와 동일하게 사용)
     */
    public void createRoom(String roomId) {
        // 1. HTTP 헤더 설정 (Authorization: Bearer sk_...)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Request Body 생성 (비공개 방으로 생성)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", roomId);
        requestBody.put("defaultAccesses", Collections.emptyList()); // 비공개(private) 방으로 설정

        // 3. HTTP 요청 엔티티 생성
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. Liveblocks API에 POST 요청 전송
            ResponseEntity<String> response = restTemplate.postForEntity(LIVEBLOCKS_API_URL, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Liveblocks room created successfully. roomId: {}", roomId);
            } else {
                log.error("Failed to create Liveblocks room. roomId: {}, status: {}, body: {}",
                        roomId, response.getStatusCode(), response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error while calling Liveblocks API to create room. roomId: {}", roomId, e);
            // 필요하다면 여기서 커스텀 예외를 발생시켜 서비스 레이어에서 처리하도록 할 수 있습니다.
            throw new RuntimeException("Liveblocks API call failed", e);
        }
    }

    // 여기에 나중에 방 삭제, 방 업데이트 등의 메소드를 추가하면 됩니다.
    // public void deleteRoom(String roomId) { ... }
}