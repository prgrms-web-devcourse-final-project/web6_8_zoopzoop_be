package org.tuna.zoopzoop.backend.domain.space.membership.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    boolean existsByMemberAndSpace(Member member, Space space);

    Page<Membership> findAllByMemberAndAuthorityOrderById(Member member, Authority authority, Pageable pageable);
    Page<Membership> findAllByMemberAndAuthorityIsNotOrderById(Member member, Authority authority, Pageable pageable);
    Page<Membership> findAllByMemberOrderById(Member member, Pageable pageable);

    List<Membership> findAllByMemberAndAuthority(Member member, Authority authority);
    List<Membership> findAllByMemberAndAuthorityOrderById(Member member, Authority authority);
    List<Membership> findAllByMemberAndAuthorityIsNot(Member member, Authority authority);
    List<Membership> findAllByMemberAndAuthorityIsNotOrderById(Member member, Authority authority);
    List<Membership> findAllByMember(Member member);
    List<Membership> findAllByMemberOrderById(Member member);

    boolean existsByMemberAndSpaceAndAuthorityIsNot(Member member, Space space, Authority authority);

    boolean existsByMemberAndSpaceAndAuthority(Member member, Space space, Authority authority);

    Optional<Membership> findByMemberAndSpace(Member member, Space space);

    List<Membership> findAllBySpaceAndAuthority(Space space, Authority authority);
    List<Membership> findAllBySpaceAndAuthorityOrderById(Space space, Authority authority);

    List<Membership> findAllBySpaceAndAuthorityIsNot(Space space, Authority authority);
    List<Membership> findAllBySpaceAndAuthorityIsNotOrderById(Space space, Authority authority);

    long countBySpaceAndAuthority(Space space, Authority authority);

    @Query("""
    select m from Membership m
    where m.member.id = :memberId and m.space.id = :spaceId
""")
    Optional<Membership> findByMemberIdAndSpaceId(Integer memberId, Integer spaceId);

    @Query("SELECT m FROM Membership m JOIN FETCH m.space WHERE m.member = :member ORDER BY m.id ASC")
    Page<Membership> findAllByMemberWithSpace(@Param("member") Member member, Pageable pageable);

    @Query("SELECT m FROM Membership m JOIN FETCH m.space WHERE m.member = :member AND m.authority = :authority ORDER BY m.id ASC")
    Page<Membership> findAllByMemberAndAuthorityWithSpace(@Param("member") Member member, @Param("authority") Authority authority, Pageable pageable);

    @Query("SELECT m FROM Membership m JOIN FETCH m.space WHERE m.member = :member AND m.authority <> :authority ORDER BY m.id ASC")
    Page<Membership> findAllByMemberAndAuthorityIsNotWithSpace(@Param("member") Member member, @Param("authority") Authority authority, Pageable pageable);

}
