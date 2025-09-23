package org.tuna.zoopzoop.backend.domain.news.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.tuna.zoopzoop.backend.domain.news.dto.res.ResBodyForNaverNews;
import reactor.core.publisher.Mono;

@Service
public class NewsSearchService {
    private final WebClient webClient;

    @Value("${naver.client_id}")
    private String client_id;

    @Value("${naver.client_secret}")
    private String client_secret;

    public NewsSearchService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.naver.com").build();
    }

    /**
     * 네이버 뉴스 API
     * @param query 검색어 (UTF-8로 인코딩 필수)
     * @param display 한 번에 표시할 결과 수 (기본 값 10, 최대 100)
     * @param start 검색 시작 위치 (기본값 1, 최대 1000)
     * @param sort 정렬 방식 ("sim", "date") sim: 정확도 순, date: 날짜 순, 둘다 내림차 순 정렬.
     */

    /*
    Q. 어째서 WebClient를 사용하는가?
    A. WebClient -> 비동기/논블로킹 HTTP 클라이언트.
    즉, 현재 우리 시스템처럼 여러명의 사용자가 뉴스 API를 통한 검색을 요청할 경우,
    기존의 Spring MVC, RestTemplate를 사용하면 블로킹된 쓰레드가 바생하여 서버의 리소스를 효율적으로 사용하지 못함.

    추가로, WebFlux의 Mono/Flux의 경우엔 Backpressure를 지원하므로, 데이터가 너무 많이 들어올 경우 서버가 감당 가능하도록 흐름을 조절.
    *Backpressure(백프레셔) : 수신자가 처리할 수 있는 속도로 발신자가 데이터를 보내도록 하는 것.
    */

    public Mono<ResBodyForNaverNews> searchNews(String query, Integer display, Integer start, String sort) {
        int finalDisplay = (display == null) ? 10 : Math.min(display, 100);
        int finalStart = (start == null) ? 1 : Math.min(start, 1000);
        String finalSort = (sort == null || (!sort.equals("sim") && !sort.equals("date"))) ? "sim" : sort;

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", finalDisplay)
                        .queryParam("start", finalStart)
                        .queryParam("sort", finalSort)
                        .build())
                .header("X-Naver-Client-Id", client_id)
                .header("X-Naver-Client-Secret", client_secret)
                .retrieve()
                .bodyToMono(ResBodyForNaverNews.class);
    }
}
