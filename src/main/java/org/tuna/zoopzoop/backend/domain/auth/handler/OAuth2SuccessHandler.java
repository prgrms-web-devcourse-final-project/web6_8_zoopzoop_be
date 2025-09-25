package org.tuna.zoopzoop.backend.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.global.config.jwt.JwtProperties;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Value("${front.redirect_domain}")
    private String redirect_domain;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // OAuth2 로그인 사용자의 속성
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 소셜 로그인 공급자(Google, Kakao)
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        // 공급자 별로 DB 에서 회원 조회
        Member member;
        if ("kakao".equals(registrationId)) {
            String kakaoId = oAuth2User.getAttributes().get("id").toString();
            member = memberService.findByKakaoKey(kakaoId);
        } else if ("google".equals(registrationId)) {
            String googleId = (String) oAuth2User.getAttributes().get("sub");
            member = memberService.findByGoogleKey(googleId);
        } else {
            throw new IllegalArgumentException(registrationId + "는 지원하지 않는 소셜 로그인입니다.");
        }

        // 조회된 회원 정보를 기반으로 AccessToken 및 RefreshToken 생성
        String accessToken = jwtUtil.generateToken(member);
        String refreshToken = jwtUtil.generateRefreshToken(member);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.getAccessTokenValidity() / 1000)
                // .domain() // 프론트엔드 & 백엔드 상위 도메인
                // .secure(true) // https 필수 설정.
                .sameSite("None")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.getRefreshTokenValidity() / 1000)
                // .domain() // 프론트엔드 & 백엔드 상위 도메인
                // .secure(true) // https 필수 설정.
                .sameSite("None")
                .build();

        // HTTP 응답에서 쿠키 값 추가.
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 로그인 성공 후 리다이렉트.
        // 배포 시에 프론트엔드와 조율이 필요한 부분일 듯 함.
        response.sendRedirect(redirect_domain + "/auth/callback");

        // 보안을 좀 더 강화하고자 한다면 CSRF 토큰 같은 걸 생각해볼 수 있겠으나,
        // 일단은 구현하지 않음.(개발 과정 중에 번거로워질 수 있을 듯 함.)
    }
}