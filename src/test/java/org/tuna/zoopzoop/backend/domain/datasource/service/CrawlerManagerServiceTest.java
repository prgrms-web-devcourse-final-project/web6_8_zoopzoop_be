package org.tuna.zoopzoop.backend.domain.datasource.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.CrawlerManagerService;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.GenericCrawler;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.NaverNewsCrawler;
import org.tuna.zoopzoop.backend.domain.datasource.dto.ArticleData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CrawlerManagerServiceTest {
    @InjectMocks
    private CrawlerManagerService crawlerManagerService; // List<Crawler>에 Mock들이 주입됨

    @Mock
    private NaverNewsCrawler naverNewsCrawler;

    @Mock
    private GenericCrawler genericCrawler;

    @BeforeEach
    void setUp() {
        // 직접 리스트를 생성자에 주입
        crawlerManagerService = new CrawlerManagerService(
                List.of(naverNewsCrawler, genericCrawler)
        );
    }

    @Test
    void FetchHtmlUrlTest() throws IOException {
        // given
        String url = "https://n.news.naver.com/mnews/article/008/0005254080"; // 원하는 URL 넣기

        // when
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")  // 크롤링 차단 방지를 위해 user-agent 설정 권장
                .timeout(10 * 1000)        // 타임아웃 (10초)
                .get();

        Elements articles = doc.select("article");

        System.out.println(doc.select("article").text());

        String html = doc.outerHtml(); // HTML 전체 소스

        if (!articles.isEmpty()) {
            for (Element article : articles) {
                // 3. 텍스트 추출
                String text = article.text();
                System.out.println("본문 내용:\n" + text);
            }
        } else {
            System.out.println("article 태그를 찾을 수 없습니다.");
        }

        // then
        System.out.println(articles);
        System.out.println(html);
        assertThat(html).contains("<html");
    }

    @Test
    void NaverCrawlerTest() throws IOException {
        // given
        String url = "https://n.news.naver.com/mnews/article/008/0005254080"; // 원하는 URL 넣기

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")  // 크롤링 차단 방지를 위해 user-agent 설정 권장
                .timeout(10 * 1000)        // 타임아웃 (10초)
                .get();

        // 제목
        String title = doc.selectFirst("h2#title_area").text();

        // 작성 날짜
        String publishedAt = doc.selectFirst(
                "span.media_end_head_info_datestamp_time._ARTICLE_DATE_TIME"
        ).attr("data-date-time");
        System.out.println(publishedAt);
        LocalDate dataCreatedDate = LocalDate.parse(publishedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(dataCreatedDate);

        // 내용
        String content = doc.select("article").text();

        // 썸네일 이미지 url
        String imageUrl = doc.selectFirst("img#img1._LAZY_LOADING._LAZY_LOADING_INIT_HIDE").attr("data-src");

        // 출처
        String sources = doc.selectFirst("span.media_end_head_top_logo_text").text();

        when(naverNewsCrawler.supports(url)).thenReturn(true);
        given(naverNewsCrawler.extract(any(Document.class))).willCallRealMethod(); // 실제 메소드 실행
        given(naverNewsCrawler.transLocalDate(any(String.class))).willCallRealMethod();

        // when
        ArticleData naverDoc = crawlerManagerService.extractContent(url);

        // then
        assertThat(naverDoc.title()).isEqualTo(title);
        assertThat(naverDoc.content()).isEqualTo(content);
        assertThat(naverDoc.dataCreatedDate()).isEqualTo(dataCreatedDate);
        assertThat(naverDoc.imageUrl()).isEqualTo(imageUrl);
        assertThat(naverDoc.sources()).isEqualTo(sources);
    }

    @Test
    void GenericCrawlerTest() throws IOException {
        // given
        String url = "https://www.slog.gg/p/14006"; // 원하는 URL 넣기

        when(genericCrawler.supports(url)).thenReturn(true);
        given(genericCrawler.extract(any(Document.class))).willCallRealMethod(); // 실제 메소드 실행

        // when
        String genericDoc = crawlerManagerService.extractContent(url).rawHtml();

        // then
        assertThat(genericDoc).contains("<html");
    }
}
