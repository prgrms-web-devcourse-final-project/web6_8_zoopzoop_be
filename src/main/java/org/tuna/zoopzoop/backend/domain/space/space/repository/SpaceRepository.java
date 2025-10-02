package org.tuna.zoopzoop.backend.domain.space.space.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

import java.util.Optional;


public interface SpaceRepository extends JpaRepository<Space, Integer> {
    Optional<Space> findByName(String name);
}
