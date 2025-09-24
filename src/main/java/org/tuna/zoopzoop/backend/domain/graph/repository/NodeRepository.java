package org.tuna.zoopzoop.backend.domain.graph.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.graph.entity.Node;

public interface NodeRepository extends JpaRepository<Node,Integer> {
}
