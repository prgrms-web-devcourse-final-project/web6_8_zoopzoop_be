package org.tuna.zoopzoop.backend.domain.datasource.entity;

import jakarta.persistence.*;
import lombok.*;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag extends BaseEntity {
    //연결된 자료 id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = true) // BaseInitData 때문에 nullable true로 변경
    private DataSource dataSource;

    //태그명
    @Column(nullable = false)
    private String tagName;
}
