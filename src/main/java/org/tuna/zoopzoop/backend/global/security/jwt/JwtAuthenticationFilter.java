package org.tuna.zoopzoop.backend.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.tuna.zoopzoop.backend.global.security.service.CustomUserDetailsService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request); // Authorization 헤더에서 JWT 토큰 추출

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) { // 토큰이 존재하고 유효한 경우
            String kakaoKey = jwtUtil.getKakaoKeyFromToken(token); //토큰에서 카카오 키 값 추출

            UserDetails userDetails = userDetailsService.loadUserByUsername(kakaoKey); // 사용자 정보 로드

            //권한 생성 로직 X (사용 안함.)

            UsernamePasswordAuthenticationToken authentication = // Spring Security 인증 객체 생성
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            null
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext에 인증 정보 설정
        }
        filterChain.doFilter(request, response); // 다음 필터로 요청 전달
    }

    private String getTokenFromRequest(HttpServletRequest request) { // Authorization 헤더에서 Bearer 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
