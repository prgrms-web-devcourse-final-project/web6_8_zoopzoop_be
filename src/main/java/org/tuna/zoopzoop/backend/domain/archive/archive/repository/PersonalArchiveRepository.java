package org.tuna.zoopzoop.backend.domain.archive.archive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;

import java.util.Optional;

public interface PersonalArchiveRepository extends JpaRepository<PersonalArchive, Integer> {
    /**
     * 회원의 PersonalArchive 조회
     *
     * @param memberId   회원 Id
     * @return  PersonalArchive 엔티티
     */
    @Query("""
    select pa
    from PersonalArchive pa
    join fetch pa.archive a
    where pa.member.id = :memberId
""")
    Optional<PersonalArchive> findByMemberId(@Param("memberId") Integer memberId);
}
