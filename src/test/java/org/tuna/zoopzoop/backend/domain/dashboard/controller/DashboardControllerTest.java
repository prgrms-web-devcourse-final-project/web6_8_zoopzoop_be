package org.tuna.zoopzoop.backend.domain.dashboard.controller;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.repository.SpaceRepository;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardControllerTest extends ControllerTestSupport {
    @Autowired private SpaceService spaceService;
    @Autowired private MemberService memberService;
    @Autowired private MembershipService membershipService;

    @Autowired SpaceRepository spaceRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired MembershipRepository membershipRepository;

    @Autowired private TransactionTemplate transactionTemplate;

    private Integer unauthorizedDashboardId;
    private Integer authorizedDashboardId;

    private String authorizedSpaceName = "TestSpace1_forDashboardControllerTest";
    private String unauthorizedSpaceName = "TestSpace2_forDashboardControllerTest";

    @Value("${liveblocks.secret-key}")
    private String testSecretKey;

    @BeforeAll
    void setUp() {
        // 1. 유저 생성
        memberService.createMember("tester1_forDashboardControllerTest", "url", "dc1111", Provider.KAKAO);
        memberService.createMember("tester2_forDashboardControllerTest", "url", "dc2222", Provider.KAKAO);

        // 2. 스페이스 생성 (생성과 동시에 대시보드도 생성됨)
        Space space1 = spaceService.createSpace(authorizedSpaceName, "thumb1");
        Space space2 = spaceService.createSpace(unauthorizedSpaceName, "thumb2");

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

    @AfterAll
    void tearDown() {
        // 멤버십, 스페이스, 멤버 모두 삭제
        membershipRepository.deleteAll();
        spaceRepository.deleteAll();
        memberRepository.deleteAll();
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
    @DisplayName("대시보드 그래프 데이터 저장 요청 - 성공")
    void updateGraph_Success() throws Exception {
        // Given
        String url = String.format("/api/v1/dashboard/%d/graph", authorizedDashboardId);
        String requestBody = createReactFlowJsonBody();
        String validSignature = generateLiveblocksSignature(requestBody);

        // When: 데이터 저장 요청 (메세지 발행)
        ResultActions updateResult = mvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Liveblocks-Signature", validSignature) // ★ 서명 헤더 추가
                .content(requestBody));

        // Then: 요청 접수 성공 응답 확인
        expectAccepted(updateResult, "데이터 업데이트 요청이 성공적으로 접수되었습니다.");

        // Then: (비동기 검증) 최종적으로 DB에 데이터가 반영될 때까지 최대 5초간 기다립니다.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // [수정] transactionTemplate을 사용하여 트랜잭션 내에서 검증 로직을 실행
            transactionTemplate.execute(status -> {
                Space space = spaceService.findByName(authorizedSpaceName);
                Graph updatedGraph = space.getDashboard().getGraph();
                assertThat(updatedGraph.getNodes()).hasSize(2);
                assertThat(updatedGraph.getEdges()).hasSize(1);
                assertThat(updatedGraph.getNodes().get(0).getData().get("title")).isEqualTo("노드1");
                return null; // execute 메서드는 반환값이 필요
            });
        });
    }

    @Test
    @DisplayName("대시보드 그래프 데이터 저장 - 실패: 존재하지 않는 대시보드")
    void updateGraph_Fail_NotFound() throws Exception {
        // Given
        Integer nonExistentDashboardId = 9999;
        String url = String.format("/api/v1/dashboard/%d/graph", nonExistentDashboardId);
        String requestBody = createReactFlowJsonBody();
        String validSignature = generateLiveblocksSignature(requestBody);

        // When: 데이터 저장
        ResultActions resultActions = mvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Liveblocks-Signature", validSignature) // ★ 서명 헤더 추가
                .content(requestBody));

        // Then
        expectNotFound(
                resultActions,
                nonExistentDashboardId + " ID를 가진 대시보드를 찾을 수 없습니다."
        );
    }

    @Test
    @DisplayName("대시보드 그래프 데이터 저장 - 실패: 서명 검증 실패")
    void updateGraph_Fail_Forbidden() throws Exception {
        // Given
        String url = String.format("/api/v1/dashboard/%d/graph", authorizedDashboardId);
        String requestBody = createReactFlowJsonBody();
        String invalidSignature = "t=123,v1=invalid_signature"; // 유효하지 않은 서명

        // When
        ResultActions resultActions = mvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Liveblocks-Signature", invalidSignature) // ★ 잘못된 서명 헤더 추가
                .content(requestBody));

        // Then
        expectForbidden(resultActions, "Invalid webhook signature.");
    }

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

    // ======================= HELPER METHODS ======================== //
    private String generateLiveblocksSignature(String requestBody) throws NoSuchAlgorithmException, InvalidKeyException {
        long timestamp = System.currentTimeMillis();
        String payload = timestamp + "." + requestBody;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(testSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        // v1 서명은 해시값의 Hex 인코딩 문자열입니다.
        String signatureHash = Hex.encodeHexString(hash);

        return String.format("t=%d,v1=%s", timestamp, signatureHash);
    }

}