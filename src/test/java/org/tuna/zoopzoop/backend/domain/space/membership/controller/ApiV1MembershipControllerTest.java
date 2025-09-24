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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        spaceService.createSpace("기존 스페이스 3_forMembershipControllerTest");
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

        // test1 -> 스페이스 3 가입 (ADMIN)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc1111"),
                spaceService.findByName("기존 스페이스 3_forMembershipControllerTest"),
                Authority.ADMIN
        );

        // test2 -> 스페이스 3 가입 (READ_WRITE)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc2222"),
                spaceService.findByName("기존 스페이스 3_forMembershipControllerTest"),
                Authority.READ_WRITE
        );

        // test3 -> 스페이스 3 가입 (READ_ONLY)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("mc3333"),
                spaceService.findByName("기존 스페이스 3_forMembershipControllerTest"),
                Authority.READ_ONLY
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

    // ============================= LIST Space Members ============================= //

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 목록 조회 - 성공")
    void listSpaceMembers_Success() throws Exception {
        // given
        var member1 = memberService.findByKakaoKey("mc1111");
        var member2 = memberService.findByKakaoKey("mc2222");
        var member3 = memberService.findByKakaoKey("mc3333");
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());

        // when
        ResultActions resultActions = performGet(url);

        // then
        expectOk(resultActions, "스페이스 멤버 목록을 조회했습니다.");

        resultActions
                .andExpect(jsonPath("$.data.members.length()").value(3))
                .andExpect(jsonPath("$.data.spaceId").value(space.getId()))
                .andExpect(jsonPath("$.data.spaceName").value(space.getName()))
                .andExpect(jsonPath("$.data.members[0].id").exists())
                .andExpect(jsonPath("$.data.members[0].name").value(member1.getName()))
                .andExpect(jsonPath("$.data.members[0].profileUrl").value(member1.getProfileImageUrl()))
                .andExpect(jsonPath("$.data.members[0].authority").value("ADMIN"))
                .andExpect(jsonPath("$.data.members[1].id").exists())
                .andExpect(jsonPath("$.data.members[1].name").value(member2.getName()))
                .andExpect(jsonPath("$.data.members[1].profileUrl").value(member2.getProfileImageUrl()))
                .andExpect(jsonPath("$.data.members[1].authority").value("READ_WRITE"))
                .andExpect(jsonPath("$.data.members[2].id").exists())
                .andExpect(jsonPath("$.data.members[2].name").value(member3.getName()))
                .andExpect(jsonPath("$.data.members[2].profileUrl").value(member3.getProfileImageUrl()))
                .andExpect(jsonPath("$.data.members[2].authority").value("READ_ONLY"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 목록 조회 - 실패 : 스페이스 멤버가 아님")
    void listSpaceMembers_Fail_NotSpaceMember() throws Exception {
        // given
        var space = spaceService.findByName("기존 스페이스 1_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());

        // when
        ResultActions resultActions = performGet(url);

        // then
        expectForbidden(resultActions, "액세스가 거부되었습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 목록 조회 - 실패 : 스페이스가 존재하지 않음")
    void listSpaceMembers_Fail_NotExistSpace() throws Exception {
        // given
        Integer spaceId = 9999;
        String url = "/api/v1/space/member/%d".formatted(spaceId);

        // when
        ResultActions resultActions = performGet(url);

        // then
        expectNotFound(resultActions, "존재하지 않는 스페이스입니다.");
    }

    // ============================= CHANGE MEMBER AUTHORITY ============================= //

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 성공")
    void changeMemberAuthority_Success() throws Exception {
        // given
        var member2 = memberService.findByKakaoKey("mc2222");
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "READ_ONLY",
                    "memberId": %d
                }
                """.formatted(member2.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectOk(resultActions, "멤버 권한을 변경했습니다.");

        resultActions
                .andExpect(jsonPath("$.data.spaceId").value(space.getId()))
                .andExpect(jsonPath("$.data.spaceName").value(space.getName()))
                .andExpect(jsonPath("$.data.member.id").value(member2.getId()))
                .andExpect(jsonPath("$.data.member.name").value(member2.getName()))
                .andExpect(jsonPath("$.data.member.profileUrl").value(member2.getProfileImageUrl()))
                .andExpect(jsonPath("$.data.member.authority").value("READ_ONLY"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 어드민 권한 없음")
    void changeMemberAuthority_Fail_NoAuthority() throws Exception {
        // given
        var member3 = memberService.findByKakaoKey("mc3333");
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "READ_WRITE",
                    "memberId": %d
                }
                """.formatted(member3.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectForbidden(resultActions, "액세스가 거부되었습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 스페이스가 존재하지 않음")
    void changeMemberAuthority_Fail_NotExistSpace() throws Exception {
        // given
        var member2 = memberService.findByKakaoKey("mc2222");
        Integer spaceId = 9999;
        String url = "/api/v1/space/member/%d".formatted(spaceId);
        String requestBody = """
                {
                    "newAuthority": "READ_ONLY",
                    "memberId": %d
                }
                """.formatted(member2.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectNotFound(resultActions, "존재하지 않는 스페이스입니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 멤버가 존재하지 않음")
    void changeMemberAuthority_Fail_NotExistMember() throws Exception {
        // given
        Integer memberId = 9999;
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "READ_ONLY",
                    "memberId": %d
                }
                """.formatted(memberId);

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectNotFound(resultActions, "9999 id를 가진 사용자를 찾을 수 없습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 멤버가 스페이스에 속해있지 않음")
    void changeMemberAuthority_Fail_MemberNotInSpace() throws Exception {
        // given
        var member3 = memberService.findByKakaoKey("mc3333");
        var space = spaceService.findByName("기존 스페이스 2_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "READ_ONLY",
                    "memberId": %d
                }
                """.formatted(member3.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectForbidden(resultActions, "액세스가 거부되었습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 잘못된 권한")
    void changeMemberAuthority_Fail_WrongAuthority() throws Exception {
        // given
        var member2 = memberService.findByKakaoKey("mc2222");
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "WRONG_AUTHORITY",
                    "memberId": %d
                }
                """.formatted(member2.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectBadRequest(resultActions, "Invalid value provided for field 'newAuthority'. Please check the allowed values.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 본인의 권한을 변경하려고 함")
    void changeMemberAuthority_Fail_ChangeOwnAuthority() throws Exception {
        // given
        var member1 = memberService.findByKakaoKey("mc1111");
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "READ_ONLY",
                    "memberId": %d
                }
                """.formatted(member1.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectBadRequest(resultActions, "본인의 권한은 변경할 수 없습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 이미 동일한 권한을 가지고 있음")
    void changeMemberAuthority_Fail_SameAuthority() throws Exception {
        // given
        var member2 = memberService.findByKakaoKey("mc2222");
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "READ_WRITE",
                    "memberId": %d
                }
                """.formatted(member2.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.msg").value("이미 요청된 권한을 가지고 있습니다."));
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 멤버Id 누락")
    void changeMemberAuthority_Fail_MemberIdMissing() throws Exception {
        // given
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "newAuthority": "READ_ONLY"
                }
                """;

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectBadRequest(resultActions, "memberId-NotNull-must not be null");
    }

    @Test
    @WithUserDetails(value = "KAKAO:mc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("스페이스 멤버 권한 변경 - 실패 : 권한 누락")
    void changeMemberAuthority_Fail_AuthorityMissing() throws Exception {
        // given
        var member2 = memberService.findByKakaoKey("mc2222");
        var space = spaceService.findByName("기존 스페이스 3_forMembershipControllerTest");
        String url = "/api/v1/space/member/%d".formatted(space.getId());
        String requestBody = """
                {
                    "memberId": %d
                }
                """.formatted(member2.getId());

        // when
        ResultActions resultActions = performPut(url, requestBody);

        // then
        expectBadRequest(resultActions, "newAuthority-NotNull-must not be null");
    }

}