package org.tuna.zoopzoop.backend.domain.datasource.dto;

public record ArticleData(
        String title,
        String publishedAt,
        String content,
        String imgUrl,
        String rawHtml // GenericCrawler 용
) {
}
