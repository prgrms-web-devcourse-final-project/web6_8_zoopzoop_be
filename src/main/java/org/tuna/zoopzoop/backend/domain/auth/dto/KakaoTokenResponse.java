package org.tuna.zoopzoop.backend.domain.auth.dto;

public record KakaoTokenResponse(
        String access_token,
        String token_type,
        String refresh_token,
        Long expires_in,
        String scope,
        Long refresh_token_expires_in
) {}
