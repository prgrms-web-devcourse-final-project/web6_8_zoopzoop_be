package org.tuna.zoopzoop.backend.domain.member.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.MemberShip;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.util.List;

@Setter
@Getter
@Entity
@NoArgsConstructor
public class Member extends BaseEntity {
    //---------- 필드 ----------//
    @Column(unique = true, nullable = false)
    private String name;

//    @Column(unique = true, nullable = false)
//    private String email;

    @Column(unique = true, nullable = false)
    private Long kakaoKey;

    @Column
    private String profileImageUrl;

    @Column
    private Boolean active; //soft-delete 용 status, default = true;

    //---------- 연관 관계 ----------//
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private PersonalArchive personalArchive; //PersonalArchive(Archive 매핑 테이블), 1:1 관계, cascade.all

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberShip> memberShips; //MemberShip, 1:N 관계, cascade.all

    //---------- 생성자 ----------//
    @Builder
    public Member(String name, Long kakaoKey, String profileImageUrl) {
        this.name = name;
        this.kakaoKey = kakaoKey;
        this.profileImageUrl = profileImageUrl;
        this.active = true;
        this.personalArchive = new PersonalArchive(this); //Member 객체 생성 시 PersonalArchive 자동 생성.
    }

    //---------- 메소드 ----------//
    public boolean isActive() { return this.active; }
    public void updateName(String name) { //사용자 이름 수정
        this.name = name;
    } //사용자 이름 변경
    public void deactivate() { this.active = false; } //soft-delete
    public void activate() { this.active = true; } //restore
}
