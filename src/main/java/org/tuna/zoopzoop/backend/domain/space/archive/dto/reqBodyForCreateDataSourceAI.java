package org.tuna.zoopzoop.backend.domain.space.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record reqBodyForCreateDataSourceAI(
        @NotBlank String title,
        @NotNull LocalDate createdAt,
        @NotBlank String sourceUrl,
        String imageUrl,
        Integer folderId // 0 이면 default
) {}
