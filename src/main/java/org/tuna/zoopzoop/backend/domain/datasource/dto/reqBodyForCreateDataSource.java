package org.tuna.zoopzoop.backend.domain.datasource.dto;
import jakarta.validation.constraints.NotBlank;

public record reqBodyForCreateDataSource(
    @NotBlank String sourceUrl,
    Integer folderId            // null 일 경우 default 폴더(최상위 폴더)
) {}
