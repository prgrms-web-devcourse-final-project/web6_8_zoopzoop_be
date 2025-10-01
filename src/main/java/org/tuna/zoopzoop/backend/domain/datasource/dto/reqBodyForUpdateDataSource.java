package org.tuna.zoopzoop.backend.domain.datasource.dto;

import java.util.List;

public record reqBodyForUpdateDataSource(
        String title,
        String summary,
        String sourceUrl,
        String imageUrl,
        String source,
        List<String> tags,
        String category
) {}
