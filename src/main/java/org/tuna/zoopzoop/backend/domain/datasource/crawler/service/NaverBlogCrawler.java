package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NaverBlogCrawler implements Crawler {
    private static final SupportedDomain DOMAIN = SupportedDomain.NAVERBLOG;
    private static final DateTimeFormatter NAVERBLOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy. M. d. HH:mm");

    @Override
    public boolean supports(String domain) {
        return domain.contains(DOMAIN.getDomain());
    }

    @Override
    public CrawlerResult<?> extract(Document doc) throws IOException {
        /*
            블로그 본문은 <iframe id="mainFrame"> 안에 로드되므로
            먼저 메인 페이지를 가져온 뒤 iframe의 src를 추출하여
            해당 URL로 다시 connect 해야 실제 본문 내용을 크롤링할 수 있다.
         */
        Element iframe = doc.selectFirst("iframe#mainFrame");
        String iframeUrl = iframe.absUrl("src");

        Document iframeDoc = Jsoup.connect(iframeUrl)
                .userAgent("Mozilla/5.0")  // 크롤링 차단 방지를 위해 user-agent 설정 권장
                .timeout(10 * 1000)        // 타임아웃 (10초)
                .get();

        // 제목
        Element titleSpans = iframeDoc.selectFirst("div.se-module.se-module-text.se-title-text");
        String title = titleSpans.text();

        // 작성일자
        String publishedAt = iframeDoc.selectFirst("span.se_publishDate.pcol2").text();
        LocalDateTime rawDate = LocalDateTime.parse(publishedAt, DateTimeFormatter.ofPattern("yyyy. M. d. HH:mm"));
        LocalDate dataCreatedDate = rawDate.toLocalDate();

        // 내용
        Elements spans = iframeDoc.select(".se-main-container span");
        StringBuilder sb = new StringBuilder();
        for (Element span : spans) {
            sb.append(span.text()); // 태그 안 텍스트만
        }
        String content = sb.toString();

        // 썸네일 이미지 URL
        Element img = iframeDoc.select("div.se-main-container img").first();

        String imageUrl = "";
        if (img != null) {
            if (!img.attr("data-lazy-src").isEmpty()) {
                imageUrl = img.attr("data-lazy-src");
            }
        }

        // 출처
        String source = "네이버 블로그";

        return new CrawlerResult<>(
                CrawlerResult.CrawlerType.SPECIFIC,
                new SpecificSiteDto(title, dataCreatedDate, content, imageUrl, source)
        );
    }

    @Override
    public LocalDate transLocalDate(String rawDate) {
        LocalDateTime dateTime = LocalDateTime.parse(rawDate, NAVERBLOG_FORMATTER);
        return dateTime.toLocalDate(); // 시간 버리고 날짜만
    }
}
