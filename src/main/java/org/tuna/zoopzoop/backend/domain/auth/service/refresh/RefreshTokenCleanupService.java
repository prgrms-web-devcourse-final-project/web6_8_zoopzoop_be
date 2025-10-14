package org.tuna.zoopzoop.backend.domain.auth.service.refresh;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.auth.entity.RefreshToken;
import org.tuna.zoopzoop.backend.domain.auth.repository.RefreshTokenRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(fixedRate = 60 * 60 * 1000) // 1시간마다 실행
    public void deleteExpiredTokens() {
        List<RefreshToken> expiredTokens = refreshTokenRepository.findAllByExpiredAtBefore(LocalDateTime.now());
        if (!expiredTokens.isEmpty()) {
            refreshTokenRepository.deleteAll(expiredTokens);
        }
    }
}