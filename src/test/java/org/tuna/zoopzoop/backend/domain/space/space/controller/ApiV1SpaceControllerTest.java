package org.tuna.zoopzoop.backend.domain.space.space.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiV1SpaceControllerTest extends ControllerTestSupport {
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
        spaceService.createSpace("기존 스페이스 1");
        spaceService.createSpace("기존 스페이스 2");

    }

    void setUpMember() {
        memberService.createMember(
                "spaceControllerTester1",
                "url",
                "sc1111",
                Provider.KAKAO
        );
        memberService.createMember(
                "spaceControllerTester2",
                "url",
                "sc2222",
                Provider.KAKAO
        );
        memberService.createMember(
                "spaceControllerTester3",
                "url",
                "sc3333",
                Provider.KAKAO
        );
    }

    void setUpMembership() {
        // test1 -> 스페이스 1 가입 (ADMIN)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("sc1111"),
                spaceService.findByName("기존 스페이스 1"),
                Authority.ADMIN
        );
        // test2 -> 스페이스 1 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("sc2222"),
                spaceService.findByName("기존 스페이스 1"),
                Authority.PENDING
        );
        // test1 -> 스페이스 2 가입 (PENDING)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("sc1111"),
                spaceService.findByName("기존 스페이스 2"),
                Authority.PENDING
        );
    }

    // ============================= CREATE ============================= //

    @Test
    @DisplayName("스페이스 생성 - 성공")
    void createSpace_Success() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = createDefaultSpaceCreateRequestBody();

        // When
        ResultActions resultActions = performPost(url, requestBody);

        // Then
        expectCreated(
                resultActions,
                String.format("%s - 스페이스가 생성됐습니다.", "테스트 스페이스")
        );
        resultActions
                .andExpect(jsonPath("$.data.name").value("테스트 스페이스"));
    }

    @Test
    @DisplayName("스페이스 생성 - 실패 : 스페이스명 누락")
    void createSpace_Fail_NameMissing() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = """
                {
                    "name": ""
                }
                """;

        // When
        ResultActions resultActions = performPost(url, requestBody);

        // Then
        expectBadRequest(
                resultActions,
                "name-NotBlank-must not be blank"
        );
    }

    @Test
    @DisplayName("스페이스 생성 - 실패 : 스페이스명 길이 초과")
    void createSpace_Fail_NameTooLong() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = """
                {
                    "name": "테스트 스페이스 이름이 너무 길어서 50자를 초과하는 경우입니다. 테스트 스페이스 이름이 너무 길어서 50자를 초과하는 경우입니다."
                }
                """;

        // When
        ResultActions resultActions = performPost(url, requestBody);

        // Then
        expectBadRequest(
                resultActions,
                "name-Length-length must be between 0 and 50"
        );
    }

    @Test
    @DisplayName("스페이스 생성 - 실패 : 스페이스명 중복")
    void createSpace_Fail_NameDuplicate() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = createDefaultSpaceCreateRequestBody();
        performPost(url, requestBody); // 최초 생성

        // When
        ResultActions resultActions = performPost(url, requestBody); // 중복 생성 시도

        // Then
        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 스페이스 이름입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    // ============================= DELETE ============================= //

    @Test
    @DisplayName("스페이스 삭제 - 성공")
    void deleteSpace_Success() throws Exception {
        // Given
        Integer spaceId = spaceService.findByName("기존 스페이스 1").getId();
        String url = String.format("/api/v1/space/%d", spaceId);

        // When
        ResultActions resultActions = performDelete(url);

        // Then
        expectOk(
                resultActions,
                String.format("%s - 스페이스가 삭제됐습니다.", "기존 스페이스 1")
        );
        resultActions
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("스페이스 삭제 - 실패 : 존재하지 않는 스페이스")
    void deleteSpace_Fail_NotFound() throws Exception {
        // Given
        Integer spaceId = 9999; // 존재하지 않는 스페이스 ID
        String url = String.format("/api/v1/space/%d", spaceId);

        // When
        ResultActions resultActions = performDelete(url);

        // Then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 스페이스입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }


    // ======================= Modify ======================== //
    @Test
    @DisplayName("스페이스 이름 변경 - 성공")
    void modifySpaceName_Success() throws Exception {
        // Given
        Integer spaceId = spaceService.findByName("기존 스페이스 1").getId();
        String url = String.format("/api/v1/space/%d", spaceId);
        String requestBody = """
                {
                    "name": "변경된 스페이스 이름"
                }
                """;

        // When
        ResultActions resultActions = performPut(url, requestBody);

        // Then
        expectOk(
                resultActions,
                String.format("%s - 스페이스 이름이 변경됐습니다.", "변경된 스페이스 이름")
        );
        resultActions
                .andExpect(jsonPath("$.data.name").value("변경된 스페이스 이름"));
    }

    @Test
    @DisplayName("스페이스 이름 변경 - 실패 : 존재하지 않는 스페이스")
    void modifySpaceName_Fail_NotFound() throws Exception {
        // Given
        Integer spaceId = 9999; // 존재하지 않는 스페이스 ID
        String url = String.format("/api/v1/space/%d", spaceId);
        String requestBody = """
                {
                    "name": "변경된 스페이스 이름"
                }
                """;

        // When
        ResultActions resultActions = performPut(url, requestBody);

        // Then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 스페이스입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("스페이스 이름 변경 - 실패 : 스페이스명 누락")
    void modifySpaceName_Fail_NameMissing() throws Exception {
        // Given
        Integer spaceId = spaceService.findByName("기존 스페이스 1").getId();
        String url = String.format("/api/v1/space/%d", spaceId);
        String requestBody = """
                {
                    "name": ""
                }
                """;

        // When
        ResultActions resultActions = performPut(url, requestBody);

        // Then
        expectBadRequest(
                resultActions,
                "name-NotBlank-must not be blank"
        );
    }

    @Test
    @DisplayName("스페이스 이름 변경 - 실패 : 스페이스명 길이 초과")
    void modifySpaceName_Fail_NameTooLong() throws Exception {
        // Given
        Integer spaceId = spaceService.findByName("기존 스페이스 1").getId();
        String url = String.format("/api/v1/space/%d", spaceId);
        String requestBody = """
                {
                    "name": "테스트 스페이스 이름이 너무 길어서 50자를 초과하는 경우입니다. 테스트 스페이스 이름이 너무 길어서 50자를 초과하는 경우입니다."
                }
                """;

        // When
        ResultActions resultActions = performPut(url, requestBody);

        // Then
        expectBadRequest(
                resultActions,
                "name-Length-length must be between 0 and 50"
        );
    }

    @Test
    @DisplayName("스페이스 이름 변경 - 실패 : 스페이스명 중복")
    void modifySpaceName_Fail_NameDuplicate() throws Exception {
        // Given
        Integer spaceId = spaceService.findByName("기존 스페이스 1").getId();
        String url = String.format("/api/v1/space/%d", spaceId);
        String requestBody = """
                {
                    "name": "기존 스페이스 2"
                }
                """;

        // When
        ResultActions resultActions = performPut(url, requestBody);

        // Then
        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 스페이스 이름입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    // ======================= Read ======================= //

    @Test
    @WithUserDetails(value = "KAKAO:sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("나의 스페이스 전체 조회 - 성공")
    void getMySpaces_Success() throws Exception {
        // Given
        String url = "/api/v1/space";

        // When
        ResultActions resultActions = performGet(url);

        // Then
        expectOk(
                resultActions,
                "스페이스 목록이 조회됐습니다."
        );
        resultActions
                .andExpect(jsonPath("$.data.spaces").isArray())
                .andExpect(jsonPath("$.data.spaces.length()").value(2))
                .andDo(print());

        resultActions
                .andExpect(jsonPath("$.data.spaces[0].id").isNumber())
                .andExpect(jsonPath("$.data.spaces[0].name").value("기존 스페이스 1"))
                .andExpect(jsonPath("$.data.spaces[0].authority").value("ADMIN"))
                .andExpect(jsonPath("$.data.spaces[1].id").isNumber())
                .andExpect(jsonPath("$.data.spaces[1].name").value("기존 스페이스 2"))
                .andExpect(jsonPath("$.data.spaces[1].authority").value("PENDING"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("초대받은 스페이스 전체 조회 - 성공")
    void getInvitedSpaces_Success() throws Exception {
        // Given
        String url = "/api/v1/space?state=PENDING";

        // When
        ResultActions resultActions = performGet(url);

        // Then
        expectOk(
                resultActions,
                "스페이스 목록이 조회됐습니다."
        );
        resultActions
                .andExpect(jsonPath("$.data.spaces").isArray())
                .andExpect(jsonPath("$.data.spaces.length()").value(1))
                .andDo(print());

        resultActions
                .andExpect(jsonPath("$.data.spaces[0].id").isNumber())
                .andExpect(jsonPath("$.data.spaces[0].name").value("기존 스페이스 2"))
                .andExpect(jsonPath("$.data.spaces[0].authority").value("PENDING"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("가입 중인 스페이스 전체 조회 - 성공")
    void getJoinedSpaces_Success() throws Exception {
        // Given
        String url = "/api/v1/space?state=JOINED";

        // When
        ResultActions resultActions = performGet(url);

        // Then
        expectOk(
                resultActions,
                "스페이스 목록이 조회됐습니다."
        );
        resultActions
                .andExpect(jsonPath("$.data.spaces").isArray())
                .andExpect(jsonPath("$.data.spaces.length()").value(2))
                .andDo(print());

        resultActions
                .andExpect(jsonPath("$.data.spaces[0].id").isNumber())
                .andExpect(jsonPath("$.data.spaces[0].name").value("기존 스페이스 1"))
                .andExpect(jsonPath("$.data.spaces[0].authority").value("ADMIN"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("나의 스페이스 전체 조회 - 실패 : 인증되지 않은 사용자")
    void getMySpaces_Fail_Unauthorized() throws Exception {
        // Given
        String url = "/api/v1/space";

        // When
        ResultActions resultActions = performGet(url);

        // Then
        expectUnauthorized(resultActions);
    }

    @Test
    @WithUserDetails(value = "KAKAO:sc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("나의 스페이스 전체 조회 - 실패 : 잘못된 state 파라미터")
    void getMySpaces_Fail_InvalidState() throws Exception {
        // Given
        String url = "/api/v1/space?state=INVALID";

        // When
        ResultActions resultActions = performGet(url);

        // Then
        expectBadRequest(
                resultActions,
                "state-InvalidState-잘못된 요청입니다."
        );
    }

    // ======================= TEST DATA FACTORIES ======================== //

    private String createDefaultSpaceCreateRequestBody() {
        return """
                {
                    "name": "테스트 스페이스"
                }
                """;
    }


}