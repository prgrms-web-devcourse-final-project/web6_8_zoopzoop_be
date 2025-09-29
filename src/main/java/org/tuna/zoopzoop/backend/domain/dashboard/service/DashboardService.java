package org.tuna.zoopzoop.backend.domain.dashboard.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.BodyForReactFlow;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Dashboard;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Edge;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Node;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.DashboardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {
    private final DashboardRepository dashboardRepository;
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
}
