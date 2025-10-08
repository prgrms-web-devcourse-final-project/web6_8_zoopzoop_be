package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrawlerManagerService {
    private final List<Crawler> crawlers;

    public CrawlerResult<?> extractContent(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();

        for (Crawler crawler : crawlers) {
            if (crawler.supports(url)) {
                return crawler.extract(doc);
            }
        }
        return null;
    }
}
