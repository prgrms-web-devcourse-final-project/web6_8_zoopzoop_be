package org.tuna.zoopzoop.backend.domain.archive.folder.dto;

import jakarta.validation.constraints.NotBlank;

public record reqBodyForCreateFolder(
        @NotBlank String folderName
) {}
