package org.tuna.zoopzoop.backend.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
//    Optional<Member> findByEmail(String email);
    Optional<Member> findByName(String name);
    Optional<Member> findByProviderAndProviderKey(Provider provider, String providerKey);
    Optional<Member> findByProviderKey(String providerKey);
    List<Member> findByActiveTrue(); // 활성 사용자 조회
    List<Member> findByActiveFalse(); // 비활성 사용자 조회
}
