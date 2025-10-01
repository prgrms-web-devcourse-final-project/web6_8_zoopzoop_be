package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class VelogCrawler implements Crawler{
    private static final SupportedDomain DOMAIN = SupportedDomain.VELOG;
    private static final DateTimeFormatter VELOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    @Override
    public boolean supports(String domain) {
        return domain.contains(DOMAIN.getDomain());
    }

    @Override
    public CrawlerResult<?> extract(Document doc) {
        // 제목
        String title = doc.selectFirst("meta[property=og:title]").attr("content");

        // 작성 날짜
        String publishedAt = doc.selectFirst(
                "div.information > span:not([class])"
        ).text();

        LocalDate dataCreatedDate = transLocalDate(publishedAt);

        // 내용(ai한테 줘야함)
        String content = doc.selectFirst("div.atom-one").text();

        // 썸네일 이미지 url
        String imageUrl = doc.selectFirst("meta[property=og:image]").attr("content");

        // 출처
        String source = doc.selectFirst("span.username > a").text();

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
