package org.tuna.zoopzoop.backend.domain.news.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.news.dto.res.ResBodyForNaverNews;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class NewsAPIServiceTest {
    @Autowired
    private NewsAPIService newsSearchService;

    @Test
    @DisplayName("뉴스 서비스 테스트 - 정상적인 JSON 구조 반환 여부 확인")
    void newsJsonStructureTest() {
        Mono<ResBodyForNaverNews> result = newsSearchService.searchNews("뉴스", "sim");

        // JSON 구조 확인
        result.doOnNext(res -> {
            assertNotNull(res.total());
            assertNotNull(res.items());

            res.items().forEach(item -> {
                assertNotNull(item.title());
                assertNotNull(item.link());
                assertNotNull(item.description());
                assertNotNull(item.pubDate());
            });
        }).block(); // Mono 블로킹.
    }
}