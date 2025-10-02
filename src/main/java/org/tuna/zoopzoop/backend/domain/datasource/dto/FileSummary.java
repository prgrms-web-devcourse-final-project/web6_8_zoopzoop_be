package org.tuna.zoopzoop.backend.domain.datasource.dto;

import java.time.LocalDate;
import java.util.List;

public record FileSummary(
        Integer dataSourceId,
        String title,
        LocalDate createdAt,
        String summary,
        String sourceUrl,
        String imageUrl,
        List<String> tags,
        String category
) {}


