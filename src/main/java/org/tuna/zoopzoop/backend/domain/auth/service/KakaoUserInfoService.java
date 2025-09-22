package org.tuna.zoopzoop.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoUserInfoService implements OAuth2UserInfoService {
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Override
    public boolean supports(String registrationId) {
        return "kakao".equalsIgnoreCase(registrationId);
    }

    @Override
    public Member processUser(Map<String, Object> attributes) {
        String kakaoId = attributes.get("id").toString();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String name = (String) profile.get("nickname");
        String profileImage = (String) profile.get("profile_image_url");

        return memberRepository.findByProviderAndProviderKey(Provider.KAKAO,kakaoId)
                .orElseGet(() -> memberService.createMember(name, profileImage, kakaoId, Provider.KAKAO));
    }
}