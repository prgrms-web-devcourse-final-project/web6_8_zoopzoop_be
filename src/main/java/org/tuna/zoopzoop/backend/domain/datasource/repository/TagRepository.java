package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    @Query("""
        select distinct t.tagName
        from Tag t
        where t.dataSource.folder.id = :folderId
    """)
    List<String> findDistinctTagNamesByFolderId(@Param("folderId") Integer folderId);
}
