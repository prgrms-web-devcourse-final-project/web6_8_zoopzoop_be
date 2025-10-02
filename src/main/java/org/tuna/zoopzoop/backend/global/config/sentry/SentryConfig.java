package org.tuna.zoopzoop.backend.global.config.sentry;

import io.sentry.SentryOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SentryConfig {
    private final ProfileChecker profileChecker;

    @Bean
    public SentryOptions.BeforeSendCallback beforeSend() {
        return (event, hint) -> {
            log.info("[Sentry] 현재 프로필: {}", profileChecker.getActiveProfiles()[0]);
            if("test".equals(profileChecker.getActiveProfiles()[0])) return null;
            log.info("[Sentry] 정상 통과됨.");
            if(event.getMessage() != null
                    && event.getMessage().getFormatted().contains("JWT 토큰")) {
                return null;
            }
            return event;
        };
    }
}