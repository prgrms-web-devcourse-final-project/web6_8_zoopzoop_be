package org.tuna.zoopzoop.backend.domain.datasource.dto;

import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;

import java.time.LocalDateTime;
import java.util.List;

public record FileSummary(
        Integer dataSourceId,
        String title,
        LocalDateTime createdAt,
        String summary,
        String sourceUrl,
        String imageUrl,
        List<Tag> tags
) {}
