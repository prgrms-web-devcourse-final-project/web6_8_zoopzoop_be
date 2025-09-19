package org.tuna.zoopzoop.backend.domain.space.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.space.space.dto.SpaceCreateRequest;
import org.tuna.zoopzoop.backend.domain.space.space.dto.SpaceCreateResponse;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

@RestController
@RequestMapping("/api/v1/space")
@RequiredArgsConstructor
@Tag(name = "ApiV1SpaceController", description = "스페이스 관련 API")
public class ApiV1SpaceController {
    private final SpaceService spaceService;

    @PostMapping
    @Operation(summary = "스페이스 생성")
    public RsData<SpaceCreateResponse> createClub(
            @Valid @RequestBody SpaceCreateRequest reqBody
    ){
        return new RsData<>("201", "클럽이 생성됐습니다.",
                new SpaceCreateResponse(
                        "buffer"
                )
        );
    }

}
