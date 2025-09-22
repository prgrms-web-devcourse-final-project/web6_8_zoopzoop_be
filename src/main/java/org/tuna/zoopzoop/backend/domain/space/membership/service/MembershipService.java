package org.tuna.zoopzoop.backend.domain.space.membership.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;

    /**
     * 스페이스에 멤버 추가
     * @param member 추가할 멤버
     * @param space 멤버가 추가될 스페이스
     * @param authority 멤버의 권한
     * @return 생성된 Membership 엔티티
     */
    public Membership addMemberToSpace(Member member, Space space, Authority authority) {
        Membership membership = new Membership();
        membership.setMember(member);
        membership.setSpace(space);
        membership.setAuthority(authority);
        return membershipRepository.save(membership);
    }
}
