package org.tuna.zoopzoop.backend.domain.datasource.dto;

import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;

public record reqBodyForUpdateDataSource(
        JsonNullable<String> title,
        JsonNullable<String> summary,
        JsonNullable<String> sourceUrl,
        JsonNullable<String> imageUrl,
        JsonNullable<String> source,
        JsonNullable<List<String>> tags,
        JsonNullable<String> category
) {}
