package org.tuna.zoopzoop.backend.domain.datasource.crawler.dto;

public record CrawlerResult<T>(
        CrawlerType type, // SPECIFIC or UNSPECIFIC
        T data
) {
    public enum CrawlerType {
        SPECIFIC, UNSPECIFIC
    }
}
