package org.tuna.zoopzoop.backend.domain.member.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    //회원 조회 관련
    public Member findById(Integer id) {
        return memberRepository.findById(id).orElseThrow(() ->
                new NoResultException(id + " id를 가진 사용자를 찾을 수 없습니다.")
        );
    }
    public Member findByName(String name){
        return memberRepository.findByName(name).orElseThrow(() ->
                new NoResultException(name + " 이름을 가진 사용자를 찾을 수 없습니다.")
        );
    }
    public Member findByKakaoKey(Long kakaoKey){
        return memberRepository.findByKakaoKey(kakaoKey).orElseThrow(() ->
                new NoResultException(kakaoKey + " 카카오 키를 가진 사용자를 찾을 수 없습니다.")
        );
    }
//    public Member findByEmail(String email){
//        return memberRepository.findByEmail(email).orElseThrow(() ->
//                new NoResultException(email + " 이메일을 가진 사용자를 찾을 수 없습니다.")
//        );
//    }
    public List<Member> findAll(){ return memberRepository.findAll(); }
    public List<Member> findAllActive(){ return memberRepository.findByActiveTrue(); }
    public List<Member> findAllInactive(){ return memberRepository.findByActiveFalse(); }
    //빈 List를 전달하는 경우, 예외 처리를 할 지는 고민해봐야 할 사항.

    //회원 생성/정보 수정 관련
    @Transactional
    public Member createMember(String name, Long kakaoKey, String profileUrl){
//        if(memberRepository.findByEmail(email).isPresent()){
//            throw new DataIntegrityViolationException("이미 사용중인 이메일입니다.");
//        }
        if(memberRepository.findByName(name).isPresent()) {
            throw new DataIntegrityViolationException("이미 사용중인 이름입니다.");
        }

        Member member = Member.builder()
                .name(name)
                .kakaoKey(kakaoKey)
                .profileImageUrl(profileUrl)
                .build();
        return memberRepository.save(member);
    }

    @Transactional
    public void updateMemberName(Member member, String newName){
        if(memberRepository.findByName(newName).isPresent()) {
            throw new DataIntegrityViolationException("이미 사용중인 이름입니다.");
        }
        member.updateName(newName);
    }

    //회원 삭제/복구 관련
    public void softDeleteMember(Member member){ member.deactivate(); }
    public void hardDeleteMember(Member member){ memberRepository.delete(member); }
    public void restoreMember(Member member){ member.activate(); }
}
