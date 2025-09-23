package org.tuna.zoopzoop.backend.domain.space.membership.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.JoinState;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;

    /**
     * 멤버가 스페이스에 가입되어 있는지 여부 확인 (PENDING 상태 포함)
     * @param member 확인할 멤버
     * @param space 확인할 스페이스
     * @return
     */
    public boolean isMemberInSpace(Member member, Space space) {
        return membershipRepository.existsByMemberAndSpace(member, space);
    }

    /**
     * 멤버가 스페이스에 가입되어 있는지 여부 확인 (PENDING 상태 제외)
     * @param member 확인할 멤버
     * @param space 확인할 스페이스
     * @return
     */
    public boolean isMemberJoinedSpace(Member member, Space space) {
        return membershipRepository.existsByMemberAndSpaceAndAuthorityIsNot(member, space, Authority.PENDING);
    }

    /**
     * 멤버가 스페이스의 ADMIN 권한을 가지고 있는지 여부 확인
     * @param member 확인할 멤버
     * @param space 확인할 스페이스
     * @return
     */
    public boolean isMemberAdminInSpace(Member member, Space space) {
        return membershipRepository.existsByMemberAndSpaceAndAuthority(member, space, Authority.ADMIN);
    }

    /**
     * 스페이스에 멤버 추가
     * @param member 추가할 멤버
     * @param space 멤버가 추가될 스페이스
     * @param authority 멤버의 권한
     * @return 생성된 Membership 엔티티
     */
    public Membership addMemberToSpace(Member member, Space space, Authority authority) {
        // 이미 해당 멤버가 스페이스에 속해있는지 확인
        if (membershipRepository.existsByMemberAndSpace(member, space)) {
            throw new DataIntegrityViolationException("이미 스페이스에 속한 멤버입니다.");
        }


        Membership membership = new Membership();
        membership.setMember(member);
        membership.setSpace(space);
        membership.setAuthority(authority);
        return membershipRepository.save(membership);
    }

    /**
     * 멤버가 속한 스페이스 목록 조회
     * @param member 조회할 멤버
     * @param state 멤버의 가입 상태로 필터링 (PENDING, JOINED, ALL)
     * @return 멤버가 속한 스페이스 목록
     */
    public List<Membership> findByMember(Member member, String state) {
        if (state.equalsIgnoreCase("PENDING")) {
            return membershipRepository.findAllByMemberAndAuthority(member, Authority.PENDING);
        } else if (state.equalsIgnoreCase("JOINED")) {
            return membershipRepository.findAllByMemberAndAuthorityIsNot(member, Authority.PENDING);
        } else {
            return membershipRepository.findAllByMember(member);
        }
    }

    /**
     * 멤버와 스페이스로 Membership 조회
     * @param member 조회할 멤버
     * @param space 조회할 스페이스
     * @return 해당 멤버와 스페이스에 해당하는 Membership 엔티티
     * @throws NoResultException 해당 멤버가 스페이스에 속해있지 않은 경우
     */
    public Membership findByMemberAndSpace(Member member, Space space) {
        return membershipRepository.findByMemberAndSpace(member, space)
                .orElseThrow(() -> new NoResultException("해당 멤버는 스페이스에 속해있지 않습니다."));
    }
}
