package org.tuna.zoopzoop.backend.domain.auth.service.refresh;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.auth.entity.RefreshToken;
import org.tuna.zoopzoop.backend.domain.auth.repository.RefreshTokenRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    private LocalDateTime getExpirationLocalDateTimeFromToken(String token) {
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token); // 기존 메서드
        if (expirationDate == null) return null;

        return LocalDateTime.ofInstant(expirationDate.toInstant(), ZoneId.systemDefault());
    }

    public String saveSession(Member member, String refreshToken) {
        String sessionId = UUID.randomUUID().toString();

        refreshTokenRepository.findByMember(member).ifPresent(refreshTokenRepository::delete);

        RefreshToken token = RefreshToken.builder()
                .member(member)
                .refreshToken(refreshToken)
                .sessionId(sessionId)
                .expiredAt(getExpirationLocalDateTimeFromToken(refreshToken))
                .build();

        refreshTokenRepository.save(token);
        return sessionId;
    }

    public RefreshToken getBySessionId(String sessionId) {
        return refreshTokenRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BadCredentialsException("세션을 찾을 수 없습니다."));
    }

    public void deleteBySessionId(String sessionId) {
        RefreshToken token = refreshTokenRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BadCredentialsException("잘못된 요청입니다."));
        refreshTokenRepository.delete(token);
    }

    public void deleteByMember(Member member) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByMember(member);
        if (!tokens.isEmpty()) {
            refreshTokenRepository.deleteAll(tokens);
        }
    }
}
