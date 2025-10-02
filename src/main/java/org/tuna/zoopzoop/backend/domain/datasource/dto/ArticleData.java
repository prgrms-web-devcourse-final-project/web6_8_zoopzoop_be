package org.tuna.zoopzoop.backend.domain.datasource.dto;

import java.time.LocalDate;

public record ArticleData(
        String title, // 제목
        String content, // 내용
        LocalDate dataCreatedDate, // 작성일자
        String imageUrl, // 썸네일 이미지 url
        String source // 출처
) {
}
