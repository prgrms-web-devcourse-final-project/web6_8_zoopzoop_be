package org.tuna.zoopzoop.backend.domain.space.space.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.space.space.repository.SpaceRepository;

@Service
@RequiredArgsConstructor
public class SpaceService {
    private final SpaceRepository spaceRepository;
}
