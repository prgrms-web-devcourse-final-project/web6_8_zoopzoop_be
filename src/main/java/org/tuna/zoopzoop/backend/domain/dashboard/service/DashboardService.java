package org.tuna.zoopzoop.backend.domain.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.BodyForReactFlow;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Dashboard;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Edge;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Node;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.DashboardRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.security.MessageDigest;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {
    private final DashboardRepository dashboardRepository;
    private final MembershipService membershipService;
    private final ObjectMapper objectMapper;
    private final SignatureService signatureService;



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




}
