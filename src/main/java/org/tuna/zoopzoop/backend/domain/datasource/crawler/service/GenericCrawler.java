package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.UnspecificSiteDto;

import java.time.LocalDate;

@Component
@Order(Ordered.LOWEST_PRECEDENCE) // 모든 URL 대응 (우선순위 맨 뒤)
public class GenericCrawler implements Crawler {
    @Override
    public boolean supports(String url) {
        return true;
    }

    @Override
    public CrawlerResult<?> extract(Document doc) {
        // 불필요한 태그 제거
        doc.select("script, style, noscript, iframe, nav, header, footer, form, aside, meta, link").remove();

        // 본문만 가져오기 (HTML)
        String cleanHtml = doc.body().html();

        return new CrawlerResult<>(
                CrawlerResult.CrawlerType.UNSPECIFIC,
                new UnspecificSiteDto(cleanHtml)
        );
    }

    @Override
    public LocalDate transLocalDate(String rawDate) {
        return null;
    }
}
