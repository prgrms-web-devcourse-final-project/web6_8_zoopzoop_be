package org.tuna.zoopzoop.backend.domain.member.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public Member findByName(String name){
        return memberRepository.findByName(name).orElseThrow(() ->
                new NoResultException(name + " 이름을 가진 사용자를 찾을 수 없습니다.")
        );
    }

    public Member findByEmail(String email){
        return memberRepository.findByEmail(email).orElseThrow(() ->
                new NoResultException(email + " 이메일을 가진 사용자를 찾을 수 없습니다.")
        );
    }

    public Member createMember(String name, String email, String profileUrl){
        if(memberRepository.findByEmail(email).isPresent()){

        }
        if(memberRepository.findByName(name).isPresent()) {

        }

        Member member = Member.builder()
                .name(name)
                .email(email)
                .profileImageUrl(profileUrl)
                .build();
        return memberRepository.save(member);
    }
}
