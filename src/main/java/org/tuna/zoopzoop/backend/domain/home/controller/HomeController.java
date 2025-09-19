package org.tuna.zoopzoop.backend.domain.home.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

import static java.net.InetAddress.getLocalHost;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

@RestController
@Tag(name = "HomeController", description = "홈 컨트롤러")
public class HomeController {
    @Value("${kakao.client_id}")
    private String kakaoClientId;

    @Value("${kakao.redirect_uri}")
    private String kakaoRedirectUri;

    @SneakyThrows
    @GetMapping(produces = TEXT_HTML_VALUE)
    @Operation(summary = "메인 페이지")
    public String main() {
        InetAddress localHost = getLocalHost();

        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + kakaoClientId
                + "&redirect_uri=" + kakaoRedirectUri;

        return """
                <h1>API 서버</h1>
                <p>Host Name: %s</p>
                <p>Host Address: %s</p>
                <div>
                    <a href="/swagger-ui/index.html">API 문서로 이동</a>
                </div>
                <div>
                    <a href="%s">카카오 로그인 테스트</a>
                </div>
                """.formatted(localHost.getHostName(), localHost.getHostAddress(), kakaoLoginUrl);
    }
}