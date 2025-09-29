package org.tuna.zoopzoop.backend.domain.auth.entity;

import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.auth.dto.AuthResultData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthResult {
    private final Map<String, AuthResultData> results = new ConcurrentHashMap<>();

    public void put(String state, String accessToken, String sessionId) {
        results.put(state, new AuthResultData(accessToken, sessionId));
    }

    public AuthResultData get(String state) {
        return results.remove(state);
    }

    public void consume(String state) {
        results.remove(state);
    }
}