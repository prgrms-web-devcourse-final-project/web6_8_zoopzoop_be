package org.tuna.zoopzoop.backend.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.NodeType;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
public class Node extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_id")
    private Graph graph;

    @Column
    private String nodeKey;

    @Column
    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    @ElementCollection
    @CollectionTable(name = "node_data", joinColumns = @JoinColumn(name = "node_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    private Map<String, String> data = new HashMap<>();

    @Column
    private double positonX;

    @Column
    private double positonY;
}
