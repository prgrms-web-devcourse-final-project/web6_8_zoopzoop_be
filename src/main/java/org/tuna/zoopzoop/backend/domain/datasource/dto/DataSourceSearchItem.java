package org.tuna.zoopzoop.backend.domain.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class DataSourceSearchItem {
    private Integer dataSourceId;
    private String title;
    private LocalDate createdAt;
    private String summary;
    private String sourceUrl;
    private String imageUrl;
    private List<String> tags;
    private String category;
}
