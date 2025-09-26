package org.tuna.zoopzoop.backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.tuna.zoopzoop.backend.domain.auth.deprecated.KakaoTokenResponse;
import org.tuna.zoopzoop.backend.domain.auth.deprecated.KakaoUserInfoResponse;
import org.tuna.zoopzoop.backend.domain.auth.service.KakaoUserInfoService;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.global.config.jwt.JwtProperties;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
@Tag(name = "ApiV1AuthController", description = "인증/인가 REST API 컨트롤러")
public class ApiV1AuthController {
    private final JwtUtil jwtUtil;
    private final MemberService memberService;
    private final JwtProperties jwtProperties;
    private final KakaoUserInfoService kakaoUserInfoService;
    private final WebClient webClient;

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

    @GetMapping("/test")
    @Operation(summary = "소셜 로그인 테스트용 엔드 포인트")
    public ResponseEntity<RsData<Map<String, String>>> test(
            @RequestParam String code
            ) {
        Map<String, Object> attributes = getAttributesFromCode(code);
        Member member = kakaoUserInfoService.processUser(attributes);

        String accessToken = jwtUtil.generateToken(member);
        String refreshToken = jwtUtil.generateRefreshToken(member);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>(
                        "200",
                        "로그인 테스트 완료.",
                        tokens
                ));
    }

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    String TOKEN_URL;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    String USER_INFO_URL;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    String CLIENT_ID;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    String REDIRECT_URI;

    public Map<String, Object> getAttributesFromCode(String code) {
        KakaoTokenResponse tokenResponse = webClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type","authorization_code")
                        .with("client_id", CLIENT_ID)
                        .with("redirect_uri", REDIRECT_URI)
                        .with("code", code))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();

        String accessToken = tokenResponse.access_token();

        KakaoUserInfoResponse userInfo = webClient.get()
                .uri(USER_INFO_URL)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", userInfo.id());
        attributes.put("kakao_account", userInfo.kakao_account());
        return attributes;
    }
}
