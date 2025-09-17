package org.tuna.zoopzoop.backend.domain.space.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Space extends BaseEntity {
    //Space 이름
    @Column(unique = true, nullable = false)
    private String name;

    //soft-delete 용 status
    //default = true;
    @Column(nullable = false)
    private boolean active;

    //연결된 MemberShip
    //Space 삭제시 cascade.all
    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberShip> memberShips;

    //연결된 Invitation
    //Space 삭제시 cascade.all
    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invitation> invitations;

    @Builder
    public Space(String name, Boolean active) {
        this.name = name;
        this.active = active;
    }
}
