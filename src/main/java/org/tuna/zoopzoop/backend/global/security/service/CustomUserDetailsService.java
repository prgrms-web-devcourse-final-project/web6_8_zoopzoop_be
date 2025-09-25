package org.tuna.zoopzoop.backend.global.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberService memberService;

    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        // subject = "provider:providerKey"
        String[] parts = subject.split(":");
        if (parts.length != 2) {
            throw new UsernameNotFoundException("잘못된 토큰 subject: " + subject);
        }

        String provider = parts[0]; // kakao, google
        String providerKey = parts[1];

        Member member;
        if ("KAKAO".equals(provider)) {
            member = memberService.findByKakaoKey(providerKey);
        } else if ("GOOGLE".equals(provider)) {
            member = memberService.findByGoogleKey(providerKey);
        } else {
            throw new UsernameNotFoundException("지원하지 않는 provider: " + provider);
        }

        if (!member.isActive()) {
            throw new UsernameNotFoundException("비활성화된 계정입니다: " + providerKey);
        }

        return new CustomUserDetails(member);
    }
}