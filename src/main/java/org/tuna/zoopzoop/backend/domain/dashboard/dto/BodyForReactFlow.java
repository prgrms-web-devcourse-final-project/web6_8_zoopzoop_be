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
            @JsonProperty("selected") Boolean selected,
            @JsonProperty("dragging") Boolean dragging,
            @JsonProperty("position") PositionDto positionDto,
            @JsonProperty("measured") MeasurementsDto measurements,
            @JsonProperty("data") DataDto data
    ) {
        public record PositionDto(
            @JsonProperty("x") double x,
            @JsonProperty("y") double y
        ) {}

        public record MeasurementsDto(
            @JsonProperty("width") double width,
            @JsonProperty("height") double height
        ) {}

        public record DataDto(
            @JsonProperty("content") String content,
            @JsonProperty("createdAt") String createAt, // YYYY-MM-DD format
            @JsonProperty("link") String sourceUrl,
            @JsonProperty("title") String title,
            @JsonProperty("user") WriterDto writer
        ) {}

        public record WriterDto(
                @JsonProperty("name") String name,
                @JsonProperty("profileUrl") String profileImageUrl
        ) {}

    }

    public record EdgeDto(
            @JsonProperty("id") String edgeKey,
            @JsonProperty("source") String sourceNodeKey,
            @JsonProperty("target") String targetNodeKey
    ) {
    }

    // DTO -> Entity, BodyForReactFlow를 Graph 엔티티로 변환
    public Graph toEntity() {
        Graph graph = new Graph();

        List<Node> nodeEntities = this.nodes().stream()
                .map(dto -> {
                    Node node = new Node();
                    node.setNodeKey(dto.nodeKey());
                    node.setNodeType(NodeType.valueOf(dto.nodeType().toUpperCase()));
                    node.setSelected(dto.selected() != null ? dto.selected() : false);
                    node.setDragging(dto.dragging() != null ? dto.dragging() : false);
                    node.setPositionX(dto.positionDto().x());
                    node.setPositionY(dto.positionDto().y());
                    if (dto.measurements() != null) {
                        node.setWidth(dto.measurements().width());
                        node.setHeight(dto.measurements().height());
                    }
                    if (dto.data() != null) {
                        node.setData(Map.of(
                                "content", dto.data().content(),
                                "createdAt", dto.data().createAt(),
                                "sourceUrl", dto.data().sourceUrl(),
                                "title", dto.data().title(),
                                "writerName", dto.data().writer() != null ? dto.data().writer().name() : null,
                                "writerProfileImageUrl", dto.data().writer() != null ? dto.data().writer().profileImageUrl() : null
                        ));
                    }
                    return node;
                })
                .toList();

        List<Edge> edgeEntities = this.edges().stream()
                .map(dto -> {
                    Edge edge = new Edge();
                    edge.setEdgeKey(dto.edgeKey());
                    edge.setSourceNodeKey(dto.sourceNodeKey());
                    edge.setTargetNodeKey(dto.targetNodeKey());
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
                        n.isSelected(),
                        n.isDragging(),
                        new NodeDto.PositionDto(n.getPositionX(), n.getPositionY()),
                        new NodeDto.MeasurementsDto(n.getWidth(), n.getHeight()),
                        new NodeDto.DataDto(
                                n.getData().get("content"),
                                n.getData().get("createdAt"),
                                n.getData().get("sourceUrl"),
                                n.getData().get("title"),
                                new NodeDto.WriterDto(
                                        n.getData().get("writerName"),
                                        n.getData().get("writerProfileImageUrl")
                                )
                        )
                ))
                .toList();

        List<EdgeDto> edgeDtos = graph.getEdges().stream()
                .map(e -> new EdgeDto(
                        e.getEdgeKey(),
                        e.getSourceNodeKey(),
                        e.getTargetNodeKey()
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
                    node.setPositionX(dto.positionDto().x());
                    node.setPositionY(dto.positionDto().y());
                    if (dto.measurements() != null) {
                        node.setWidth(dto.measurements().width());
                        node.setHeight(dto.measurements().height());
                    }
                    if (dto.data() != null) {
                        node.setData(Map.of(
                                "content", dto.data().content(),
                                "createdAt", dto.data().createAt(),
                                "sourceUrl", dto.data().sourceUrl(),
                                "title", dto.data().title(),
                                "writerName", dto.data().writer() != null ? dto.data().writer().name() : null,
                                "writerProfileImageUrl", dto.data().writer() != null ? dto.data().writer().profileImageUrl() : null
                        ));
                    }
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
                    edge.setGraph(graph); // 연관관계 설정
                    return edge;
                })
                .toList();
    }


}
