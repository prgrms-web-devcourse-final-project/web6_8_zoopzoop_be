package org.tuna.zoopzoop.backend.domain.space.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.space.space.dto.SpaceSaveRequest;
import org.tuna.zoopzoop.backend.domain.space.space.dto.SpaceSaveResponse;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
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
    public RsData<SpaceSaveResponse> createClub(
            @Valid @RequestBody SpaceSaveRequest reqBody
    ){
        Space newSpace = spaceService.createSpace(reqBody.name());

        return new RsData<>(
                "201",
                String.format("%s - 스페이스가 생성됐습니다.", newSpace.getName()),
                new SpaceSaveResponse(
                        newSpace.getId(),
                        newSpace.getName()
                )
        );
    }

    @DeleteMapping("/{spaceId}")
    @Operation(summary = "스페이스 삭제")
    public RsData<Void> deleteSpace(
            @PathVariable Integer spaceId
    ){
        String deletedSpaceName = spaceService.deleteSpace(spaceId);
        return new RsData<>(
                "200",
                String.format("%s - 스페이스가 삭제됐습니다.", deletedSpaceName),
                null
        );
    }

    @PutMapping("/{spaceId}")
    @Operation(summary = "스페이스 이름 변경")
    public RsData<SpaceSaveResponse> updateSpaceName(
            @PathVariable Integer spaceId,
            @Valid @RequestBody SpaceSaveRequest reqBody
    ){
        Space updatedSpace = spaceService.updateSpaceName(spaceId, reqBody.name());

        return new RsData<>(
                "200",
                String.format("%s - 스페이스 이름이 변경됐습니다.", updatedSpace.getName()),
                new SpaceSaveResponse(
                        updatedSpace.getId(),
                        updatedSpace.getName()
                )
        );
    }

}
