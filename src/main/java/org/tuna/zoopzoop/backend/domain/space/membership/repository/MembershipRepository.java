package org.tuna.zoopzoop.backend.domain.space.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    boolean existsByMemberAndSpace(Member member, Space space);

    List<Membership> findAllByMemberAndAuthority(Member member, Authority authority);

    List<Membership> findAllByMemberAndAuthorityIsNot(Member member, Authority authority);

    List<Membership> findAllByMember(Member member);
}
