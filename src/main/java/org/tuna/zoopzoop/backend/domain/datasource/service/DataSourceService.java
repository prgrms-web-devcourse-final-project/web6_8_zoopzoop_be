package org.tuna.zoopzoop.backend.domain.datasource.service;

import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;

import java.util.List;

public interface DataSourceService {
    int create(int folderId, CreateCmd cmd);
    MoveResult moveOne(int dataSourceId, int targetFolderId);
    void moveMany(List<Integer> dataSourceIds, int targetFolderId);
    int update(int dataSourceId, UpdateCmd cmd);
    int softDeleteMany(List<Integer> ids);
    int restoreMany(List<Integer> ids);
    void hardDeleteOne(int id);
    void hardDeleteMany(List<Integer> ids);
    Page<DataSourceSearchItem> searchInArchive(int archiveId, DataSourceSearchCondition cond, Pageable pageable);

    @Builder(toBuilder = true)
    record CreateCmd(
            String title,
            String summary,
            String sourceUrl,
            String imageUrl,
            String source,
            Category category,
            java.time.LocalDate dataCreatedDate,
            List<String> tags
    ) {}

    @Builder
    record UpdateCmd(
            JsonNullable<String> title,
            JsonNullable<String> summary,
            JsonNullable<String> sourceUrl,
            JsonNullable<String> imageUrl,
            JsonNullable<String> source,
            JsonNullable<List<String>> tags,
            JsonNullable<String> category
    ) {}

    record MoveResult(Integer dataSourceId, Integer folderId) {}
}

