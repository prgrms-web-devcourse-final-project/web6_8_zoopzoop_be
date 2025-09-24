package org.tuna.zoopzoop.backend.domain.graph.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.graph.entity.Edge;

public interface EdgeRepository extends JpaRepository<Edge,Integer> {
}
