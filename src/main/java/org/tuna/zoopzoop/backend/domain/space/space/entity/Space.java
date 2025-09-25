package org.tuna.zoopzoop.backend.domain.space.space.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.SharingArchive;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.util.List;

@Getter
@Setter
@Entity
public class Space extends BaseEntity {
    //Space 이름
    @Column(unique = true, nullable = false)
    private String name;

    //soft-delete 용 status
    //default = true;
    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private SharingArchive sharingArchive;

    @Column(nullable = true)
    private String imageUrl;

    //연결된 MemberShip
    //Space 삭제시 cascade.all
    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberShips;

    public Space() {
        this.sharingArchive = new SharingArchive(this);
    }

    @Builder
    public Space(String name, Boolean active) {
        this.name = name;

        if (active != null)
            this.active = active;

        this.sharingArchive = new SharingArchive(this);
    }
}
