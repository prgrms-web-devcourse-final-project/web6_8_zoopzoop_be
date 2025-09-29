package org.tuna.zoopzoop.backend.domain.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.BodyForReactFlow;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.service.DashboardService;
import org.tuna.zoopzoop.backend.domain.dashboard.service.GraphService;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/dashboard")
@Tag(name = "ApiV1GraphController", description = "React-flow 데이터 컨트롤러")
public class ApiV1DashboardController {
    private final DashboardService dashboardService;

    /**
     * React-flow 데이터 저장(갱신) API
     * @param dashboardId React-flow 데이터의 dashboard 식별 id
     * @param requestBody React-flow 에서 보내주는 body 전체
     * @param signature Liveblocks-Signature 헤더 값
     * @return ResponseEntity<RsData<Void>>
     */
    @PutMapping("/{dashboardId}/graph")
    @Operation(summary = "React-flow 데이터 저장(갱신)")
    public ResponseEntity<RsData<Void>> updateGraph(
            @PathVariable Integer dashboardId,
            @RequestBody String requestBody,
            @RequestHeader("Liveblocks-Signature") String signature
    ) {
        dashboardService.verifyAndUpdateGraph(dashboardId, requestBody, signature);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>(
                        "200",
                        "React-flow 데이터를 저장했습니다.",
                        null
                ));
    }

    /**
     * React-flow 데이터 조회 API
     * @param dashboardId React-flow 데이터의 dashboard 식별 id
     */
    @GetMapping("/{dashboardId}/graph")
    @Operation(summary = "React-flow 데이터 조회")
    public ResponseEntity<RsData<BodyForReactFlow>> getGraph(
            @PathVariable Integer dashboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws AccessDeniedException {
        // TODO : 권한 체크 로직 추가
        Member member = userDetails.getMember();
        dashboardService.verifyAccessPermission(member, dashboardId);

        Graph graph = dashboardService.getGraphByDashboardId(dashboardId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new RsData<>(
                        "200",
                        "ID: " + dashboardId + " 의 React-flow 데이터를 조회했습니다.",
                        BodyForReactFlow.from(graph)
                ));

    }
}
