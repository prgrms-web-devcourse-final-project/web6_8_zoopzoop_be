package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@MappedSuperclass
public abstract class ArchiveMapping extends BaseEntity {
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "archive_id", nullable = false)
    public Archive archive;
}
