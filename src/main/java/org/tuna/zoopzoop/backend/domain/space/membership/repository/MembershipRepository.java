package org.tuna.zoopzoop.backend.domain.space.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;

public interface MembershipRepository extends JpaRepository<Membership, Integer> {
}
