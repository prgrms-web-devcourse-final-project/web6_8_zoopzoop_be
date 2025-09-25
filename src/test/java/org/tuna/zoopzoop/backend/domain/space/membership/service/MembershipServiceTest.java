package org.tuna.zoopzoop.backend.domain.space.membership.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;

import java.nio.file.AccessDeniedException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MembershipServiceTest {
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MembershipService membershipService;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private MemberRepository memberRepository;

    @BeforeAll
    void setUp() {
        membershipRepository.deleteAll();
        setUpMember();
        setUpSpace();
        setUpMembership();
    }

    void setUpSpace() {
        spaceService.createSpace("기존 스페이스 1_forMembershipServiceTest");
        spaceService.createSpace("기존 스페이스 2_forMembershipServiceTest");
        spaceService.createSpace("기존 스페이스 3_forMembershipServiceTest");
    }

    void setUpMember() {
        memberService.createMember(
                "tester1_forMembershipServiceTest",
                "url",
                "ms1111",
                Provider.KAKAO
        );
        memberService.createMember(
                "tester2_forMembershipServiceTest",
                "url",
                "ms2222",
                Provider.KAKAO
        );
        memberService.createMember(
                "tester3_forMembershipServiceTest",
                "url",
                "ms3333",
                Provider.KAKAO
        );
    }

    void setUpMembership() {
        var member1 = memberService.findByKakaoKey("ms1111");
        var member2 = memberService.findByKakaoKey("ms2222");
        var space1 = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");
        var space2 = spaceService.findByName("기존 스페이스 2_forMembershipServiceTest");
        var space3 = spaceService.findByName("기존 스페이스 3_forMembershipServiceTest");

        // member1 -> space1 (ADMIN)
        membershipService.addMemberToSpace(member1, space1, Authority.ADMIN);

        // member2 -> space1 (READ_ONLY)
        membershipService.addMemberToSpace(member2, space1, Authority.READ_ONLY);

        // member1 -> space2 (PENDING)
        membershipService.addMemberToSpace(member1, space2, Authority.PENDING);

        // member2 -> space2 (ADMIN)
        membershipService.addMemberToSpace(member2, space2, Authority.ADMIN);

        // member1  -> space3 (ADMIN)
        membershipService.addMemberToSpace(member1, space3, Authority.ADMIN);
    }

    // ============================= ADD MEMBER TO SPACE ============================= //

    @Test
    @DisplayName("스페이스에 멤버 추가 - 성공")
    void addMemberToSpace_Success() {
        // Given
        var member = memberService.findByKakaoKey("ms3333");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");

        // When
        var membership = membershipService.addMemberToSpace(member, space, Authority.ADMIN);

        // Then
        assertNotNull(membership);
        assertNotNull(membership.getId());
        assertEquals(member.getId(), membership.getMember().getId());
        assertEquals(space.getId(), membership.getSpace().getId());
        assertEquals(Authority.ADMIN, membership.getAuthority());
    }

    @Test
    @DisplayName("스페이스에 멤버 추가 - 실패 : 이미 멤버로 존재")
    void addMemberToSpace_Fail_AlreadyMember() {
        // Given
        var member = memberService.findByKakaoKey("ms3333");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");
        membershipService.addMemberToSpace(member, space, Authority.ADMIN);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipService.addMemberToSpace(member, space, Authority.READ_ONLY);
        });
    }

    // ============================= ACCEPT INVITE ============================= //
    @Test
    @DisplayName("초대 수락 - 성공")
    void acceptInvitation_Success() {
        // Given
        var member = memberService.findByKakaoKey("ms1111");
        var space = spaceService.findByName("기존 스페이스 2_forMembershipServiceTest");
        var membership = membershipService.findByMemberAndSpace(member, space);

        // When
        membershipService.acceptInvitation(membership);
        var updatedMembership = membershipService.findById(membership.getId());

        // Then
        assertEquals(Authority.READ_ONLY, updatedMembership.getAuthority());
    }

    @Test
    @DisplayName("초대 수락 - 실패 : PENDING 상태가 아님")
    void acceptInvitation_Fail_NotPending() {
        // Given
        var member = memberService.findByKakaoKey("ms1111");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");
        var membership = membershipService.findByMemberAndSpace(member, space);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipService.acceptInvitation(membership);
        });
    }

    // ============================= REJECT INVITE ============================= //

    @Test
    @DisplayName("초대 거절 - 성공")
    void rejectInvitation_Success() {
        // Given
        var member = memberService.findByKakaoKey("ms1111");
        var space = spaceService.findByName("기존 스페이스 2_forMembershipServiceTest");
        var membership = membershipService.findByMemberAndSpace(member, space);

        // When
        membershipService.rejectInvitation(membership);

        // Then
        assertThrows(NoResultException.class, () -> {
            membershipService.findById(membership.getId());
        });
    }

    // ============================= CHECK INVITATION VALIDATION ============================= //

    @Test
    @DisplayName("초대 검증 - 성공")
    void validateMembershipInvitation_Success() {
        // Given
        var member = memberService.findByKakaoKey("ms1111");
        var space = spaceService.findByName("기존 스페이스 2_forMembershipServiceTest");
        var membership = membershipService.findByMemberAndSpace(member, space);

        // When & Then
        assertDoesNotThrow(() -> {
            membershipService.validateMembershipInvitation(membership, member);
        });
    }

    @Test
    @DisplayName("초대 검증 - 실패 : 멤버 불일치")
    void validateMembershipInvitation_Fail_MemberMismatch() {
        // Given
        var member1 = memberService.findByKakaoKey("ms1111");
        var member2 = memberService.findByKakaoKey("ms2222");
        var space = spaceService.findByName("기존 스페이스 2_forMembershipServiceTest");
        var membership = membershipService.findByMemberAndSpace(member1, space);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            membershipService.validateMembershipInvitation(membership, member2);
        });
    }

    @Test
    @DisplayName("초대 검증 - 실패 : PENDING 상태가 아님")
    void validateMembershipInvitation_Fail_NotPending() {
        // Given
        var member = memberService.findByKakaoKey("ms1111");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");
        var membership = membershipService.findByMemberAndSpace(member, space);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipService.validateMembershipInvitation(membership, member);
        });
    }

    // ============================= CHANGE AUTHORITY ============================= //

    @Test
    @DisplayName("권한 변경 - 성공")
    void changeAuthority_Success() {
        // Given
        var member = memberService.findByKakaoKey("ms2222");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");
        var membership = membershipService.findByMemberAndSpace(member, space);

        // When
        var updatedMembership = membershipService.changeAuthority(membership, Authority.ADMIN);

        // Then
        assertEquals(Authority.ADMIN, updatedMembership.getAuthority());
    }

    // ============================= INVITE MEMBERS ============================= //

    @Test
    @DisplayName("멤버 단건 초대 - 성공")
    void inviteMemberToSpace_Success() {
        // Given
        Member member3 = memberService.findByKakaoKey("ms3333");
        List<String> targetMembers = List.of(member3.getName());
        var space = spaceService.findByName("기존 스페이스 3_forMembershipServiceTest");

        // When
        List<Membership> results = membershipService.inviteMembersToSpace(space, targetMembers);

        // Then
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getMember().getName()).isEqualTo(member3.getName());
        assertThat(results.get(0).getAuthority()).isEqualTo(Authority.PENDING);
        assertThat(results.get(0).getSpace().getId()).isEqualTo(space.getId());

        Membership membership3 = membershipRepository.findByMemberAndSpace(member3, space).orElseThrow();

        assertThat(membership3.getAuthority()).isEqualTo(Authority.PENDING);
    }

    @Test
    @DisplayName("멤버 다건 초대 - 성공")
    void inviteMultipleMembersToSpace_Success() {
        // Given
        Member member2 = memberService.findByKakaoKey("ms2222");
        Member member3 = memberService.findByKakaoKey("ms3333");

        List<String> targetMembers = List.of(member2.getName(), member3.getName());
        var space = spaceService.findByName("기존 스페이스 3_forMembershipServiceTest");

        // When
        List<Membership> results = membershipService.inviteMembersToSpace(space, targetMembers);

        // Then
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0).getAuthority()).isEqualTo(Authority.PENDING);
        assertThat(results.get(1).getAuthority()).isEqualTo(Authority.PENDING);
        assertThat(results.get(0).getSpace().getId()).isEqualTo(space.getId());
        assertThat(results.get(1).getSpace().getId()).isEqualTo(space.getId());
        assertThat(results.get(0).getMember().getId()).isIn(member2.getId(), member3.getId());
        assertThat(results.get(1).getMember().getId()).isIn(member2.getId(), member3.getId());

        Membership membership2 = membershipRepository.findByMemberAndSpace(member2, space).orElseThrow();
        Membership membership3 = membershipRepository.findByMemberAndSpace(member3, space).orElseThrow();

        assertThat(membership2.getAuthority()).isEqualTo(Authority.PENDING);
        assertThat(membership3.getAuthority()).isEqualTo(Authority.PENDING);
    }

    @Test
    @DisplayName("멤버 초대 - 성공 : 일부만 초대 (이미 멤버인 경우 제외)")
    void inviteMembersToSpace_PartialSuccess_AlreadyMember() {
        // Given
        Member member1 = memberService.findByKakaoKey("ms1111");
        Member member3 = memberService.findByKakaoKey("ms3333");

        List<String> targetMembers = List.of(member1.getName(), member3.getName());
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");

        // When
        List<Membership> results = membershipService.inviteMembersToSpace(space, targetMembers);

        // Then
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getMember().getId()).isEqualTo(member3.getId());
        assertThat(results.get(0).getAuthority()).isEqualTo(Authority.PENDING);
        assertThat(results.get(0).getSpace().getId()).isEqualTo(space.getId());

        Membership membership3 = membershipRepository.findByMemberAndSpace(member3, space).orElseThrow();
        assertThat(membership3.getAuthority()).isEqualTo(Authority.PENDING);
    }

    @Test
    @DisplayName("멤버 초대 - 성공 : 일부만 초대 (없는 아이디 제외)")
    void inviteMembersToSpace_PartialSuccess_NonExistentMember() {
        // Given
        Member member2 = memberService.findByKakaoKey("ms2222");
        String nonExistentName = "nonExistentMember_forMembershipServiceTest";

        List<String> targetMembers = List.of(member2.getName(), nonExistentName);
        var space = spaceService.findByName("기존 스페이스 3_forMembershipServiceTest");

        // When
        List<Membership> results = membershipService.inviteMembersToSpace(space, targetMembers);

        // Then
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getMember().getId()).isEqualTo(member2.getId());
        assertThat(results.get(0).getAuthority()).isEqualTo(Authority.PENDING);
        assertThat(results.get(0).getSpace().getId()).isEqualTo(space.getId());

        Membership membership2 = membershipRepository.findByMemberAndSpace(member2, space).orElseThrow();
        assertThat(membership2.getAuthority()).isEqualTo(Authority.PENDING);
    }


    // ============================ EXPEL MEMBERS ============================= //

    @Test
    @DisplayName("멤버 퇴출 - 성공")
    void expelMemberFromSpace_Success() throws AccessDeniedException {
        // Given
        Member member2 = memberService.findByKakaoKey("ms2222");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");
        Membership membershipToExpel = membershipService.findByMemberAndSpace(member2, space);

        // When
        membershipService.expelMemberFromSpace(member2, space);

        // Then
        assertThrows(NoResultException.class, () -> {
            membershipService.findById(membershipToExpel.getId());
        });
    }
}