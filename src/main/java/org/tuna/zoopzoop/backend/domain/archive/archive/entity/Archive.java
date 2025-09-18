package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.enums.ArchiveType;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class Archive extends BaseEntity {
    @Column
    @Enumerated(EnumType.STRING)
    private ArchiveType archiveType;

    public Archive(ArchiveType archiveType) {
        this.archiveType = archiveType;
    }
}
