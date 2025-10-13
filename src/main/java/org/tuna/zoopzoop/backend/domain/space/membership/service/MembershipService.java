package org.tuna.zoopzoop.backend.domain.space.membership.service;

import jakarta.persistence.NoResultException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.SSE.service.EmitterService;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.etc.SpaceMemberInfo;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.JoinState;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.space.dto.etc.SpaceInvitationInfo;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final MemberService memberService;
    private final NotificationService notificationService;

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
    public Page<Membership> findByMember(Member member, String state, Pageable pageable) {
        if (state.equalsIgnoreCase("PENDING")) {
            return membershipRepository.findAllByMemberAndAuthorityWithSpace(member, Authority.PENDING, pageable);
        } else if (state.equalsIgnoreCase("JOINED")) {
            return membershipRepository.findAllByMemberAndAuthorityIsNotWithSpace(member, Authority.PENDING, pageable);
        } else {
            return membershipRepository.findAllByMemberWithSpace(member, pageable);
        }
    }

    /**
     * 멤버가 속한 스페이스 목록 조회
     * @param member 조회할 멤버
     * @param state 멤버의 가입 상태로 필터링 (PENDING, JOINED, ALL)
     * @return 멤버가 속한 스페이스 목록
     */
    public List<Membership> findByMember(Member member, String state) {
        if (state.equalsIgnoreCase("PENDING")) {
            return membershipRepository.findAllByMemberAndAuthorityOrderById(member, Authority.PENDING);
        } else if (state.equalsIgnoreCase("JOINED")) {
            return membershipRepository.findAllByMemberAndAuthorityIsNotOrderById(member, Authority.PENDING);
        } else {
            List<Membership> memberships = membershipRepository.findAllByMemberOrderById(member);
            return memberships;
        }
    }

    /**
     * 스페이스에 속한 멤버 중 초대 상태(PENDING)인 멤버십 목록 조회
     * @param space 조회할 스페이스
     * @return 해당 스페이스에 속한 초대 상태(PENDING)인 멤버십 목록
     */
    public List<Membership> findInvitationsBySpace(Space space) {
        return membershipRepository.findAllBySpaceAndAuthorityOrderById(space, Authority.PENDING);
    }

    /**
     * 스페이스에 속한 멤버 중 가입 상태(JOINED)인 멤버십 목록 조회
     * @param space 조회할 스페이스
     * @return 해당 스페이스에 속한 가입 상태(JOINED)인 멤버십 목록
     */
    public List<Membership> findMembersBySpace(Space space) {
        return membershipRepository.findAllBySpaceAndAuthorityIsNotOrderById(space, Authority.PENDING);
    }

    /**
     * 여러 스페이스에 속한 멤버 목록을 한 번의 쿼리로 조회 (N+1 문제 해결용)
     * @param spaces 조회할 스페이스 목록
     * @return 스페이스 ID를 key로, 해당 스페이스의 멤버 정보 리스트를 value로 갖는 Map
     */
    @Transactional(readOnly = true)
    public Map<Integer, List<SpaceMemberInfo>> findMembersBySpaces(List<Space> spaces) {
        if (spaces == null || spaces.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 한 번의 쿼리로 모든 스페이스의 멤버십 정보를 가져옴
        List<Membership> allMemberships = membershipRepository.findAllMembersInSpaces(spaces);

        // 2. Space ID 별로 그룹핑하여 Map으로 변환
        return allMemberships.stream()
                .collect(Collectors.groupingBy(
                        membership -> membership.getSpace().getId(), // Key: Space ID
                        Collectors.mapping( // Value: List<SpaceMemberInfo> DTO로 변환
                                membership -> new SpaceMemberInfo(
                                        membership.getMember().getId(),
                                        membership.getMember().getName(),
                                        membership.getMember().getProfileImageUrl(),
                                        membership.getAuthority()
                                ),
                                Collectors.toList()
                        )
                ));
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


    /**
     * requester가 스페이스의 유일한 ADMIN인지 확인
     * @param requester 요청자 멤버
     * @param space 확인할 스페이스
     */
    public boolean checkIsOnlyAdmin(Member requester, Space space) {
        // 1. 요청자가 ADMIN 권한을 가지고 있는지 확인
        if (!isMemberAdminInSpace(requester, space)) {
            return false;
        }

        // 2. 스페이스의 ADMIN 멤버 수 조회
        long adminCount = membershipRepository.countBySpaceAndAuthority(space, Authority.ADMIN);

        // 3. ADMIN 멤버가 1명인지 확인
        return adminCount == 1;
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
     * 스페이스에 멤버 초대 (여러 명)
     * @param space 멤버가 추가될 스페이스
     * @param invitedName 초대할 멤버 이름 목록
     * @return 생성된 Membership 엔티티 목록
     */
    @Transactional
    public List<Membership> inviteMembersToSpace(Space space, List<String> invitedName) {
        // 1. 이름 중복 제거
        List<String> uniqueNames = invitedName.stream().distinct().toList();

        // 2. 존재하는 멤버만 필터링
        List<Member> members = uniqueNames.stream()
                .map(memberService::findOptionalByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        // 3. 이미 초대된 멤버는 제외
        List<Membership> invitedMemberships = members.stream()
                .filter(member -> !membershipRepository.existsByMemberAndSpace(member, space))
                .map(member -> {
                    Membership membership = new Membership();
                    membership.setMember(member);
                    membership.setSpace(space);
                    membership.setAuthority(Authority.PENDING);
                    return membership;
                })
                .toList();

        // 4. 멤버십 저장
        List<Membership> savedMemberships = membershipRepository.saveAll(invitedMemberships);

        // 5. 알림 전송 호출
        savedMemberships.forEach(membership -> {
            notificationService.sendSpaceInvitation(
                    (long) membership.getMember().getId(),
                        new SpaceInvitationInfo(
                                space.getId(),
                                space.getName(),
                                space.getThumbnailUrl(),
                                membership.getId()
                    )
            );
        });

        // 6. 저장된 멤버십 반환
        return savedMemberships;
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

    // ======================== 멤버십 삭제 ======================== //
    /**
     * 멤버를 스페이스에서 퇴출 (멤버십 삭제)
     * @param member 탈퇴할 멤버
     * @param space 탈퇴할 스페이스
     */
    public void expelMemberFromSpace(Member member, Space space) {
        Membership membership = findByMemberAndSpace(member, space);
        membershipRepository.delete(membership);
    }

    public void leaveSpace(Member member, Space space) {
        // 유일한 어드민은 탈퇴할 수 없음
        if(checkIsOnlyAdmin(member, space)) {
            throw new IllegalArgumentException("유일한 어드민은 탈퇴할 수 없습니다.");
        }

        // 초대 상태면 탈퇴할 수 없음 -> 초대 거절 로직 사용
        if(!isMemberJoinedSpace(member, space)) {
            throw new NoResultException("해당 멤버는 스페이스에 속해있지 않습니다.");
        }

        Membership membership = findByMemberAndSpace(member, space);
        membershipRepository.delete(membership);
    }


}
