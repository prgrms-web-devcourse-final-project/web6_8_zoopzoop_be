package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;

import java.util.List;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Integer> {
    List<DataSource> findAllByFolder(Folder folder);
}

