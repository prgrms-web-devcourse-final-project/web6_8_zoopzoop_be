package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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
        String title = doc.select("meta[property=og:title]").attr("content");


        // 작성 날짜
        String publishedAt = Optional.ofNullable(
                        doc.selectFirst("span.media_end_head_info_datestamp_time._ARTICLE_DATE_TIME")
                )
                .map(el -> el.attr("data-date-time"))
                .orElse(""); // 값 없으면 빈 문자열

        LocalDate dataCreatedDate = publishedAt.isEmpty()
                ? null
                : transLocalDate(publishedAt);


        // 내용(ai한테 줘야함)
        String content = Optional.ofNullable(doc.selectFirst("article"))
                .map(Element::text)
                .orElse("");


        // 썸네일 이미지 url
        String imageUrl = doc.select("meta[property=og:image]").attr("content");


        // 출처
        String source = doc.select("meta[name=twitter:creator]").attr("content");

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
