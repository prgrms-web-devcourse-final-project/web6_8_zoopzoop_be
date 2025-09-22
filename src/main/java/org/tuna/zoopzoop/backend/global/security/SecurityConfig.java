package org.tuna.zoopzoop.backend.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomAuthenticationEntryPoint;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 모든 요청 허용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/member", "/api/v1/member/**").authenticated()
                        .requestMatchers(
                                "/",
                                "/favicon.ico",
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/oauth/**",
                                "/webjars/**",
                                "/api/v1/**" // API 테스트용으로 모두 허용. 차후 필수로 변경 필요.
                        ).permitAll()
                        .anyRequest().denyAll()
                )

                // CSRF 비활성화 (H2 콘솔 사용 위해 필요)
                .csrf(csrf -> csrf.disable())

                // H2 콘솔 사용 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // 기본 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                );
        return http.build();
    }
}