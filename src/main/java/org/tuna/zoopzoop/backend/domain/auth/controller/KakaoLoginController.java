package org.tuna.zoopzoop.backend.domain.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.auth.service.KakaoAuthService;
import org.tuna.zoopzoop.backend.global.config.jwt.JwtProperties;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class KakaoLoginController {
    private final KakaoAuthService kakaoAuthService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @GetMapping("/oauth/kakao")
    public ResponseEntity<RsData<Map<String, String>>> kakaoCallback(@RequestParam String code) {
        Map<String, String> tokens = kakaoAuthService.loginWithKakao(code);
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokens.get("accessToken"))
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.getAccessTokenValidity() / 1000)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken",  tokens.get("refreshToken"))
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtProperties.getRefreshTokenValidity() / 1000)
                .sameSite("Lax")
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(new RsData<>(
                        "200",
                        "카카오 로그인에 성공했습니다.",
                        tokens
                        )
                );
    }
}
