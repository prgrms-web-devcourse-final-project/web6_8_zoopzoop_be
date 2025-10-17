package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;

public interface DataSourceQRepository {
    Page<DataSourceSearchItem> search(Integer memberId, DataSourceSearchCondition cond, Pageable pageable);
    Page<DataSourceSearchItem> searchInArchive(Integer archiveId, DataSourceSearchCondition cond, Pageable pageable);
}