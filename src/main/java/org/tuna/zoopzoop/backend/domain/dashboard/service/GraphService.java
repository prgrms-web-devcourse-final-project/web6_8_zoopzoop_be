package org.tuna.zoopzoop.backend.domain.dashboard.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.GraphRepository;

@Service
@RequiredArgsConstructor
public class GraphService {
    private final GraphRepository graphRepository;

    @Transactional
    public Graph saveGraph(Graph graph) {
        return graphRepository.save(graph);
    }

    public Graph getGraph(Integer id) {
        return graphRepository.findGraphById(id).orElseThrow(() ->
                new NoResultException(id + " id를 가진 그래프를 찾을 수 없습니다.")
        );
    }
}

