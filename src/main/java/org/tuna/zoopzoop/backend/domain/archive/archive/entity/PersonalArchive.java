package org.tuna.zoopzoop.backend.domain.archive.archive.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class PersonalArchive extends ArchiveMapping {
    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public PersonalArchive(Member member) {
        this.member = member;
        this.archive = new Archive("개인");
    }
}
