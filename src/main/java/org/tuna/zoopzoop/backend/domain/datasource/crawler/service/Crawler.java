package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;

import java.time.LocalDate;

public interface Crawler {
    boolean supports(String domain);
    CrawlerResult<?> extract(Document doc);
    LocalDate transLocalDate(String rawDate);
}
