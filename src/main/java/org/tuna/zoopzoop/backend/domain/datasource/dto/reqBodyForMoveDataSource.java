package org.tuna.zoopzoop.backend.domain.datasource.dto;

import jakarta.validation.constraints.NotNull;

public record reqBodyForMoveDataSource(
        @NotNull Integer folderId
) {}
