package org.tuna.zoopzoop.backend.domain.news.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.tuna.zoopzoop.backend.domain.news.dto.res.ResBodyForNaverNews;
import reactor.core.publisher.Flux;
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
     * @param sort 정렬 방식 ("sim", "date") sim: 정확도 순, date: 날짜 순, 둘다 내림차 순 정렬.
     */

    /*
    Q. 어째서 WebClient를 사용하는가?
    A. WebClient -> 비동기/논블로킹 HTTP 클라이언트.
       즉, 현재 우리 시스템처럼 여러명의 사용자가 뉴스 API를 통한 검색을 요청할 경우,
       기존의 Spring MVC, RestTemplate를 사용하면 블로킹된 쓰레드가 바생하여 서버의 리소스를 효율적으로 사용하지 못함.

       추가로, WebFlux의 Mono/Flux의 경우엔 Backpressure를 지원하므로,
       데이터가 너무 많이 들어올 경우 서버가 감당 가능하도록 흐름을 조절.
       *Backpressure(백프레셔) : 수신자가 처리할 수 있는 속도로 발신자가 데이터를 보내도록 하는 것.

    Q. 중복 뉴스나 불필요한 데이터는?
    A. filter()를 통해 네이버 뉴스 본문 링크만 걸러내고,
       distinct()를 적용하여 동일한 링크를 가진 뉴스는 제거.
       → API가 중복된 데이터를 반환해도, 최종적으로는 고유한 뉴스만 내려줌.

       n.news.naver.com 도메인의 아닌 경우, 아직 크롤링 기능을 지원할 지 미정인 상태이므로,
       마찬가지로 예외 처리한다. (크롤링하지 않으면 요약 기능을 사용할 수 없기 때문.)

    Q. display=10 으로 요청했는데, 링크 필터링으로 인해 결과가 10개보다 작을 경우는?
    A. 한 페이지만 요청할 경우, 필터링과 중복 제거 과정에서 일부 데이터가 빠져 결과의 개수가 적을 수 있다.

       그래서 현재 로직
       1. 여러 페이지를 호출한다.(.range(0, 1000 / finalDisplay), 최대 1000건 까지.)
       2. 필터링 & 중복 제거
       3. display 값 만큼 최종 개수 제한(.take(display))
       4. 그렇게 추출한 데이터를 collectList()로 모은 후, 다시 ResBodyForNaverNews로 감싸기.

       위 과정을 거쳐도 개수가 부족한 경우엔 부족한 만큼 내보낸다.
    */
    public Mono<ResBodyForNaverNews> searchNews(String query, String sort) {
        int finalDisplay = 100;
        String finalSort = (sort == null || (!sort.equals("sim") && !sort.equals("date"))) ? "sim" : sort;

        return Flux
                .range(0, 10000 / finalDisplay)
                .concatMap(page -> webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("v1/search/news.json")
                                .queryParam("query", query)
                                .queryParam("display", finalDisplay)
                                .queryParam("start", page * finalDisplay + 1)
                                .queryParam("sort", finalSort)
                                .build())
                        .header("X-Naver-Client-Id", client_id)
                        .header("X-Naver-Client-Secret", client_secret)
                        .retrieve()
                        .bodyToMono(ResBodyForNaverNews.class)
                        .flatMapMany(res -> Flux.fromIterable(res.items()))
                        .filter(item -> item.link().startsWith("https://n.news.naver.com/"))
                )
                .distinct(ResBodyForNaverNews.NewsItem::link)
                .take(100)
                .collectList()
                .map(items -> new ResBodyForNaverNews(
                        items.size(),
                        items
                ));
    }
}
