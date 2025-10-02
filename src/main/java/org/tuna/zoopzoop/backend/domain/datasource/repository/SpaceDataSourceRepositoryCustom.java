package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpaceDataSourceRepositoryCustom {
    Optional<DataSource> findByIdAndArchiveId(Integer id, Integer archiveId);
    List<Integer> findExistingIdsInArchive(Integer archiveId, Collection<Integer> ids);
}
