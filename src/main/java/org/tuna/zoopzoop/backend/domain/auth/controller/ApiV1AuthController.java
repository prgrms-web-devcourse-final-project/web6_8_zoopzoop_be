package org.tuna.zoopzoop.backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.auth.dto.AuthResultData;
import org.tuna.zoopzoop.backend.domain.auth.entity.AuthResult;
import org.tuna.zoopzoop.backend.domain.auth.entity.RefreshToken;
import org.tuna.zoopzoop.backend.domain.auth.service.RefreshTokenService;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
@Tag(name = "ApiV1AuthController", description = "인증/인가 REST API 컨트롤러")
public class ApiV1AuthController {
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthResult authResult;

    /**
     * 사용자 로그아웃 API
     * @param response Servlet 기반 웹에서 server -> client로 http 응답을 보내기 위한 객체, 자동 주입.
     */
    @GetMapping("/logout")
    @Operation(summary = "사용자 로그아웃")
    public ResponseEntity<RsData<Void>> logout(
            @CookieValue(name = "sessionId")
            String sessionId,
            HttpServletResponse response) {

        // 서버에서 RefreshToken 삭제
        refreshTokenService.deleteBySessionId(sessionId);

        // 클라이언트 쿠키 삭제 (AccessToken + SessionId)
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie sessionCookie = ResponseCookie.from("sessionId", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>("200", "정상적으로 로그아웃 했습니다.", null));
    }

    /**
     * refreshToken 기반으로 accessToken 재발급
     * @param sessionId 쿠키에 포함된 현재 로그인한 사용자의 sessionId.
     * @param response Servlet 기반 웹에서 server -> client로 http 응답을 보내기 위한 객체, 자동 주입.
     */
    @PostMapping("/refresh")
    @Operation(summary = "사용자 액세스 토큰 재발급 (서버 저장 RefreshToken 사용)")
    public ResponseEntity<RsData<Void>> refreshToken(
            @CookieValue(name = "sessionId")
            String sessionId,
            HttpServletResponse response
    ) {
        if (sessionId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401", "세션이 존재하지 않습니다.", null));
        }

        // sessionId로 RefreshToken 조회
        RefreshToken refreshTokenEntity;
        try {
            refreshTokenEntity = refreshTokenService.getBySessionId(sessionId);
        } catch (AuthenticationException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>(
                            "401",
                            e.getMessage(),
                            null
                    ));
        }

        String refreshToken = refreshTokenEntity.getRefreshToken();

        // RefreshToken 유효성 검사
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401", "유효하지 않은 리프레시 토큰입니다.", null));
        }

        Member member = refreshTokenEntity.getMember();

        // 새 AccessToken 발급
        String newAccessToken = jwtUtil.generateToken(member);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtUtil.getAccessTokenValiditySeconds())
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>("200", "액세스 토큰을 재발급 했습니다.", null));
    }

    /**
     * 확장프로그램의 액세스 토큰 발급을 위한 백그라운드 풀링에 대응하는 API
     * @param state 확장프로그램 로그인 시 전달한 state 값.
     */
    @GetMapping("/result")
    @Operation(summary = "확장프로그램 백그라운드 풀링 대응 API")
    public ResponseEntity<RsData<AuthResultData>> pullingResult(
            @RequestParam String state
    ) {
        AuthResultData resultData = authResult.get(state);
        if(resultData == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new RsData<>(
                            "404",
                            "state에 해당하는 토큰이 준비되지 않았거나, 잘못된 state 입니다.",
                            null
                    )
            );
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>(
                        "200",
                        "토큰이 정상적으로 발급되었습니다.",
                        resultData
                ));
    }
}
