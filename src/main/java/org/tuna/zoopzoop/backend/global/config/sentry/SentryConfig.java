package org.tuna.zoopzoop.backend.global.config.sentry;

import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback beforeSend() {
        return (event, hint) -> {
            if(event.getMessage() != null
                    && event.getMessage().getFormatted().contains("JWT 토큰")) {
                return null;
            }
            return event;
        };
    }
}
