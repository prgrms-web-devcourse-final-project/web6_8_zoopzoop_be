package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiV1InviteControllerTest {
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MembershipService membershipService;

    @BeforeAll
    void setUp() {
        setUpMember();
        setUpSpace();
        setUpMembership();
    }

    void setUpSpace() {
        spaceService.createSpace("기존 스페이스 1_forInviteControllerTest");
        spaceService.createSpace("기존 스페이스 2_forInviteControllerTest");

    }

    void setUpMember() {
        memberService.createMember(
                "InviteControllerTester1",
                "url",
                "ic1111",
                Provider.KAKAO
        );
        memberService.createMember(
                "InviteControllerTester2",
                "url",
                "ic2222",
                Provider.KAKAO
        );
        memberService.createMember(
                "InviteControllerTester3",
                "url",
                "ic3333",
                Provider.KAKAO
        );
    }

    void setUpMembership() {
        // test1 -> 스페이스 1 가입 (ADMIN)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("ic1111"),
                spaceService.findByName("기존 스페이스 1_forInviteControllerTest"),
                Authority.ADMIN
        );
        // test2 -> 스페이스 2 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("ic2222"),
                spaceService.findByName("기존 스페이스 1_forInviteControllerTest"),
                Authority.PENDING
        );
        // test1 -> 스페이스 2 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("ic1111"),
                spaceService.findByName("기존 스페이스 2_forInviteControllerTest"),
                Authority.PENDING
        );
    }

}