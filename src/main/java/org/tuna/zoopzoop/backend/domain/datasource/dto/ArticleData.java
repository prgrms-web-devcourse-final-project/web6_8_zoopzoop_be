package org.tuna.zoopzoop.backend.domain.datasource.dto;

import java.time.LocalDate;

public record ArticleData(
        String title, // 제목
        LocalDate dataCreatedDate, // 작성일자
        String content, // ai한테 줘야할 내용
        String imageUrl, // 이미지 url
        String sources, // 출처
        String rawHtml // GenericCrawler 용
) {
}
