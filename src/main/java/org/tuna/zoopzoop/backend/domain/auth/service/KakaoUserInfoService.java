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
    // Kakao 소셜 로그인의 경우
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    // 이 서비스(=KakaoUserInfoService)가, 해당 공급자를 지원하는 지에 대한 여부 확인.
    // Kakao 공급자만 지원.
    @Override
    public boolean supports(String registrationId) {
        return "kakao".equalsIgnoreCase(registrationId);
    }

    // Kakao 에서 받은 사용자 정보 Map(=attributes)에서 필요한 값 추출.
    // 이후 추출한 값을 통해 Member 엔티티 생성.
    @Override
    public Member processUser(Map<String, Object> attributes) {
        String kakaoId = attributes.get("id").toString();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        /*
        Kakao API의 경우, 허용한 사용자 정보(nickname, profile_image, email 등)를 profile Map 으로 묶어서 전달.
        즉, 필요한 값을 추출하기 위해선 attributes 에서 profile을 가져오고, profile 에서 필요한 값을 추출해야 함.
         */

        String name = (String) profile.get("nickname");
        String profileImage = (String) profile.get("profile_image_url");

        return memberRepository.findByProviderAndProviderKey(Provider.KAKAO,kakaoId)
                .orElseGet(() -> memberService.createMember(name, profileImage, kakaoId, Provider.KAKAO));
    }
}