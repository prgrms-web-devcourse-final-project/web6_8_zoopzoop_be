package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.*;
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
@Table(
        uniqueConstraints = {
                // Archive가 하나의 PersonalArchive에 연결됨
                @UniqueConstraint(
                        name = "uk_personal_archive__archive_id",
                        columnNames = "archive_id"
                ),
                // Member가 하나의 PersonalArchive만 가짐
                @UniqueConstraint(
                        name = "uk_personal_archive__member_id",
                        columnNames = "member_id"
                )
        }
)
public class PersonalArchive extends BaseEntity {
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "archive_id")
    public Archive archive;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public PersonalArchive(Member member) {
        this.member = member;
        this.archive = new Archive(ArchiveType.PERSONAL);
    }
}
