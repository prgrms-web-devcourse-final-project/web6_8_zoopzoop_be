package org.tuna.zoopzoop.backend.domain.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Edge;

public interface EdgeRepository extends JpaRepository<Edge,Integer> {
}
