package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class SharingArchive extends ArchiveMapping {
    @OneToOne
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;
}
