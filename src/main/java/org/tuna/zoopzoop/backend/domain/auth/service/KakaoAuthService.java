package org.tuna.zoopzoop.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.tuna.zoopzoop.backend.domain.auth.dto.KakaoTokenResponse;
import org.tuna.zoopzoop.backend.domain.auth.dto.KakaoUserInfoResponse;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {
    private final WebClient webClient;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @Value("${kakao.client_id}")
    private String CLIENT_ID;
    @Value("${kakao.redirect_uri}")
    private String REDIRECT_URI;

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public Map<String, String> loginWithKakao(String code) {
        // 1. 카카오에서 토큰 발급
        KakaoTokenResponse tokenResponse = webClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", CLIENT_ID)
                        .with("redirect_uri", REDIRECT_URI)
                        .with("code", code))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();

        // 2. 토큰에서 AccessToken 가져오기.
        String accessToken = tokenResponse.access_token();

        // 3. AccessToken을 통해 카카오 사용자 정보 가져오기.
        KakaoUserInfoResponse userInfo = webClient.get()
                .uri(USER_INFO_URL)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();

        // 4. Member 엔티티 리턴
        //     a. kakaoKey 값을 가진 Member 객체가 이미 존재하는 경우, 그대로 가져옴.
        //     b. 존재하지 않을 경우, 새로 만듬.
        Member member = memberRepository.findByKakaoKey(userInfo.id())
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .name(userInfo.kakao_account().profile().nickname())
                                .profileImageUrl(userInfo.kakao_account().profile().profile_image_url())
                                .kakaoKey(userInfo.id())
                                .build()
                ));

        // 5. AccessToken 및 RefreshToken 생성.
        String jwtAccessToken = jwtUtil.generateToken(member);
        String jwtRefreshToken = jwtUtil.generateRefreshToken(member);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", jwtAccessToken);
        tokens.put("refreshToken", jwtRefreshToken);

        return tokens;
    }
}