package org.tuna.zoopzoop.backend.domain.datasource.dto;

import java.util.List;

public record FolderFilesDto(
        Integer folderId,
        String folderName,
        List<FileSummary> files
) { }