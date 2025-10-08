package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NaverNewsCrawler implements Crawler {
    private static final SupportedDomain DOMAIN = SupportedDomain.NAVERNEWS;
    private static final DateTimeFormatter NAVERNEWS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // 날짜 형식

    @Override
    public boolean supports(String domain) {
        return domain.contains(DOMAIN.getDomain());
    }

    @Override
    public CrawlerResult<?> extract(Document doc) {
        // 제목
        String title = doc.selectFirst("h2#title_area").text();

        // 작성 날짜
        String publishedAt = doc.selectFirst(
                "span.media_end_head_info_datestamp_time._ARTICLE_DATE_TIME"
        ).attr("data-date-time");
        LocalDate dataCreatedDate = transLocalDate(publishedAt);

        // 내용(ai한테 줘야함)
        String content = doc.select("article").text();

        // 썸네일 이미지 url
        String imageUrl = doc.selectFirst("img#img1._LAZY_LOADING._LAZY_LOADING_INIT_HIDE").attr("data-src");

        // 출처
        String source = doc.selectFirst("span.media_end_head_top_logo_text").text();

        return new CrawlerResult<>(
                CrawlerResult.CrawlerType.SPECIFIC,
                new SpecificSiteDto(title, dataCreatedDate, content, imageUrl, source)
        );
    }

    @Override
    public LocalDate transLocalDate(String rawDate) {
        return LocalDate.parse(rawDate, NAVERNEWS_FORMATTER);
    }
}
