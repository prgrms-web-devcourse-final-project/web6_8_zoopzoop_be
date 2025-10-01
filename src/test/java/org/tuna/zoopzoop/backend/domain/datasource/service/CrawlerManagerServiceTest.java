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
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    }

    @Test
    void NaverNewsCrawlerTest() throws IOException {
        // given
        String url = "https://n.news.naver.com/mnews/article/008/0005254080";

        // Mock HTML로 Document 생성 (네트워크 호출 없음)
        String mockHtml = """
            <!doctype html>
            <html lang="ko">
            <head>
              <meta charset="utf-8">
              <meta property="og:title" content="테스트 기사 제목 - Mock">
              <meta property="og:image" content="/assets/images/thumb.png">
              <meta name="twitter:creator" content="테스트">
              <title>테스트 페이지</title>
            </head>
            <body>
              <header>
                <h1>테스트 기사</h1>
                <!-- 작성일자: data-date-time 속성에 ISO-ish 형식 -->
                <span class="media_end_head_info_datestamp_time _ARTICLE_DATE_TIME"
                      data-date-time="2025-09-30 11:11:11">2025.09.30</span>
              </header>
            
              <article>
                <p>이것은 테스트용 본문 첫 문단입니다.</p>
              </article>
            
              <footer>
                <p>저작권: Mock News</p>
              </footer>
            </body>
            </html>
        """;
        Document doc = Jsoup.parse(mockHtml);  // 파싱만, 네트워크 호출 없음

        when(naverNewsCrawler.supports(url)).thenReturn(true);
        given(naverNewsCrawler.extract(any(Document.class))).willCallRealMethod();
        given(naverNewsCrawler.transLocalDate(any(String.class))).willCallRealMethod();

        // when
        CrawlerResult<?> result = crawlerManagerService.extractContent(url, doc);
        SpecificSiteDto naverDoc = (SpecificSiteDto) result.data();

        // then
        assertThat(result.type()).isEqualTo(CrawlerResult.CrawlerType.SPECIFIC);
        assertThat(naverDoc.title()).contains("테스트 기사 제목 - Mock");
        assertThat(naverDoc.content()).contains("이것은 테스트용 본문 첫 문단입니다.");
        assertThat(naverDoc.dataCreatedDate()).isEqualTo(LocalDate.of(2025, 9, 30));
        assertThat(naverDoc.imageUrl()).isEqualTo("/assets/images/thumb.png");
        assertThat(naverDoc.source()).isEqualTo("테스트");
    }

    @Test
    void NaverBlogCrawlerTest() throws IOException {
        // given
        String url = "https://blog.naver.com/mockpost";

        // iframe 내부 Mock HTML (실제 본문 데이터)
        String iframeHtml = """
            <!doctype html>
            <html lang="ko">
            <head>
              <meta property="og:title" content="Mock 블로그 제목">
              <meta property="og:image" content="https://mockcdn.com/thumb.png">
              <meta property="og:site_name" content="네이버 블로그 Mock">
            </head>
            <body>
              <span class="se_publishDate pcol2">2025.09.30. 11:30</span>
              <div class="se-main-container">
                <span>본문 첫 번째 문장.</span>
                <span>본문 두 번째 문장.</span>
              </div>
            </body>
            </html>
        """;
        Document iframeDoc = Jsoup.parse(iframeHtml, "https://blog.naver.com/iframe/mock");

        // 제목
        String title = iframeDoc.select("meta[property=og:title]").attr("content");

        // 작성일자
        String publishedAt = Optional.ofNullable(iframeDoc.selectFirst("span.se_publishDate.pcol2"))
                .map(Element::text)
                .orElse("");

        LocalDate dataCreatedDate = null;
        Pattern datePattern = Pattern.compile("(\\d{4})\\s*\\.\\s*(\\d{1,2})\\s*\\.\\s*(\\d{1,2})");
        Matcher dateMatcher = datePattern.matcher(publishedAt);
        if (dateMatcher.find()) {
            int year = Integer.parseInt(dateMatcher.group(1));
            int month = Integer.parseInt(dateMatcher.group(2));
            int day = Integer.parseInt(dateMatcher.group(3));
            dataCreatedDate = LocalDate.of(year, month, day);
        }

        // 내용
        Elements spans = iframeDoc.select(".se-main-container span");
        StringBuilder sb = new StringBuilder();
        for (Element span : spans) {
            sb.append(span.text()); // 태그 안 텍스트만
        }
        String content = sb.toString();

        // 썸네일 이미지 URL
        String imageUrl = iframeDoc.select("meta[property=og:image]").attr("content");

        // 출처
        String source = iframeDoc.select("meta[property=og:site_name]").attr("content");


        CrawlerResult<?> result = new CrawlerResult<>(
                CrawlerResult.CrawlerType.SPECIFIC,
                new SpecificSiteDto(
                        "Mock 블로그 제목",
                        LocalDate.of(2025, 9, 30),
                        "본문 첫 번째 문장.본문 두 번째 문장.",
                        "https://mockcdn.com/thumb.png",
                        "네이버 블로그 Mock"
                )
        );

        // when
        SpecificSiteDto dto = (SpecificSiteDto) result.data();

        // then
        assertThat(result.type()).isEqualTo(CrawlerResult.CrawlerType.SPECIFIC);
        assertThat(dto.title()).isEqualTo(title);
        assertThat(dto.content()).contains(content);
        assertThat(dto.dataCreatedDate()).isEqualTo(dataCreatedDate);
        assertThat(dto.imageUrl()).isEqualTo(imageUrl);
        assertThat(dto.source()).isEqualTo(source);
    }

    @Test
    void GenericCrawlerTest() throws IOException {
        // given
        String url = "https://blog.naver.com/rainbow-brain/223387331292"; // 원하는 URL 넣기

        String mockHtml = """
            <!doctype html>
            <html lang="ko">
            <head>
              <meta charset="utf-8">
              <meta property="og:image" content="/img/thumb.png">
              <style>body {color:red}</style>
            </head>
            <body class="main-body">
              <!-- 주석: 이건 제거되어야 함 -->
              <h1 id="title">테스트 제목</h1>
              <p style="font-size:14px" onclick="alert('x')">본문 <b>내용</b></p>
              <img src="/images/test.png" onload="hack()">
              <script>console.log("불필요한 스크립트");</script>
            </body>
            </html>
        """;

        Document doc = Jsoup.parse(mockHtml, "https://example.com");

        // img 태그
        doc.select("img[src]").forEach(el ->
                el.attr("src", el.absUrl("src"))
        );

        // meta 태그 (Open Graph, Twitter Card 등)
        doc.select("meta[content]").forEach(meta -> {
            String absUrl = meta.absUrl("content");
            if (!absUrl.isEmpty() && !absUrl.equals(meta.attr("content"))) {
                meta.attr("content", absUrl);
            }
        });

        String cleanHtml = doc.body().html()
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<style[^>]*>.*?</style>", "")
                // 주석 제거
                .replaceAll("<!--.*?-->", "")
                // 연속된 공백 제거
                .replaceAll("\\s+", " ")
                // 불필요한 속성 제거
                .replaceAll("(class|id|style|onclick|onload)=\"[^\"]*\"", "")
                .trim();

        when(genericCrawler.supports(url)).thenReturn(true);
        given(genericCrawler.extract(any(Document.class))).willCallRealMethod(); // 실제 메소드 실행

        // when
        CrawlerResult<?> result = crawlerManagerService.extractContent(url, doc);
        UnspecificSiteDto genericDoc = (UnspecificSiteDto) result.data();

        // then
        assertThat(result.type()).isEqualTo(CrawlerResult.CrawlerType.UNSPECIFIC);
        assertThat(genericDoc.rawHtml()).isEqualTo(cleanHtml);
    }
}
