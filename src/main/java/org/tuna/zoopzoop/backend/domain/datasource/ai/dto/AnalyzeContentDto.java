package org.tuna.zoopzoop.backend.domain.datasource.ai.dto;

import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;

import java.util.List;

public record AnalyzeContentDto(
        String summary,
        Category category,     // ENUM 그대로 매핑 (AI 출력도 ENUM 이름으로)
        List<String> tags
) {
}
