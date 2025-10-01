package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class VelogCrawlerTest {

    private final VelogCrawler velogCrawler = new VelogCrawler();

    @Test
    void testExtract() throws IOException {
        Document doc = Jsoup.connect("https://velog.io/@imcute0703123/%EC%BA%A0%ED%8D%BC%EC%8A%A4%EC%8B%9C%EA%B7%B8%EB%84%90-2025-%EB%B0%B1%EC%84%9D%EC%97%B0%ED%99%94-1%EB%93%B1-%EB%B6%80%EC%8A%A4-%EB%9F%AC%EB%B8%8C%EB%9D%BC%EC%9D%B8-%EB%A7%A4%EC%B9%AD-%EC%84%9C%EB%B9%84%EC%8A%A4-%ED%9A%8C%EA%B3%A0%EB%A1%9D").get();
        CrawlerResult<?> result = velogCrawler.extract(doc);
        assertThat(result).isNotNull();

        System.out.println(result);
    }
}