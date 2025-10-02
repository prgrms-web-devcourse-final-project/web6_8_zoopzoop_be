package org.tuna.zoopzoop.backend.domain.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;

import java.util.Optional;

public interface GraphRepository extends JpaRepository<Graph,Integer> {
    Optional<Graph> findGraphById(Integer id);
}
