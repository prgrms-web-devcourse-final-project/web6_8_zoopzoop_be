package org.tuna.zoopzoop.backend.domain.datasource.dto;

import jakarta.validation.constraints.NotNull;

public record reqBodyForUpdateDataSource(
        @NotNull String title,
        @NotNull String summary
) {}
