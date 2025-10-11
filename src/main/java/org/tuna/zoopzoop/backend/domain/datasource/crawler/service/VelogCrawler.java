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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VelogCrawler implements Crawler{
    private static final SupportedDomain DOMAIN = SupportedDomain.VELOG;
    private static final DateTimeFormatter VELOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    @Override
    public boolean supports(String domain) {
        return domain.contains(DOMAIN.getDomain());
    }

    @Override
    public CrawlerResult<SpecificSiteDto> extract(Document doc) {
        // 제목
        Element titleElement = doc.selectFirst("meta[property=og:title]");
        String title = titleElement != null ? titleElement.attr("content") : "";

        // 작성 날짜
        Element publishedAtElement = doc.selectFirst("div.information > span:not([class])");
        String publishedAt = publishedAtElement != null ? publishedAtElement.text() : "";

        LocalDate dataCreatedDate = publishedAt.isBlank() ? DEFAULT_DATE : transLocalDate(publishedAt) ;

        // 내용(ai한테 줘야함)
        Element contentElement = doc.selectFirst("div.atom-one");
        String content = contentElement != null ? contentElement.text() : "";

        // 썸네일 이미지 url
        Element imageUrlElement = doc.selectFirst("meta[property=og:image]");
        String imageUrl = imageUrlElement != null ? imageUrlElement.attr("content") : "";

        // 출처
        Element sourceElement = doc.selectFirst("span.username > a");
        String source = sourceElement != null ? sourceElement.text() : "";

        return new CrawlerResult<>(
                CrawlerResult.CrawlerType.SPECIFIC,
                new SpecificSiteDto(title, dataCreatedDate, content, imageUrl, source)
        );
    }

    @Override
    public LocalDate transLocalDate(String rawDate) {

        if(rawDate.contains("일 전")){
            int daysAgo = Integer.parseInt(rawDate.split("일 전")[0].trim());
            return LocalDate.now().minusDays(daysAgo);
        }else if(rawDate.contains("시간 전")||rawDate.contains("방금 전")||rawDate.contains("분 전")){
            return LocalDate.now();
        }else if (rawDate.contains("어제")){
            return LocalDate.now().minusDays(1);
        }

        return LocalDate.parse(rawDate, VELOG_FORMATTER);
    }
}
