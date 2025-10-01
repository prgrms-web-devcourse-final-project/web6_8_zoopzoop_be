package org.tuna.zoopzoop.backend.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Entity
@Getter
@Setter
public class Dashboard extends BaseEntity {
    // 대시보드의 이름
    @Column(nullable = false)
    private String name;

    // 이 대시보드가 속한 스페이스
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    // 이 대시보드가 담고 있는 그래프 콘텐츠 (1:1 관계)
    // Cascade 설정을 통해 Dashboard 저장 시 Graph도 함께 저장되도록 함
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "graph_id")
    private Graph graph;

    // Dashboard 생성 시 비어있는 Graph를 함께 생성하는 편의 메서드
    public static Dashboard create(String name, Space space) {
        Dashboard dashboard = new Dashboard();
        dashboard.setName(name);
        dashboard.setSpace(space);
        dashboard.setGraph(new Graph()); // 비어있는 Graph 생성 및 연결
        return dashboard;
    }
}
