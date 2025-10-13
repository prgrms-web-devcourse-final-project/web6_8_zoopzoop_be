package org.tuna.zoopzoop.backend.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.auth.entity.RefreshToken;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findBySessionId(String sessionId);
    Optional<RefreshToken> findByMember(Member member);
    List<RefreshToken> findAllByMember(Member member);
    List<RefreshToken> findAllByExpiredAtBefore(LocalDateTime dateTime);
}
