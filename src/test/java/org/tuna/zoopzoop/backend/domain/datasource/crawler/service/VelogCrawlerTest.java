package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class VelogCrawlerTest {

    private final VelogCrawler velogCrawler = new VelogCrawler();

    @Test
    void testExtract() throws IOException {
        Document doc = Jsoup.connect("https://velog.io/@hyeonnnnn/VampireSurvivorsClone-04.-PoolManager").get();
        CrawlerResult<?> result = velogCrawler.extract(doc);
        assertThat(result).isNotNull();

        System.out.println(result);
    }
}