package org.tuna.zoopzoop.backend.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.EdgeType;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
public class Edge extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_id")
    private Graph graph;

    @Column
    private String edgeKey;

    @Column
    private String sourceNodeKey;

    @Column
    private String targetNodeKey;

    @Column
    @Enumerated(EnumType.STRING)
    private EdgeType edgeType;

    @Column
    boolean isAnimated;

    @Column
    private String stroke;

    @Column
    private Double strokeWidth;
}
