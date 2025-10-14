package org.tuna.zoopzoop.backend.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {
}
