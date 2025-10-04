package org.tuna.zoopzoop.backend.domain.dashboard.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Edge;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Node;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.EdgeType;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.NodeType;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.GraphRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GraphServiceTest {
    @Autowired
    private GraphRepository graphRepository;

    @AfterEach
    void cleanUp() {
        graphRepository.deleteAll(); // Graph만 삭제
        // 필요하면 다른 Repository도 순서대로 삭제
    }

    @Test
    @DisplayName("Graph + Node + Edge 저장 테스트")
    void saveGraphWithNodesAndEdges() {
        // Graph 생성
        Graph graph = new Graph();

        // Node 생성
        Node node1 = new Node();
        node1.setNodeKey("1");
        node1.setNodeType(NodeType.CUSTOM);
        node1.setData(Map.of("title", "노드 제목", "description", "노드 설명"));
        node1.setPositonX(100);
        node1.setPositonY(200);
        node1.setGraph(graph); // 연관관계 주인 설정

        Node node2 = new Node();
        node2.setNodeKey("2");
        node2.setNodeType(NodeType.CUSTOM);
        node2.setPositonX(300);
        node2.setPositonY(400);
        node2.setGraph(graph);

        // Edge 생성
        Edge edge = new Edge();
        edge.setEdgeKey("e1-2");
        edge.setSourceNodeKey("1");
        edge.setTargetNodeKey("2");
        edge.setEdgeType(EdgeType.SMOOTHSTEP);
        edge.setAnimated(true);
        edge.setStroke("#999");
        edge.setStrokeWidth(2.0);
        edge.setGraph(graph);

        // graph와 연결
        graph.getNodes().add(node1);
        graph.getNodes().add(node2);
        graph.getEdges().add(edge);
        Graph savedGraph = graphRepository.save(graph);

        assertNotNull(savedGraph.getId());
        assertEquals(2, savedGraph.getNodes().size());
        assertEquals(1, savedGraph.getEdges().size());
    }
}
