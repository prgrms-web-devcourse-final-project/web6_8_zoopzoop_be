package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    @Query("select t.tagName from Tag t")
    List<String> findAllTagNames();
}
