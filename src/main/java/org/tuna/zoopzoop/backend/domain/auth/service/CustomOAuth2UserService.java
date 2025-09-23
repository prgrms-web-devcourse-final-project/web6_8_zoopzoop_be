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

    private final List<OAuth2UserInfoService> oauth2UserInfoServices;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfoService userInfoService = oauth2UserInfoServices.stream()
                .filter(service -> service.supports(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + registrationId));

        Member member = userInfoService.processUser(oAuth2User.getAttributes());

        return oAuth2User; // 필요 시 커스텀 OAuth2User로 변환 가능
    }
}