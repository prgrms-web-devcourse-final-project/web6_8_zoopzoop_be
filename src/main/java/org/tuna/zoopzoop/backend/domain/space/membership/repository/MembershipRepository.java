package org.tuna.zoopzoop.backend.domain.space.membership.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    boolean existsByMemberAndSpace(Member member, Space space);

    Page<Membership> findAllByMemberAndAuthority(Member member, Authority authority, Pageable pageable);
    Page<Membership> findAllByMemberAndAuthorityIsNot(Member member, Authority authority, Pageable pageable);
    Page<Membership> findAllByMember(Member member, Pageable pageable);

    List<Membership> findAllByMemberAndAuthority(Member member, Authority authority);
    List<Membership> findAllByMemberAndAuthorityIsNot(Member member, Authority authority);
    List<Membership> findAllByMember(Member member);

    boolean existsByMemberAndSpaceAndAuthorityIsNot(Member member, Space space, Authority authority);

    boolean existsByMemberAndSpaceAndAuthority(Member member, Space space, Authority authority);

    Optional<Membership> findByMemberAndSpace(Member member, Space space);

    List<Membership> findAllBySpaceAndAuthority(Space space, Authority authority);

    List<Membership> findAllBySpaceAndAuthorityIsNot(Space space, Authority authority);

    long countBySpaceAndAuthority(Space space, Authority authority);
}
