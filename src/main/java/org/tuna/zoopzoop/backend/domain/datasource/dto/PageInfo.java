package org.tuna.zoopzoop.backend.domain.datasource.dto;

public record PageInfo(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sorted
) {}