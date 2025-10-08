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
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.UnspecificSiteDto;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.CrawlerManagerService;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.GenericCrawler;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.NaverBlogCrawler;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.NaverNewsCrawler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class CrawlerManagerServiceTest {
    @InjectMocks
    private CrawlerManagerService crawlerManagerService; // List<Crawler>에 Mock들이 주입됨

    @Mock
    private NaverNewsCrawler naverNewsCrawler;

    @Mock
    private GenericCrawler genericCrawler;

    @Mock
    private NaverBlogCrawler naverBlogCrawler;

    @BeforeEach
    void setUp() {
        // 직접 리스트를 생성자에 주입
        crawlerManagerService = new CrawlerManagerService(
                List.of(naverNewsCrawler, naverBlogCrawler, genericCrawler)
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
    void NaverNewsCrawlerTest() throws IOException {
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
        CrawlerResult<?> result = crawlerManagerService.extractContent(url);
        SpecificSiteDto naverDoc = (SpecificSiteDto) result.data();

        // then
        assertThat(result.type()).isEqualTo(CrawlerResult.CrawlerType.SPECIFIC);
        assertThat(naverDoc.title()).isEqualTo(title);
        assertThat(naverDoc.content()).isEqualTo(content);
        assertThat(naverDoc.dataCreatedDate()).isEqualTo(dataCreatedDate);
        assertThat(naverDoc.imageUrl()).isEqualTo(imageUrl);
        assertThat(naverDoc.source()).isEqualTo(sources);
    }

    @Test
    void NaverBlogCrawlerTest() throws IOException {
        // given
        String url = "https://blog.naver.com/rainbow-brain/223387331292"; // 원하는 URL 넣기
//        String url = "https://blog.naver.com/smhrd_official/223078242394";

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")  // 크롤링 차단 방지를 위해 user-agent 설정 권장
                .timeout(10 * 1000)        // 타임아웃 (10초)
                .get();

        Element iframe = doc.selectFirst("iframe#mainFrame");
        String iframeUrl = iframe.absUrl("src");

        Document iframeDoc = Jsoup.connect(iframeUrl)
                .userAgent("Mozilla/5.0")  // 크롤링 차단 방지를 위해 user-agent 설정 권장
                .timeout(10 * 1000)        // 타임아웃 (10초)
                .get();

        // 제목
        Element titleSpans = iframeDoc.selectFirst("div.se-module.se-module-text.se-title-text");
        String title = titleSpans.text();
        System.out.println(title);

        // 작성일자
        String publishedAt = iframeDoc.selectFirst("span.se_publishDate.pcol2").text();
        LocalDateTime rawDate = LocalDateTime.parse(publishedAt, DateTimeFormatter.ofPattern("yyyy. M. d. HH:mm"));
        LocalDate dataCreatedDate = rawDate.toLocalDate();
        System.out.println(dataCreatedDate);

        // 내용
        Elements spans = iframeDoc.select(".se-main-container span");
        StringBuilder sb = new StringBuilder();
        for (Element span : spans) {
            sb.append(span.text()); // 태그 안 텍스트만
        }
        String content = sb.toString();
        System.out.println(content);

        // 썸네일 이미지 URL
        Element img = iframeDoc.select("div.se-main-container img").first();

        String imageUrl = "";
        if (img != null) {
            if (!img.attr("data-lazy-src").isEmpty()) {
                imageUrl = img.attr("data-lazy-src");
            }
        }
        System.out.println(imageUrl);

        // 출처
        String source = "네이버 블로그";


        when(naverBlogCrawler.supports(url)).thenReturn(true);
        given(naverBlogCrawler.extract(any(Document.class))).willCallRealMethod(); // 실제 메소드 실행
//        given(naverBlogCrawler.transLocalDate(any(String.class))).willCallRealMethod();

        // when
        CrawlerResult<?> result = crawlerManagerService.extractContent(url);
        SpecificSiteDto naverDoc = (SpecificSiteDto) result.data();

        // then
        assertThat(result.type()).isEqualTo(CrawlerResult.CrawlerType.SPECIFIC);
        assertThat(naverDoc.title()).isEqualTo(title);
        assertThat(naverDoc.content()).isEqualTo(content);
        assertThat(naverDoc.dataCreatedDate()).isEqualTo(dataCreatedDate);
        assertThat(naverDoc.imageUrl()).isEqualTo(imageUrl);
        assertThat(naverDoc.source()).isEqualTo(source);
    }

    @Test
    void GenericCrawlerTest() throws IOException {
        // given
        String url = "https://blog.naver.com/rainbow-brain/223387331292"; // 원하는 URL 넣기

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")  // 크롤링 차단 방지를 위해 user-agent 설정 권장
                .timeout(10 * 1000)        // 타임아웃 (10초)
                .get();

        doc.select("script, style, noscript, meta, link").remove();

        String cleanHtml = doc.body().html();

        when(genericCrawler.supports(url)).thenReturn(true);
        given(genericCrawler.extract(any(Document.class))).willCallRealMethod(); // 실제 메소드 실행

        // when
        CrawlerResult<?> result = crawlerManagerService.extractContent(url);
        UnspecificSiteDto genericDoc = (UnspecificSiteDto) result.data();

        // then
        assertThat(result.type()).isEqualTo(CrawlerResult.CrawlerType.UNSPECIFIC);
//        assertThat(genericDoc.rawHtml()).contains("<html");
        assertThat(genericDoc.rawHtml()).isEqualTo(cleanHtml);
    }
}
