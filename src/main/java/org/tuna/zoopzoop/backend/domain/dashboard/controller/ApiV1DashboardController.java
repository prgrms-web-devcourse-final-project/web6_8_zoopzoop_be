package org.tuna.zoopzoop.backend.domain.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.BodyForReactFlow;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.service.GraphService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/graph")
@Tag(name = "ApiV1GraphController", description = "React-flow 데이터 컨트롤러")
public class ApiV1DashboardController {
    private final GraphService graphService;

    /**
     * LiveBlocks를 위한 React-flow 데이터 저장 API
     * @param bodyForReactFlow React-flow 데이터를 가지고 있는 Dto
     */
    @PostMapping
    @Operation(summary = "React-flow 데이터 저장")
    public ResponseEntity<RsData<Void>> createGraph(
            @RequestBody BodyForReactFlow bodyForReactFlow
    ) {
        Graph graph = bodyForReactFlow.toEntity();
        graphService.saveGraph(graph);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>(
                        "200",
                        "React-flow 데이터를 저장 했습니다.",
                        null
                ));
    }

    /**
     * LiveBlocks를 위한 React-flow 데이터 조회 API
     * @param id React-flow 데이터의 graph 식별 id
     */
    @GetMapping("/{id}")
    @Operation(summary = "React-flow 데이터 조회")
    public ResponseEntity<RsData<BodyForReactFlow>> getGraph(
            @PathVariable Integer id
    ) {
        Graph graph = graphService.getGraph(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>(
                        "200",
                        "ID: " + id + " 의 React-flow 데이터를 조회했습니다.",
                        BodyForReactFlow.from(graph)
                ));
    }
}
