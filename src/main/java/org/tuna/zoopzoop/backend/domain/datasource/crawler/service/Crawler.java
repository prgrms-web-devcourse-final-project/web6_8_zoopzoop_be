package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.tuna.zoopzoop.backend.domain.datasource.dto.ArticleData;

public interface Crawler {
    boolean supports(String domain);
    ArticleData extract(Document doc);
}
