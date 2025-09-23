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
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
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
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        setUpMember();
        setUpSpace();

        memberRepository.findAll().forEach(member -> System.out.println("Member: " + member.getName()));
        System.out.println("----- setUp 완료 -----");
    }

    void setUpSpace() {
        spaceService.createSpace("기존 스페이스 1_forMembershipServiceTest");
        spaceService.createSpace("기존 스페이스 2_forMembershipServiceTest");
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

    // ============================= ADD MEMBER TO SPACE ============================= //

    @Test
    @WithMockUser
    @DisplayName("스페이스에 멤버 추가 - 성공")
    void addMemberToSpace_Success() {
        // Given
        var member = memberService.findByKakaoKey("ms2222");
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
    @WithMockUser
    @DisplayName("스페이스에 멤버 추가 - 실패 : 이미 멤버로 존재")
    void addMemberToSpace_Fail_AlreadyMember() {
        // Given
        var member = memberService.findByKakaoKey("ms2222");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipServiceTest");
        membershipService.addMemberToSpace(member, space, Authority.ADMIN);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipService.addMemberToSpace(member, space, Authority.READ_ONLY);
        });
    }

    // ============================= MEMBER INVITE ============================= //

}