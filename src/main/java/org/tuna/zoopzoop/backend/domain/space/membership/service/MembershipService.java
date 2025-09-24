package org.tuna.zoopzoop.backend.domain.space.membership.service;

import jakarta.persistence.NoResultException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.JoinState;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final MemberService memberService;

    // ======================== 멤버십 조회 ======================== //

    /**
     * 멤버십 ID로 Membership 조회
     * @param id 조회할 멤버십 ID
     * @return 해당 ID에 해당하는 Membership 엔티티
     * @throws NoResultException 해당 ID의 멤버십이 존재하지 않는 경우
     */
    public Membership findById(Integer id) {
        return membershipRepository.findById(id)
                .orElseThrow(() -> new NoResultException("해당 멤버십이 존재하지 않습니다."));
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
     * 스페이스에 속한 멤버 중 초대 상태(PENDING)인 멤버십 목록 조회
     * @param space 조회할 스페이스
     * @return 해당 스페이스에 속한 초대 상태(PENDING)인 멤버십 목록
     */
    public List<Membership> findInvitationsBySpace(Space space) {
        return membershipRepository.findAllBySpaceAndAuthority(space, Authority.PENDING);
    }

    /**
     * 스페이스에 속한 멤버 중 가입 상태(JOINED)인 멤버십 목록 조회
     * @param space 조회할 스페이스
     * @return 해당 스페이스에 속한 가입 상태(JOINED)인 멤버십 목록
     */
    public List<Membership> findMembersBySpace(Space space) {
        return membershipRepository.findAllBySpaceAndAuthorityIsNot(space, Authority.PENDING);
    }

    // ======================== 권한 조회 ======================== //
    /**
     * 멤버가 스페이스의 어드민 권한을 가지고 있는지 확인
     * @param member 조회할 멤버
     * @param space 조회할 스페이스
     * @throws NoResultException 해당 멤버가 권한이 없는 경우
     */
    public void checkAdminAuthority(Member member, Space space) throws AccessDeniedException {
        // 스페이스에 요청자가 속해있는지 확인
        if(!isMemberJoinedSpace(member, space)) {
            throw new AccessDeniedException("액세스가 거부되었습니다.");
        }

        // 요청자의 권한이 멤버 관리 권한이 있는지 확인
        Membership requesterMembership = findByMemberAndSpace(member, space);
        if(!requesterMembership.getAuthority().canManageMembers()) {
            throw new AccessDeniedException("액세스가 거부되었습니다.");
        }
    }





    // ======================== 멤버십 존재 여부 확인 ======================== //

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


    // ======================== 멤버십 생성 및 수정 ======================== //

    /**
     * 스페이스에 멤버 추가 (멤버십 생성)
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
     * 멤버의 권한 변경
     * @param membership 권한을 변경할 Membership 엔티티
     * @param newAuthority 새로운 권한
     * @return 변경된 Membership 엔티티
     */
    public Membership changeAuthority(Membership membership, Authority newAuthority) {

        membership.setAuthority(newAuthority);
        return membershipRepository.save(membership);
    }

    /**
     * 멤버의 권한 변경 처리 (요청자 검증 포함)
     * @param requester 권한 변경을 요청하는 멤버
     * @param space 변경이 이루어질 스페이스
     * @param targetMemberId 권한이 변경될 대상 멤버의 ID
     * @param newAuthority 새로운 권한
     * @return 변경된 Membership 엔티티
     * @throws AccessDeniedException 요청자가 멤버를 관리할 권한이 없는 경우
     */
    public Membership changeMemberAuthority(Member requester, Space space, Integer targetMemberId, Authority newAuthority) throws AccessDeniedException {
        // 1. 요청자가 멤버를 관리할 권한이 있는지 확인 (기존 로직 재사용)
        checkAdminAuthority(requester, space);

        // 2. 변경 대상 멤버 조회
        // ※ MemberService가 필요하므로 의존성 주입(DI)이 필요할 수 있습니다.
        Member targetMember = memberService.findById(targetMemberId);

        // 3. 자기 자신의 권한을 변경하는지 확인
        if (targetMember.equals(requester)) {
            throw new IllegalArgumentException("본인의 권한은 변경할 수 없습니다.");
        }

        // 4. PENDING 상태로 변경하려고 하는지 확인
        if (newAuthority == Authority.PENDING) {
            throw new IllegalArgumentException("멤버 권한을 PENDING(가입 대기)으로 변경할 수 없습니다.");
        }

        // 5. 대상 멤버의 현재 멤버십 정보 조회
        Membership targetMembership = findByMemberAndSpace(targetMember, space);

        // 6. 이미 같은 권한인지 확인
        if (targetMembership.getAuthority() == newAuthority) {
            // ※ 409 Conflict에 해당하는 예외를 사용하는 것이 좋습니다.
            throw new DataIntegrityViolationException("이미 요청된 권한을 가지고 있습니다.");
        }

        // 7. 모든 검증 통과 후, 권한 변경 로직 실행
        return changeAuthority(targetMembership, newAuthority);
    }


    // ======================== 멤버십 초대 처리 ======================== //

    /**
     * 멤버십 초대의 적절성 확인: 멤버십의 멤버가 일치하고, 권한이 PENDING이어야 함.
     * 일치하지 않으면 AccessDeniedException, 권한이 PENDING이 아니면 DataIntegrityViolationException 발생.
     */
    public void validateMembershipInvitation(Membership membership, Member member) throws AccessDeniedException {
        if (!membership.getMember().equals(member)) {
            throw new AccessDeniedException("액세스가 거부되었습니다.");
        }
        if (membership.getAuthority() != Authority.PENDING) {
            throw new DataIntegrityViolationException("이미 완료된 초대입니다.");
        }
    }

    /**
     * 초대 수락 처리: 멤버십의 권한을 READ_ONLY로 변경
     * @param membership
     */
    public void acceptInvitation(Membership membership) {
        if (membership.getAuthority() != Authority.PENDING) {
            throw new DataIntegrityViolationException("이미 완료된 초대입니다.");
        }
        changeAuthority(membership, Authority.READ_ONLY);
    }

    /**
     * 초대 거절 처리: 멤버십 엔티티 삭제
     * @param membership
     */
    public void rejectInvitation(Membership membership) {
        membershipRepository.delete(membership);
    }

}
