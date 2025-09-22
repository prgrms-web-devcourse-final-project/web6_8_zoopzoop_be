package org.tuna.zoopzoop.backend.global.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private final Member member;
    public CustomUserDetails(Member member) { this.member = member; }

    public Member getMember() { return member; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return null; }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return String.valueOf(member.getKakaoKey()); }

    public String getNickname() { return member.getName(); }
    public String getProfileImageUrl() { return member.getProfileImageUrl(); }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 여부
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.isActive(); // 계정 활성화 여부
    }
}
