package org.tuna.zoopzoop.backend.domain.dashboard.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.GraphRepository;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardControllerTest extends ControllerTestSupport {
    @Autowired
    private SpaceService spaceService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MembershipService membershipService;

    private Integer authorizedDashboardId;
    private Integer unauthorizedDashboardId;

    // 테스트에 필요한 유저, 스페이스, 멤버십 데이터를 미리 설정합니다.
    @BeforeAll
    void setUp() {
        // 1. 유저 생성
        memberService.createMember("tester1_forDashboardControllerTest", "url", "dc1111", Provider.KAKAO);
        memberService.createMember("tester2_forDashboardControllerTest", "url", "dc2222", Provider.KAKAO);

        // 2. 스페이스 생성 (생성과 동시에 대시보드도 생성됨)
        Space space1 = spaceService.createSpace("TestSpace1_forDashboardControllerTest", "thumb1");
        Space space2 = spaceService.createSpace("TestSpace2_forDashboardControllerTest", "thumb2");

        // 테스트에서 사용할 대시보드 ID 저장
        this.authorizedDashboardId = space1.getDashboard().getId();
        this.unauthorizedDashboardId = space2.getDashboard().getId();

        // 3. 멤버십 설정
        // user1은 Test Space 1에만 멤버로 가입 (접근 권한 있음)
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("dc1111"),
                space1,
                Authority.ADMIN
        );
        // user2는 Test Space 2에만 멤버로 가입
        membershipService.addMemberToSpace(
                memberService.findByKakaoKey("dc2222"),
                space2,
                Authority.ADMIN
        );
    }

    // ============================= GET GRAPH ============================= //

    @Test
    @WithUserDetails(value = "KAKAO:dc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("대시보드 그래프 데이터 조회 - 성공")
    void getGraph_Success() throws Exception {
        // Given
        String url = String.format("/api/v1/dashboard/%d/graph", authorizedDashboardId);

        // When
        ResultActions resultActions = performGet(url);

        // Then
        expectOk(
                resultActions,
                String.format("ID: %d 의 React-flow 데이터를 조회했습니다.", authorizedDashboardId)
        );
        resultActions
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.nodes").isEmpty())
                .andExpect(jsonPath("$.data.edges").isArray())
                .andExpect(jsonPath("$.data.edges").isEmpty());
    }

    @Test
    @WithUserDetails(value = "KAKAO:dc2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("대시보드 그래프 데이터 조회 - 실패: 접근 권한 없음")
    void getGraph_Fail_Forbidden() throws Exception {
        // Given
        // user2는 space1의 멤버가 아니므로, space1의 대시보드에 접근할 수 없음
        String url = String.format("/api/v1/dashboard/%d/graph", authorizedDashboardId);

        // When
        ResultActions resultActions = performGet(url);

        // Then
        // TODO: 실제 구현된 권한 체크 로직의 예외 메시지에 따라 "권한이 없습니다." 부분을 수정해야 합니다.
        expectForbidden(resultActions, "대시보드의 접근 권한이 없습니다.");
    }

    @Test
    @WithUserDetails(value = "KAKAO:dc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("대시보드 그래프 데이터 조회 - 실패: 존재하지 않는 대시보드")
    void getGraph_Fail_NotFound() throws Exception {
        // Given
        Integer nonExistentDashboardId = 9999;
        String url = String.format("/api/v1/dashboard/%d/graph", nonExistentDashboardId);

        // When
        ResultActions resultActions = performGet(url);

        // Then
        expectNotFound(
                resultActions,
                nonExistentDashboardId + " ID를 가진 대시보드를 찾을 수 없습니다."
        );
    }

    // ============================= UPDATE GRAPH ============================= //

    @Test
    @WithUserDetails(value = "KAKAO:dc1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("대시보드 그래프 데이터 저장 - 성공")
    void updateGraph_Success() throws Exception {
        // Given
        String url = String.format("/api/v1/dashboard/%d/graph", authorizedDashboardId);
        String requestBody = createReactFlowJsonBody();

        // When: 데이터 수정
        ResultActions updateResult = performPut(url, requestBody);

        // Then: 수정 성공 응답 확인
        expectOk(
                updateResult,
                "React-flow 데이터를 저장했습니다."
        );

        // When: 데이터 재조회하여 검증
        ResultActions getResult = performGet(url);

        // Then: 재조회 결과가 수정한 데이터와 일치하는지 확인
        getResult
                .andExpect(jsonPath("$.data.nodes", hasSize(2)))
                .andExpect(jsonPath("$.data.edges", hasSize(1)))
                .andExpect(jsonPath("$.data.nodes[0].id").value("1"))
                .andExpect(jsonPath("$.data.nodes[0].data.title").value("노드1"))
                .andExpect(jsonPath("$.data.edges[0].id").value("e1-2"));
    }

    @Test
    @DisplayName("대시보드 그래프 데이터 저장 - 실패: 존재하지 않는 대시보드")
    void updateGraph_Fail_NotFound() throws Exception {
        // Given
        Integer nonExistentDashboardId = 9999;
        String url = String.format("/api/v1/dashboard/%d/graph", nonExistentDashboardId);
        String requestBody = createReactFlowJsonBody();

        // When
        ResultActions resultActions = performPut(url, requestBody);

        // Then
        expectNotFound(
                resultActions,
                nonExistentDashboardId + " ID를 가진 대시보드를 찾을 수 없습니다."
        );
    }

//    @Test
//    @DisplayName("대시보드 그래프 데이터 저장 - 실패: 서명 검증 실패")
//    void updateGraph_Fail_Forbidden() throws Exception {
//        // Given
//        String url = String.format("/api/v1/dashboard/%d/graph", authorizedDashboardId);
//        String requestBody = createReactFlowJsonBody();
//
//        // When
//        ResultActions resultActions = performPut(url, requestBody);
//
//        // Then
//        // TODO: 실제 구현된 권한 체크 로직의 예외 메시지에 따라 "권한이 없습니다." 부분을 수정해야 합니다.
//        expectForbidden(resultActions, "액세스가 거부되었습니다.");
//    }

    // ======================= TEST DATA FACTORIES ======================== //

    private String createReactFlowJsonBody() {
        return """
            {
                "nodes": [
                    {
                        "id": "1",
                        "type": "CUSTOM",
                        "data": { "title": "노드1", "description": "설명1" },
                        "position": { "x": 100, "y": 200 }
                    },
                    {
                        "id": "2",
                        "type": "CUSTOM",
                        "data": { "title": "노드2" },
                        "position": { "x": 300, "y": 400 }
                    }
                ],
                "edges": [
                    {
                        "id": "e1-2",
                        "source": "1",
                        "target": "2",
                        "type": "SMOOTHSTEP",
                        "animated": true,
                        "style": { "stroke": "#999", "strokeWidth": 2.0 }
                    }
                ]
            }
            """;
    }

}