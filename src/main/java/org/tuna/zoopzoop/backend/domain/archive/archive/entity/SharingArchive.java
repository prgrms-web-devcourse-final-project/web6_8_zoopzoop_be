package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.enums.ArchiveType;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class SharingArchive extends BaseEntity {
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "archive_id", nullable = false)
    public Archive archive;

    @OneToOne
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    public SharingArchive(Space space) {
        this.space = space;
        this.archive = new Archive(ArchiveType.SHARED);

        // 🔧 default 폴더 자동 생성
        Folder defaultFolder = new Folder("default");
        this.archive.addFolder(defaultFolder);
    }
}
