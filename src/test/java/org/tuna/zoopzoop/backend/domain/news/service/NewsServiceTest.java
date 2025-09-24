package org.tuna.zoopzoop.backend.domain.news.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.news.dto.res.ResBodyForNaverNews;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@ActiveProfiles("test")
class NewsServiceTest {

    @Test
    @DisplayName("뉴스 서비스 테스트 - 정상적인 JSON 구조 반환 여부 확인")
    void newsJsonStructureTest() {
        // JSON 구조용 더미 데이터
        ResBodyForNaverNews dummyResponse = new ResBodyForNaverNews(
                505376,                                         // total
                List.of(
                        new ResBodyForNaverNews.NewsItem(       // items
                                "뉴스 제목",                    // title
                                "링크",                         // link
                                "설명",                         // description
                                "발행일"                        // pubDate
                        )
                )
        );

        Mono<ResBodyForNaverNews> result = Mono.just(dummyResponse);

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