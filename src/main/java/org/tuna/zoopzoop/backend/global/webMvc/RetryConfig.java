package org.tuna.zoopzoop.backend.global.webMvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
    retry config 빈
 */
@Configuration
@EnableRetry
public class RetryConfig {
}