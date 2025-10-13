package org.tuna.zoopzoop.backend.domain.datasource.dto;

import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;

import java.time.LocalDate;
import java.util.List;

public record DataSourceDto(
        String title, // 제목
        String summary, // 요약내용
        LocalDate dataCreatedDate, // 작성일자
        String sourceUrl, // 소스 데이터 URL
        String imageUrl, // 썸네일 이미지 url
        String source, // 출처
        Category category, // 대분류 카테고리
        List<String> tags // 태그 목록
) {
}
