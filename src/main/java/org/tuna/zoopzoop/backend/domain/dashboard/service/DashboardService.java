package org.tuna.zoopzoop.backend.domain.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.BodyForReactFlow;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.GraphUpdateMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.ReqBodyForLiveblocksAuth;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Dashboard;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Edge;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Node;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.DashboardRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.global.clients.liveblocks.LiveblocksClient;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {
    private final DashboardRepository dashboardRepository;
    private final MembershipService membershipService;
    private final ObjectMapper objectMapper;
    private final SignatureService signatureService;
    private final RabbitTemplate rabbitTemplate;
    private final SpaceService spaceService;
    private final LiveblocksClient liveblocksClient;


    // =========================== Graph 관련 메서드 ===========================

    /**
     * 대시보드 ID를 통해 Graph 데이터를 조회하는 메서드
     */
    @Transactional(readOnly = true)
    public Graph getGraphByDashboardId(Integer dashboardId) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new NoResultException(dashboardId + " ID를 가진 대시보드를 찾을 수 없습니다."));

        return dashboard.getGraph();
    }

    /**
     * 특정 대시보드의 Graph 데이터를 덮어쓰는(수정) 메서드
     */
    public void updateGraph(Integer dashboardId, BodyForReactFlow dto) {
        Graph graph = getGraphByDashboardId(dashboardId);

        // 기존 Graph의 노드와 엣지를 모두 삭제
        graph.getNodes().clear();
        graph.getEdges().clear();

        // DTO로부터 새로운 노드와 엣지 Entity 리스트를 생성
        List<Node> newNodes = dto.toNodeEntities(graph); // DTO에 변환 로직이 있다고 가정
        List<Edge> newEdges = dto.toEdgeEntities(graph);

        // Graph에 새로운 리스트를 추가
        graph.getNodes().addAll(newNodes);
        graph.getEdges().addAll(newEdges);

    }

    /**
     * 서명 검증 후 Graph 업데이트를 수행하는 메서드
     * @param dashboardId 대시보드 ID
     * @param requestBody 요청 바디
     * @param signatureHeader 서명 헤더
     */
    public void verifyAndUpdateGraph(Integer dashboardId, String requestBody, String signatureHeader) {
        // 1. 서명 검증
        if (!signatureService.isValidSignature(requestBody, signatureHeader)) {
            throw new SecurityException("Invalid webhook signature.");
        }

        // 2. 검증 통과 후, 기존 업데이트 로직 실행
        try {
            BodyForReactFlow dto = objectMapper.readValue(requestBody, BodyForReactFlow.class);
            updateGraph(dashboardId, dto);
        } catch (NoResultException e) {
            throw new NoResultException(dashboardId + " ID를 가진 대시보드를 찾을 수 없습니다.");
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to process request body.", e);
        }
    }

    // =========================== 권한 관련 메서드 ===========================

    /**
     * 대시보드 접근 권한을 검증하는 메서드
     * @param member 접근을 시도하는 멤버
     * @param dashboardId 접근하려는 대시보드 ID
     */
    public void verifyAccessPermission(Member member, Integer dashboardId) throws AccessDeniedException {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new NoResultException(dashboardId + " ID를 가진 대시보드를 찾을 수 없습니다."));

        try {
            membershipService.findByMemberAndSpace(member, dashboard.getSpace());
        } catch (NoResultException e) {
            throw new AccessDeniedException("대시보드의 접근 권한이 없습니다.");
        }
    }

    // =========================== message 관리 메서드  ===========================

    /**
     * Graph 업데이트 요청을 RabbitMQ 큐에 비동기적으로 발행하는 메서드
     * @param dashboardId 대시보드 ID
     * @param requestBody 요청 바디
     * @param signatureHeader 서명 헤더
     */
    public void queueGraphUpdate(Integer dashboardId, String requestBody, String signatureHeader){
        // 서명 검증은 동기적으로 즉시 처리
        if (!signatureService.isValidSignature(requestBody, signatureHeader)) {
            throw new SecurityException("Invalid webhook signature.");
        }

        // 대시보드 존재 여부 확인
        if (!dashboardRepository.existsById(dashboardId)) {
            throw new NoResultException(dashboardId + " ID를 가진 대시보드를 찾을 수 없습니다.");
        }

        // 큐에 보낼 메시지 생성
        GraphUpdateMessage message = new GraphUpdateMessage(dashboardId, requestBody);

        // RabbitMQ에 메시지 발행
        rabbitTemplate.convertAndSend("zoopzoop.exchange", "graph.update.rk", message);
    }

    // =========================== 기타 메서드 ===========================

    /**
     * 특정 스페이스에 대한 Liveblocks 접속 토큰(JWT)을 발급합니다.
     * @param spaceId 스페이스 ID
     * @param member 토큰을 요청하는 멤버
     * @return 발급된 JWT 문자열
     * @throws AccessDeniedException 멤버가 해당 스페이스에 속해있지 않거나 권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public String getAuthTokenForSpace(Integer spaceId, Member member) throws AccessDeniedException {
        Space space = spaceService.findById(spaceId);

        // 해당 스페이스에 멤버가 속해있는지, PENDING 상태는 아닌지 확인
        Membership membership = membershipService.findByMemberAndSpace(member, space);
        if (membership.getAuthority().equals(Authority.PENDING)) {
            throw new AccessDeniedException("스페이스에 가입된 멤버가 아닙니다.");
        }

        // Liveblocks Room ID 생성
        String roomId = "space_" + space.getId();

        // Liveblocks에 전달할 사용자 정보 생성
        String userId = String.valueOf(member.getId());
        ReqBodyForLiveblocksAuth.UserInfo userInfo = new ReqBodyForLiveblocksAuth.UserInfo(
                member.getName(),
                member.getProfileImageUrl()
        );

        // Liveblocks 권한 설정 (내 서비스의 Authority -> Liveblocks 권한으로 변환)
        List<String> permissions;
        switch (membership.getAuthority()) {
            case ADMIN, READ_WRITE:
                permissions = List.of("room:write");
                break;
            case READ_ONLY:
                permissions = Collections.emptyList(); // 빈 리스트는 읽기 전용을 의미
                break;
            default:
                // PENDING 등 다른 상태는 위에서 이미 필터링됨
                throw new AccessDeniedException("유효하지 않은 권한입니다.");
        }

        // Liveblocks Client에 전달할 요청 객체 생성
        ReqBodyForLiveblocksAuth authRequest = new ReqBodyForLiveblocksAuth(
                userId,
                userInfo,
                Map.of(roomId, permissions)
        );

        // LiveblocksClient를 통해 토큰 발급 요청
        return liveblocksClient.getAuthToken(authRequest);
    }


}
