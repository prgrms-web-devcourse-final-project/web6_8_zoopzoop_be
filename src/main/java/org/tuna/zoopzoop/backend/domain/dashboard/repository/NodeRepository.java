package org.tuna.zoopzoop.backend.domain.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Node;

public interface NodeRepository extends JpaRepository<Node,Integer> {
}
