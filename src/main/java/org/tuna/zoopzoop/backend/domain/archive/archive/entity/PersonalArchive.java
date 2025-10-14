package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.archive.enums.ArchiveType;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class PersonalArchive extends BaseEntity {
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "archive_id", nullable = false)
    public Archive archive;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public PersonalArchive(Member member) {
        this.member = member;
        this.archive = new Archive(ArchiveType.PERSONAL);
    }
}
