package org.tuna.zoopzoop.backend.domain.datasource.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DataSourceSearchCondition {
    private final String title;
    private final String summary;
    private final String category;
    private final String folderName;
    private final Boolean isActive;
}
