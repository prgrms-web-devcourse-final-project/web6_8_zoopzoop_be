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
public class GoogleUserInfoService implements OAuth2UserInfoService {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Override
    public boolean supports(String registrationId) {
        return "google".equalsIgnoreCase(registrationId);
    }

    @Override
    public Member processUser(Map<String, Object> attributes) {
        String googleId = (String) attributes.get("sub"); // 구글 user-id
        String name = (String) attributes.get("name");
        String profileImage = (String) attributes.get("picture");

        return memberRepository.findByProviderAndProviderKey(Provider.GOOGLE, googleId)
                .orElseGet(() -> memberService.createMember(name, profileImage, googleId, Provider.GOOGLE));
    }
}