package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.enums.ArchiveType;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Archive extends BaseEntity {
    //연결된 객체(개인 아카이브, 공유 아카이브)타입.
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ArchiveType type;

    //연결된 객체의 id
    @Column(nullable = false)
    private Integer entityId;
}
