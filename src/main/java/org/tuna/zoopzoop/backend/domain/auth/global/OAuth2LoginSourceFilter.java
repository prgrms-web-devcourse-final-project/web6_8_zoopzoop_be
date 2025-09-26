package org.tuna.zoopzoop.backend.domain.auth.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OAuth2LoginSourceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String source = request.getParameter("source");

        // OAuth2 로그인 시작 URL이면 세션에 저장
        if (uri.startsWith("/oauth2/authorization") && source != null) {
            request.getSession().setAttribute("loginSource", source);
        }

        filterChain.doFilter(request, response);
    }
}
