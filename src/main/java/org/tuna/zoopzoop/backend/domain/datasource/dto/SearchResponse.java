package org.tuna.zoopzoop.backend.domain.datasource.dto;

import java.util.List;

public record SearchResponse<T>(
        List<T> items,
        PageInfo pageInfo
) {}
