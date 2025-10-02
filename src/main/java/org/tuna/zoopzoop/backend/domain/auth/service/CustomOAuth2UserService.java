package org.tuna.zoopzoop.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    // @RequiredArgsConstructor 어노테이션을 통해, OAuth2UserInfoService를 인터페이스로 사용하는
    // GoogleUserInfoService, KakaoUserInfoService를 한번에 주입.
    private final List<OAuth2UserInfoService> oauth2UserInfoServices;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // SpringBoot OAuth2 공급자에서 사용자 정보 받아오기.
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        // 공급자(Google, Kakao) 받아오기.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // oauth2UserInfoService 리스트를 순회하며 공급자를 지원하는 서비스를 찾음.
        OAuth2UserInfoService userInfoService = oauth2UserInfoServices.stream()
                .filter(service -> service.supports(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(registrationId + "는 지원하지 않는 소셜 로그인입니다."));
                // 지원하지 않는 공급자의 경우 예외 발생.
                // 하지만 발생할 일 없는 예외.

        // 선택된 서비스에서 사용자 정보 처리.
        Member member = userInfoService.processUser(oAuth2User.getAttributes());

        return oAuth2User;
    }
}