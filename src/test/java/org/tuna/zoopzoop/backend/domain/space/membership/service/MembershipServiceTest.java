package org.tuna.zoopzoop.backend.domain.space.membership.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MembershipServiceTest {
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MembershipService membershipService;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        setUpMember();
        setUpSpace();
    }

    void setUpSpace() {
        spaceService.createSpace("기존 스페이스 1");
        spaceService.createSpace("기존 스페이스 2");

    }

    void setUpMember() {
        memberService.createMember(
                "테스트 유저1",
                4001L,
                "url");
        memberService.createMember(
                "테스트 유저2",
                4002L,
                "url");
        memberService.createMember(
                "테스트 유저3",
                4003L,
                "url");
    }

    // ============================= ADD MEMBER TO SPACE ============================= //

    @Test
    @WithUserDetails(value = "4001", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스에 멤버 추가 - 성공")
    void addMemberToSpace_Success() {
        // Given
        var member = memberService.findByKakaoKey(4002L);
        var space = spaceService.findByName("기존 스페이스 1");

        // When
        var membership = membershipService.addMemberToSpace(member, space, Authority.ADMIN);

        // Then
        assertNotNull(membership);
        assertNotNull(membership.getId());
        assertEquals(member.getId(), membership.getMember().getId());
        assertEquals(space.getId(), membership.getSpace().getId());
        assertEquals(org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority.ADMIN, membership.getAuthority());
    }

    @Test
    @WithUserDetails(value = "4001", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스에 멤버 추가 - 실패 : 이미 멤버로 존재")
    void addMemberToSpace_Fail_AlreadyMember() {
        // Given
        var member = memberService.findByKakaoKey(4002L);
        var space = spaceService.findByName("기존 스페이스 1");
        membershipService.addMemberToSpace(member, space, Authority.ADMIN);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipService.addMemberToSpace(member, space, Authority.READ_ONLY);
        });
    }

    @Test
    @WithUserDetails(value = "4001", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스에 멤버 추가 - 실패 : 스페이스가 존재하지 않음")
    void addMemberToSpace_Fail_SpaceNotFound() {
        // Given
        var member = memberService.findByKakaoKey(4002L);
        var space = spaceService.findByName("존재하지 않는 스페이스");

        // When & Then
        assertThrows(NoResultException.class, () -> {
            membershipService.addMemberToSpace(member, space, Authority.ADMIN);
        });
    }

    @Test
    @WithUserDetails(value = "4001", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스에 멤버 추가 - 실패 : 멤버가 존재하지 않음")
    void addMemberToSpace_Fail_MemberNotFound() {
        // Given
        var member = memberService.findByKakaoKey(9999L);
        var space = spaceService.findByName("기존 스페이스 1");

        // When & Then
        assertThrows(NoResultException.class, () -> {
            membershipService.addMemberToSpace(member, space, Authority.ADMIN);
        });
    }
}