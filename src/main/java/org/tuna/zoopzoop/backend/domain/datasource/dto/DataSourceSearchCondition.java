package org.tuna.zoopzoop.backend.domain.datasource.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DataSourceSearchCondition {
    private final String title;
    private final String summary;
    private final LocalDate createdAtAfter;
    private final String folderName;
    private final String category;
}
