package org.tuna.zoopzoop.backend.domain.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {
    @Value("${REDIRECT_DOMAIN}")
    private String redirect_domain;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // 프론트로 리다이렉트
        // 필요하면 쿼리 파라미터로 에러 정보 전달

        String source = request.getParameter("source");

        if("extension".equals(source)){
            String redirectUrl = redirect_domain + "/extension/callback "
                    + "?success=false"
                    + "&error=" + URLEncoder.encode(exception.getMessage(), "UTF-8");
            response.sendRedirect(redirectUrl);
            return;
        }

        String redirectUrl =
                redirect_domain + "/auth/callback"
                + "?success=false"
                + "&error=" + URLEncoder.encode(exception.getMessage(), "UTF-8");

        response.sendRedirect(redirectUrl);
    }
}
