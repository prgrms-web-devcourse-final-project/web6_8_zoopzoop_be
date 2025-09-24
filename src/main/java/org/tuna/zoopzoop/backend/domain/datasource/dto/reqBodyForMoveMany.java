package org.tuna.zoopzoop.backend.domain.datasource.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record reqBodyForMoveMany(
        @NotNull Integer folderId,
        @NotEmpty List<Integer> dataSourceId
) {}
