package org.tuna.zoopzoop.backend.domain.archive.folder.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        // 복합 Unique 제약(archive_id, name)
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_folder__archive_id__name",
                        columnNames = {"archive_id", "name"}
                )
        },
        // Archive 별 조회 속도 개선
        indexes = {
                @Index( name = "idx_folder__archive_id", columnList = "archive_id")
        }
)
public class Folder extends BaseEntity {
    //연결된 아카이브 id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "archive_id")
    private Archive archive;

    //폴더 이름
    @Column(nullable = false)
    private String name;

    //디폴트 폴더 여부
    @Column(nullable = false)
    private boolean isDefault = false;

    // 폴더 삭제 시 데이터 일괄 삭제
    @OneToMany(mappedBy = "folder", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<DataSource> dataSources = new ArrayList<>();
}
