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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiV1MembershipControllerTest extends ControllerTestSupport {
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
        spaceService.createSpace("기존 스페이스 1_forMembershipControllerTest");
        spaceService.createSpace("기존 스페이스 2_forMembershipControllerTest");

    }

    void setUpMember() {
        memberService.createMember(
                "MembershipControllerTester1",
                "url",
                "mc1111",
                Provider.KAKAO
        );
        memberService.createMember(
                "MembershipControllerTester2",
                "url",
                "mc2222",
                Provider.KAKAO
        );
        memberService.createMember(
                "MembershipControllerTester3",
                "url",
                "mc3333",
                Provider.KAKAO
        );
    }

    void setUpMembership() {
        // test1 -> 스페이스 1 가입 (ADMIN)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc1111"),
                spaceService.findByName("기존 스페이스 1_forMembershipControllerTest"),
                Authority.ADMIN
        );

        // test2 -> 스페이스 1 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc2222"),
                spaceService.findByName("기존 스페이스 1_forMembershipControllerTest"),
                Authority.PENDING
        );

        // test3 -> 스페이스 1 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc3333"),
                spaceService.findByName("기존 스페이스 1_forMembershipControllerTest"),
                Authority.PENDING
        );

        // test2 -> 스페이스 2 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc2222"),
                spaceService.findByName("기존 스페이스 2_forMembershipControllerTest"),
                Authority.PENDING
        );
        // test1 -> 스페이스 2 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc1111"),
                spaceService.findByName("기존 스페이스 2_forMembershipControllerTest"),
                Authority.PENDING
        );
    }


    // ============================= LIST INVITED USERS ============================= //

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스에서 보낸 초대 목록 조회 - 성공")
    void listPendingInvites_Success() throws Exception {
        // given
        var member1 = memberService.findByKakaoKey("mc1111");
        var member2 = memberService.findByKakaoKey("mc2222");
        var member3 = memberService.findByKakaoKey("mc3333");
        var space = spaceService.findByName("기존 스페이스 1_forMembershipControllerTest");
        String url = "/api/v1/space/member/invite/%d".formatted(space.getId());

        // when
        ResultActions resultActions = performGet(url);

        // then
        expectOk(resultActions, "스페이스 초대 목록을 조회했습니다.");

        resultActions
                .andExpect(jsonPath("$.data.invitedUsers.length()").value(2))
                .andExpect(jsonPath("$.data.spaceId").value(space.getId()))
                .andExpect(jsonPath("$.data.invitedUsers[0].id").exists())
                .andExpect(jsonPath("$.data.invitedUsers[0].name").value(member2.getName()))
                .andExpect(jsonPath("$.data.invitedUsers[0].profileUrl").value(member2.getProfileImageUrl()))
                .andExpect(jsonPath("$.data.invitedUsers[1].id").exists())
                .andExpect(jsonPath("$.data.invitedUsers[1].name").value(member3.getName()))
                .andExpect(jsonPath("$.data.invitedUsers[1].profileUrl").value(member3.getProfileImageUrl()));
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스에서 보낸 초대 목록 조회 - 실패 : 스페이스 멤버가 아님")
    void listPendingInvites_Fail_NotSpaceMember() throws Exception {
        // given
        var space = spaceService.findByName("기존 스페이스 2_forMembershipControllerTest");
        String url = "/api/v1/space/member/invite/%d".formatted(space.getId());

        // when
        ResultActions resultActions = performGet(url);

        // then
        expectForbidden(resultActions, "액세스가 거부되었습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스에서 보낸 초대 목록 조회 - 실패 : 스페이스가 존재하지 않음")
    void listPendingInvites_Fail_NotExistSpace() throws Exception {
        // given
        Integer spaceId = 9999;
        String url = "/api/v1/space/member/invite/%d".formatted(spaceId);

        // when
        ResultActions resultActions = performGet(url);

        // then
        expectNotFound(resultActions, "존재하지 않는 스페이스입니다.");
    }

}