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
    // Google 소셜 로그인의 경우
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    // 이 서비스(=GoogleUserInfoService)가, 해당 공급자를 지원하는 지에 대한 여부 확인.
    // Google 공급자만 지원.
    @Override
    public boolean supports(String registrationId) {
        return "google".equalsIgnoreCase(registrationId);
    }


    // Google 에서 받은 사용자 정보 Map(=attributes)에서 필요한 값 추출.
    // 이후 추출한 값을 통해 Member 엔티티 생성.
    @Override
    public Member processUser(Map<String, Object> attributes) {
        String googleId = (String) attributes.get("sub"); // 구글 user-id
        String name = (String) attributes.get("name");
        String profileImage = (String) attributes.get("picture");

        return memberRepository.findByProviderAndProviderKey(Provider.GOOGLE, googleId)
                .orElseGet(() -> memberService.createMember(name, profileImage, googleId, Provider.GOOGLE));
    }
}