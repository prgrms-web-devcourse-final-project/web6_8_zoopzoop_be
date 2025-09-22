package org.tuna.zoopzoop.backend.domain.space.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    boolean existsByMemberAndSpace(Member member, Space space);

}
