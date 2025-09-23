package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.dto.ArticleData;

@Component
@Order(Ordered.LOWEST_PRECEDENCE) // 모든 URL 대응 (우선순위 맨 뒤)
public class GenericCrawler implements Crawler {
    @Override
    public boolean supports(String url) {
        return true;
    }

    @Override
    public ArticleData extract(Document doc) {
        return new ArticleData(null, null, null, null, doc.outerHtml());
    }
}
