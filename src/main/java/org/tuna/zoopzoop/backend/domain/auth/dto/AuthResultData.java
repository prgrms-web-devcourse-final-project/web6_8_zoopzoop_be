package org.tuna.zoopzoop.backend.domain.auth.dto;

public class AuthResultData {
    private final String accessToken;
    private final String sessionId;

    public AuthResultData(String accessToken, String sessionId) {
        this.accessToken = accessToken;
        this.sessionId = sessionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getSessionId() {
        return sessionId;
    }
}