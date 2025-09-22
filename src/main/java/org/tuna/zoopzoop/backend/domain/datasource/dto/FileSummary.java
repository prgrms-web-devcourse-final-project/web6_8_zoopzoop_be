package org.tuna.zoopzoop.backend.domain.datasource.dto;

import java.time.LocalDateTime;

public record FileSummary(
        Integer fileId,
        String fileName,
        LocalDateTime createdAt
) { }
