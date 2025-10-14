package org.tuna.zoopzoop.backend.domain.archive.folder.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Folder extends BaseEntity {
    //연결된 아카이브 id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", nullable = false)
    private Archive archive;

    //폴더 이름
    @Column(nullable = false)
    private String name;

    //디폴트 폴더 여부
    @Column(nullable = false)
    private boolean isDefault = false;
}
