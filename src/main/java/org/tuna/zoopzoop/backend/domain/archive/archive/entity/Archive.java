package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.enums.ArchiveType;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Archive extends BaseEntity {
    // Personal / Shared 생성 후 불변
    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ArchiveType archiveType;

    //아카이브 삭제(아마도 계정 탈퇴) 시 폴더 일괄 삭제
    @OneToMany(mappedBy = "archive", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Folder> folders = new ArrayList<>();

    public Archive(ArchiveType archiveType) {
        this.archiveType = archiveType;
    }

    public void addFolder(Folder folder) {
        if (!this.folders.contains(folder)) {
            this.folders.add(folder);
        }
        if (folder.getArchive() != this) {
            folder.setArchive(this);
        }
    }

    public void removeFolder(Folder folder) {
        this.folders.remove(folder);
        if (folder.getArchive() == this) {
            folder.setArchive(null);
        }
    }
}
