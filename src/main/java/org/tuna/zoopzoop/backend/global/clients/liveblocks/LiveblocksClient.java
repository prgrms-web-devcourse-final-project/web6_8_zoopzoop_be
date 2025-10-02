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

    /**
     * Liveblocks 서버의 방을 삭제합니다.
     * @param roomId 삭제할 방의 고유 ID
     */
    public void deleteRoom(String roomId) {
        String deleteUrl = LIVEBLOCKS_API_URL + "/" + roomId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, requestEntity, Void.class);
            log.info("Liveblocks room deleted successfully. roomId: {}", roomId);
        } catch (RestClientException e) {
            log.error("Error while calling Liveblocks API to delete room. roomId: {}", roomId, e);
            // 방 삭제 실패가 전체 로직에 큰 영향을 주지 않는다면,
            // 예외를 던지는 대신 에러 로그만 남기고 넘어갈 수도 있습니다.
            // 여기서는 일단 예외를 던져서 트랜잭션을 롤백하도록 합니다.
            throw new RuntimeException("Liveblocks API call failed", e);
        }
    }
}