package org.tuna.zoopzoop.backend.domain.home.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.news.service.NewsSearchService;

import java.net.InetAddress;

import static java.net.InetAddress.getLocalHost;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;

@RestController
@RequiredArgsConstructor
@Tag(name = "HomeController", description = "홈 컨트롤러")
public class HomeController {
//    @Value("${kakao.client_id}")
//    private String kakaoClientId;
//
//    @Value("${kakao.redirect_uri}")
//    private String kakaoRedirectUri;
    private final NewsSearchService newsSearchService;

    @SneakyThrows
    @GetMapping(produces = TEXT_HTML_VALUE)
    @Operation(summary = "메인 페이지")
    public String main() {
        InetAddress localHost = getLocalHost();

        String kakaoLoginUrl = "http://localhost:8080/oauth2/authorization/kakao";
        String googleLoginUrl = "http://localhost:8080/oauth2/authorization/google";
        String logoutUrl = "http://localhost:8080/api/v1/auth/logout";

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
                <div>
                    <a href="%s">구글 로그인 테스트</a>
                </div>
                <div>
                    <a href="%s">로그아웃 테스트</a>
                </div>
                
                <h2>뉴스 검색 테스트</h2>
                <form action="/search-news" method="get">
                    <input type="text" name="query" placeholder="검색어 입력"/>
                    <input type="submit" value="검색"/>
                </form>
                """.formatted(localHost.getHostName(), localHost.getHostAddress(), kakaoLoginUrl, googleLoginUrl, logoutUrl);
    }
}