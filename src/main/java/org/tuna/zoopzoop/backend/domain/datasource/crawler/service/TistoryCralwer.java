package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
public class TistoryCralwer implements Crawler {
    private static final SupportedDomain DOMAIN = SupportedDomain.TISTORY;
    private static final DateTimeFormatter TISTORY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"); // 날짜 형식

    @Override
    public boolean supports(String domain) {
        return domain.contains(DOMAIN.getDomain());
    }

    @Override
    public CrawlerResult<?> extract(Document doc) {
        // 제목
        String title = doc.select("meta[property=og:title]").attr("content");


        // 작성일자
        String publishedAt = doc.selectFirst("meta[property=og:regDate]")
                .attr("content");

        LocalDate dataCreatedDate = publishedAt.isEmpty()
                ? null
                : transLocalDate(publishedAt);


        // 내용
        String content = Optional.ofNullable(doc.selectFirst("div.contents_style").text()).orElse("");


        // 썸네일 이미지 URL
        String imageUrl = doc.select("meta[property=og:image]").attr("content");

        // 접근 권한이 없는 이미지 url에 대해서 예외처리
        imageUrl = Optional.of(imageUrl)
                .filter(url -> {
                    try {
                        Jsoup.connect(url)
                                .ignoreContentType(true)
                                .timeout(3000)
                                .execute();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .orElse("");


        // 출처
        String source = "tistory | " + doc.select("meta[property=og:site_name]").attr("content");

        return new CrawlerResult<>(
                CrawlerResult.CrawlerType.SPECIFIC,
                new SpecificSiteDto(title, dataCreatedDate, content, imageUrl, source)
        );
    }

    @Override
    public LocalDate transLocalDate(String rawDate) {
        return LocalDate.parse(rawDate, TISTORY_FORMATTER);
    }
}
