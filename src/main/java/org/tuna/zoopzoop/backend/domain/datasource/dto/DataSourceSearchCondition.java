package org.tuna.zoopzoop.backend.domain.datasource.dto;

import lombok.Builder;
import lombok.Getter;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;

@Getter
@Builder
public class DataSourceSearchCondition {
    private final String title;
    private final String summary;
    private final Category category;
//    private final String Source;
    private final Integer folderId;
    private final String folderName;
    private final Boolean isActive;
    private final String keyword;

}
