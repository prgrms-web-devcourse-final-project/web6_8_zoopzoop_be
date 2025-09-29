package org.tuna.zoopzoop.backend.domain.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Edge;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Node;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.EdgeType;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.NodeType;

import java.util.List;
import java.util.Map;

// Request, Response 범용 Dto
public record BodyForReactFlow(
        List<NodeDto> nodes,
        List<EdgeDto> edges
) {

    public record NodeDto(
            @JsonProperty("id") String nodeKey,
            @JsonProperty("type") String nodeType,
            Map<String, String> data,
            @JsonProperty("position") PositionDto positionDto
    ) {
        public record PositionDto(
            @JsonProperty("x") double x,
            @JsonProperty("y") double y
    ) {}
    }

    public record EdgeDto(
            @JsonProperty("id") String edgeKey,
            @JsonProperty("source") String sourceNodeKey,
            @JsonProperty("target") String targetNodeKey,
            @JsonProperty("type") String edgeType,
            @JsonProperty("animated") boolean isAnimated,
            @JsonProperty("style") StyleDto styleDto
    ) {
        public record StyleDto(
                String stroke,
                Double strokeWidth
        ) {}
    }

    // DTO -> Entity, BodyForReactFlow를 Graph 엔티티로 변환
    public Graph toEntity() {
        Graph graph = new Graph();

        List<Node> nodeEntities = this.nodes().stream()
                .map(dto -> {
                    Node node = new Node();
                    node.setNodeKey(dto.nodeKey());
                    node.setNodeType(NodeType.valueOf(dto.nodeType().toUpperCase()));
                    node.setData(dto.data());
                    node.setPositonX(dto.positionDto().x());
                    node.setPositonY(dto.positionDto().y());
                    node.setGraph(graph); // 연관관계 설정
                    return node;
                })
                .toList();

        List<Edge> edgeEntities = this.edges().stream()
                .map(dto -> {
                    Edge edge = new Edge();
                    edge.setEdgeKey(dto.edgeKey());
                    edge.setSourceNodeKey(dto.sourceNodeKey());
                    edge.setTargetNodeKey(dto.targetNodeKey());
                    edge.setEdgeType(EdgeType.valueOf(dto.edgeType().toUpperCase()));
                    edge.setAnimated(dto.isAnimated());
                    if (dto.styleDto() != null) {
                        edge.setStroke(dto.styleDto().stroke());
                        edge.setStrokeWidth(dto.styleDto().strokeWidth());
                    }
                    edge.setGraph(graph); // 연관관계 설정
                    return edge;
                })
                .toList();

        graph.getNodes().addAll(nodeEntities);
        graph.getEdges().addAll(edgeEntities);

        return graph;
    }

    // Entity -> DTO, Graph 엔티티를 ResBodyForReactFlow로 변환
    public static BodyForReactFlow from(Graph graph) {
        List<NodeDto> nodeDtos = graph.getNodes().stream()
                .map(n -> new NodeDto(
                        n.getNodeKey(),
                        n.getNodeType().name().toUpperCase(),
                        n.getData(),
                        new NodeDto.PositionDto(n.getPositonX(), n.getPositonY())
                ))
                .toList();

        List<EdgeDto> edgeDtos = graph.getEdges().stream()
                .map(e -> new EdgeDto(
                        e.getEdgeKey(),
                        e.getSourceNodeKey(),
                        e.getTargetNodeKey(),
                        e.getEdgeType().name().toUpperCase(),
                        e.isAnimated(),
                        new EdgeDto.StyleDto(e.getStroke(), e.getStrokeWidth())
                ))
                .toList();

        return new BodyForReactFlow(nodeDtos, edgeDtos);
    }

    public List<Node> toNodeEntities(Graph graph) {
        return this.nodes().stream()
                .map(dto -> {
                    Node node = new Node();
                    node.setNodeKey(dto.nodeKey());
                    node.setNodeType(NodeType.valueOf(dto.nodeType().toUpperCase()));
                    node.setData(dto.data());
                    node.setPositonX(dto.positionDto().x());
                    node.setPositonY(dto.positionDto().y());
                    node.setGraph(graph); // 연관관계 설정
                    return node;
                })
                .toList();
    }

    public List<Edge> toEdgeEntities(Graph graph) {
        return this.edges().stream()
                .map(dto -> {
                    Edge edge = new Edge();
                    edge.setEdgeKey(dto.edgeKey());
                    edge.setSourceNodeKey(dto.sourceNodeKey());
                    edge.setTargetNodeKey(dto.targetNodeKey());
                    edge.setEdgeType(EdgeType.valueOf(dto.edgeType().toUpperCase()));
                    edge.setAnimated(dto.isAnimated());
                    if (dto.styleDto() != null) {
                        edge.setStroke(dto.styleDto().stroke());
                        edge.setStrokeWidth(dto.styleDto().strokeWidth());
                    }
                    edge.setGraph(graph); // 연관관계 설정
                    return edge;
                })
                .toList();
    }


}
