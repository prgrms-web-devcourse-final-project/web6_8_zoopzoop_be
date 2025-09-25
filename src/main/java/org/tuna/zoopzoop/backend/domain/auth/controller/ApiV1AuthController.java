package org.tuna.zoopzoop.backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.global.config.jwt.JwtProperties;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
@Tag(name = "ApiV1AuthController", description = "인증/인가 REST API 컨트롤러")
public class ApiV1AuthController {
    private final JwtUtil jwtUtil;
    private final MemberService memberService;
    private final JwtProperties jwtProperties;

    /**
     * 사용자 로그아웃 API
     * @param response Servlet 기반 웹에서 server -> client로 http 응답을 보내기 위한 객체, 자동 주입.
     */
    @GetMapping("/logout")
    @Operation(summary = "사용자 로그아웃")
    public ResponseEntity<RsData<Void>> logout(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) // 쿠키 삭제
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) // 쿠키 삭제
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>(
                                "200",
                                "정상적으로 로그아웃 했습니다.",
                                null
                        )
                );
    }

    /**
     * refreshToken 기반으로 accessToken 재발급
     * @param refreshToken 쿠키에 포함된 현재 로그인한 사용자의 refreshToken
     * @param response Servlet 기반 웹에서 server -> client로 http 응답을 보내기 위한 객체, 자동 주입.
     */
    @PostMapping("/refresh")
    @Operation(summary = "사용자 액세스 토큰 재발급 (리프레시 토큰이 유효할 경우)")
    public ResponseEntity<RsData<Void>> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                                         HttpServletResponse response) {

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>(
                            "401",
                            "유효하지 않은 리프레시 토큰입니다.",
                            null
                    ));
        }

        String providerKey = jwtUtil.getProviderKeyFromToken(refreshToken);
        Member member = memberService.findByProviderKey(providerKey);

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
                .body(new RsData<>(
                                "200",
                                "액세스 토큰을 재발급 했습니다.",
                                null
                ));
    }
}
