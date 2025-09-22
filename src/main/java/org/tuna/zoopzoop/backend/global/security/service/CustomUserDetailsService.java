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

    @Override
    public UserDetails loadUserByUsername(String kakaoKeyStr) throws UsernameNotFoundException {
        Long kakaoKey;
        try {
            kakaoKey = Long.parseLong(kakaoKeyStr);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("잘못된 카카오 키: " + kakaoKeyStr, e);
        }

        Member member = memberService.findByKakaoKey(kakaoKey);
        if (!member.isActive()) {
            throw new UsernameNotFoundException("비활성화된 계정입니다: " + kakaoKey);
        }
        return new CustomUserDetails(member);
    }
}