package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.global.clients.liveblocks.LiveblocksClient;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @MockitoBean
    private LiveblocksClient liveblocksClient;

    @BeforeAll
    void setUp() {
        Mockito.doNothing().when(liveblocksClient).createRoom(anyString());
        Mockito.doNothing().when(liveblocksClient).deleteRoom(anyString());

        setUpMember();
        setUpSpace();
        setUpMembership();
    }

    void setUpSpace() {
        Space space1 = spaceService.createSpace("기존 스페이스 1_forInviteControllerTest", "dummyUrl1");
        Space space2 = spaceService.createSpace("기존 스페이스 2_forInviteControllerTest", "dummyUrl2");
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

        // test2 -> 스페이스 1 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("ic2222"),
                spaceService.findByName("기존 스페이스 1_forInviteControllerTest"),
                Authority.PENDING
        );

        // test3 -> 스페이스 1 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("ic3333"),
                spaceService.findByName("기존 스페이스 1_forInviteControllerTest"),
                Authority.PENDING
        );

        // test2 -> 스페이스 2 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("ic2222"),
                spaceService.findByName("기존 스페이스 2_forInviteControllerTest"),
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
        ResultActions resultActions = performPost(url);

        // then
        expectOk(resultActions, "스페이스 초대가 수락됐습니다.");

        resultActions
                .andExpect(jsonPath("$.data.id").value(space.getId()))
                .andExpect(jsonPath("$.data.name").value(space.getName()));
    }

    @Test
    @WithUserDetails(value = "KAKAO:ic1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 수락 - 실패 : 존재하지 않는 초대")
    void acceptInvite_Fail_NotExistInvite() throws Exception {
        // given
        Integer inviteId = 9999;
        String url = "/api/v1/invite/%d/accept".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url);

        // then
        expectNotFound(resultActions, "해당 멤버십이 존재하지 않습니다.");
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
        ResultActions resultActions = performPost(url);

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
        ResultActions resultActions = performPost(url);

        // then
        resultActions.
                andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.msg").value("이미 완료된 초대입니다."));
    }

    // ============================= REJECT ============================= //

    @Test
    @WithUserDetails(value = "KAKAO:ic1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 거절 - 성공")
    void rejectInvite_Success() throws Exception {
        // given
        var member = memberService.findByKakaoKey("ic1111");
        var space = spaceService.findByName("기존 스페이스 2_forInviteControllerTest");
        Integer inviteId = membershipService.findByMemberAndSpace(member, space).getId();

        String url = "/api/v1/invite/%d/reject".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url);

        // then
        expectOk(resultActions, "스페이스 초대가 거절됐습니다.");

        resultActions
                .andExpect(jsonPath("$.data.id").value(space.getId()))
                .andExpect(jsonPath("$.data.name").value(space.getName()));
    }

    @Test
    @WithUserDetails(value = "KAKAO:ic1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 거절 - 실패 : 존재하지 않는 초대")
    void rejectInvite_Fail_NotExistInvite() throws Exception {
        // given
        Integer inviteId = 9999;
        String url = "/api/v1/invite/%d/reject".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url);

        // then
        expectNotFound(resultActions, "해당 멤버십이 존재하지 않습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:ic2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 거절 - 실패 : 본인의 초대가 아님")
    void rejectInvite_Fail_NotYourInvite() throws Exception {
        // given
        var member = memberService.findByKakaoKey("ic1111");
        var space = spaceService.findByName("기존 스페이스 2_forInviteControllerTest");
        Integer inviteId = membershipService.findByMemberAndSpace(member, space).getId();
        String url = "/api/v1/invite/%d/reject".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url);

        // then
        expectForbidden(resultActions, "액세스가 거부되었습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:ic1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대 거절 - 실패 : 초대 상태가 아님")
    void rejectInvite_Fail_NotPendingStatus() throws Exception {
        // given
        var member = memberService.findByKakaoKey("ic1111");
        var space = spaceService.findByName("기존 스페이스 1_forInviteControllerTest");
        Integer inviteId = membershipService.findByMemberAndSpace(member, space).getId();
        String url = "/api/v1/invite/%d/reject".formatted(inviteId);

        // when
        ResultActions resultActions = performPost(url);

        // then
        resultActions.
                andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.msg").value("이미 완료된 초대입니다."));
    }

    // ============================= GET MY INVITES ============================= //
    @Test
    @WithUserDetails(value = "KAKAO:ic2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("나에게 온 초대 목록 조회 - 성공")
    void getMyInvites_Success() throws Exception {
        // given
        String url = "/api/v1/invite";

        Space space1 = spaceService.findByName("기존 스페이스 1_forInviteControllerTest");
        Space space2 = spaceService.findByName("기존 스페이스 2_forInviteControllerTest");

        // when
        ResultActions resultActions = performGet(url);

        // then
        expectOk(resultActions, "사용자에게 온 스페이스 초대 목록을 조회했습니다.");

        resultActions
                .andExpect(jsonPath("$.data.spaces").isArray())
                .andExpect(jsonPath("$.data.spaces.length()").value(2))
                .andExpect(jsonPath("$.data.spaces[0].id").value(space1.getId()))
                .andExpect(jsonPath("$.data.spaces[0].name").value(space1.getName()))
                .andExpect(jsonPath("$.data.spaces[0].thumbnailUrl").value(space1.getThumbnailUrl()))
                .andExpect(jsonPath("$.data.spaces[1].id").value(space2.getId()))
                .andExpect(jsonPath("$.data.spaces[1].name").value(space2.getName()))
                .andExpect(jsonPath("$.data.spaces[1].thumbnailUrl").value(space2.getThumbnailUrl()));
    }


}