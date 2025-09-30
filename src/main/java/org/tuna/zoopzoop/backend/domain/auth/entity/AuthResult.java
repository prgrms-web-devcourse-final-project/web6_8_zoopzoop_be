package org.tuna.zoopzoop.backend.domain.auth.entity;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.auth.dto.AuthResultData;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthResult {
    private final RedisTemplate<String, AuthResultData> redisTemplate;
    private static final String PREFIX = "auth:result:";

    public void put(String state, String accessToken, String sessionId) {
        AuthResultData data = new AuthResultData(accessToken, sessionId);
        redisTemplate.opsForValue().set(PREFIX + state, data, Duration.ofMinutes(1)); // TTL 1분, 프론트단에선 백그라운드 풀링 형식으로 계속 작동할 것이므로.
    }

    public AuthResultData get(String state) {
        AuthResultData data = redisTemplate.opsForValue().get(PREFIX + state);
        if (data != null) redisTemplate.delete(PREFIX + state);
        return data;
    }
}