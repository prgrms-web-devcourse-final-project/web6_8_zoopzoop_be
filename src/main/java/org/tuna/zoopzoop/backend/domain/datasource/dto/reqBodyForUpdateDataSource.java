package org.tuna.zoopzoop.backend.domain.datasource.dto;

import org.openapitools.jackson.nullable.JsonNullable;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;

import java.util.List;

public record reqBodyForUpdateDataSource(
        JsonNullable<String> title,
        JsonNullable<String> summary,
        JsonNullable<String> sourceUrl,
        JsonNullable<String> imageUrl,
        JsonNullable<String> source,
        JsonNullable<List<String>> tags,
        JsonNullable<Category> category
) {}
