package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiV1InviteControllerTest extends ControllerTestSupport {
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

    // ============================= ACCEPT ============================= //

    @Test
    @WithUserDetails(value = "KAKAO:ic1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 수락 - 성공")
    void acceptInvite_Success() throws Exception {
        // given
        var member = memberService.findByKakaoKey("ic1111");
        var space = spaceService.findByName("기존 스페이스 2_forInviteControllerTest");
        Integer inviteId = membershipService.findByMemberAndSpace(member, space).getId();

        String url = "/api/v1/invite/%d/accept".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url, null);

        // then
        expectOk(resultActions, "스페이스 초대를 수락했습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:ic1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 수락 - 실패 : 존재하지 않는 초대")
    void acceptInvite_Fail_NotExistInvite() throws Exception {
        // given
        Integer inviteId = 9999;
        String url = "/api/v1/invite/%d/accept".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url, null);

        // then
        expectNotFound(resultActions, "존재하지 않는 초대 입니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:ic2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 수락 - 실패 : 본인의 초대가 아님")
    void acceptInvite_Fail_NotYourInvite() throws Exception {
        // given
        var member = memberService.findByKakaoKey("ic1111");
        var space = spaceService.findByName("기존 스페이스 2_forInviteControllerTest");
        Integer inviteId = membershipService.findByMemberAndSpace(member, space).getId();
        String url = "/api/v1/invite/%d/accept".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url, null);

        // then
        expectForbidden(resultActions, "액세스가 거부되었습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:ic1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 수락 - 실패 : 초대 상태가 아님")
    void acceptInvite_Fail_NotPendingStatus() throws Exception {
        // given
        var member = memberService.findByKakaoKey("ic1111");
        var space = spaceService.findByName("기존 스페이스 1_forInviteControllerTest");
        Integer inviteId = membershipService.findByMemberAndSpace(member, space).getId();
        String url = "/api/v1/invite/%d/accept".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url, null);

        // then
        expectBadRequest(resultActions, "이미 완료된 초대입니다.");
    }
}