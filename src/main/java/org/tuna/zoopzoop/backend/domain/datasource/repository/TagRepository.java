package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
}
