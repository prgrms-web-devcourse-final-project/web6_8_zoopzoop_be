package org.tuna.zoopzoop.backend.domain.datasource.ai.dto;

import java.time.LocalDate;

public record AiExtractorDto(
        String title, // 제목
        LocalDate dataCreatedDate, // 작성일자
        String content, // ai한테 줘야할 내용
        String imageUrl, // 썸네일 이미지 url
        String source // 출처
) {
}
