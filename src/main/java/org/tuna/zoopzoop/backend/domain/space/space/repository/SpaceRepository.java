package org.tuna.zoopzoop.backend.domain.space.space.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

public interface SpaceRepository extends JpaRepository<Space, Integer> {
}
