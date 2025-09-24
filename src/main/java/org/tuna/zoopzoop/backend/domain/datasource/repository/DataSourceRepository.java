package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;

import java.util.Collection;
import java.util.List;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Integer> {
    List<DataSource> findAllByFolder(Folder folder);

    @Query("select d.id from DataSource d where d.id in ?1")
    java.util.List<Integer> findExistingIds(Collection<Integer> ids);

    boolean existsByFolder_IdAndTitle(Integer folderId, String title);
}

