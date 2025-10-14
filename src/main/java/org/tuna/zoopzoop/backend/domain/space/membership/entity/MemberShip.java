package org.tuna.zoopzoop.backend.domain.space.membership.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class MemberShip extends BaseEntity {
    //Member 외래키
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    //Space 외래키
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    //권한(Enum 타입)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Authority authority;
}
