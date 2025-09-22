package org.tuna.zoopzoop.backend.global.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    //application.yml에 jwt 항목 작성
    //예시
    //jwt:
    //  secret-key: mySecretKeyForJWTTokenGenerationAndValidation1234567890
    //  access-token-validity: 86400000
    //  refresh-token-validity: 604800000
    private String secretKey;
    private long accessTokenValidity;
    private long refreshTokenValidity;
}