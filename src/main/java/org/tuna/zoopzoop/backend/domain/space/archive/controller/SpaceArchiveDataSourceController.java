package org.tuna.zoopzoop.backend.domain.space.archive.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.space.archive.service.SpaceArchiveDataSourceService;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spaces/{spaceId}/archive")
@RequiredArgsConstructor
@Tag(name = "ApiV1SpaceDataSource", description = "공유 아카이브의 파일 CRUD")
public class SpaceArchiveDataSourceController {

    private final SpaceArchiveDataSourceService spaceArchiveDataSourceService;

    /**
     * 자료 단건 불러오기
     */
    @PostMapping("/{dataSourceId}")
    @Operation(summary = "자료 단건 불러오기", description = "내 PersonalArchive 자료를 공유 아카이브로 불러옵니다.")
    public ResponseEntity<?> importOne(
            @PathVariable Integer spaceId,
            @PathVariable Integer dataSourceId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        spaceArchiveDataSourceService.importOne(spaceId, principal.getMember(), dataSourceId);

        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", dataSourceId + "번 자료를 불러오기에 성공하였습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    /**
     * 자료 다건 불러오기
     */
    @PostMapping("")
    @Operation(summary = "자료 다건 불러오기", description = "내 PersonalArchive 자료들을 공유 아카이브로 불러옵니다.")
    public ResponseEntity<?> importMany(
            @PathVariable Integer spaceId,
            @Valid @RequestBody reqBodyForDeleteMany body, // dataSourceId: List<Integer>
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        int cnt = spaceArchiveDataSourceService.importMany(spaceId, principal.getMember(), body.dataSourceId());

        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", cnt + "건의 자료 불러오기에 성공하였습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{dataSourceId}")
    @Operation(summary = "자료 단건 삭제", description = "해당 스페이스의 공유 아카이브에서 자료를 단건 삭제합니다.")
    public ResponseEntity<?> deleteOne(@PathVariable Integer spaceId, @PathVariable Integer dataSourceId,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        int deleted = spaceArchiveDataSourceService.deleteOne(spaceId, principal.getMember(), dataSourceId);
        return ResponseEntity.ok(Map.of("status", 200, "msg", deleted + "번 자료가 삭제됐습니다.", "data", Map.of("dataSourceId", deleted)));
    }

    @PostMapping("/delete")
    @Operation(summary = "자료 다건 삭제", description = "해당 스페이스의 공유 아카이브에서 자료를 다건 삭제합니다.")
    public ResponseEntity<?> deleteMany(@PathVariable Integer spaceId, @Valid @RequestBody reqBodyForDeleteMany body,
                                        @AuthenticationPrincipal CustomUserDetails principal) {
        spaceArchiveDataSourceService.deleteMany(spaceId, principal.getMember(), body.dataSourceId());

        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "복수개의 자료가 삭제됐습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/soft-delete")
    @Operation(summary = "자료 다건 임시 삭제", description = "해당 스페이스의 공유 아카이브에서 자료를 임시 삭제합니다.")
    public ResponseEntity<?> softDelete(@PathVariable Integer spaceId, @Valid @RequestBody IdsRequest req,
                                        @AuthenticationPrincipal CustomUserDetails principal) {
        spaceArchiveDataSourceService.softDelete(spaceId, principal.getMember(), req.ids());

        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "자료들이 임시 삭제됐습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/restore")
    @Operation(summary = "자료 다건 복원", description = "해당 스페이스의 공유 아카이브에서 자료를 복원합니다.")
    public ResponseEntity<?> restore(@PathVariable Integer spaceId, @Valid @RequestBody IdsRequest req,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        spaceArchiveDataSourceService.restore(spaceId, principal.getMember(), req.ids());

        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "자료들이 복구됐습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{dataSourceId}/move")
    @Operation(summary = "자료 단건 이동", description = "해당 스페이스의 공유 아카이브에서 자료를 단건 이동합니다.")
    public ResponseEntity<?> moveOne(@PathVariable Integer spaceId, @PathVariable Integer dataSourceId,
                                     @Valid @RequestBody reqBodyForMoveDataSource rq,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        var result = spaceArchiveDataSourceService.moveOne(spaceId, principal.getMember(), dataSourceId, rq.folderId());
        return ResponseEntity.ok(Map.of("status", 200, "msg", result.datasourceId()+"번 자료가 "+result.folderId()+"번 폴더로 이동했습니다.",
                "data", Map.of("folderId", result.folderId(), "dataSourceId", result.datasourceId())));
    }

    @PatchMapping("/move")
    @Operation(summary = "자료 다건 이동", description = "해당 스페이스의 공유 아카이브에서 자료를 다건 이동합니다.")
    public ResponseEntity<?> moveMany(@PathVariable Integer spaceId,
                                      @Valid @RequestBody reqBodyForMoveMany rq,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        spaceArchiveDataSourceService.moveMany(spaceId, principal.getMember(), rq.folderId(), rq.dataSourceId());

        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "복수 개의 자료를 이동했습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{dataSourceId}")
    @Operation(summary = "자료 수정", description = "해당 스페이스의 공유 아카이브에서 자료를 수정합니다.")
    public ResponseEntity<?> update(@PathVariable Integer spaceId, @PathVariable Integer dataSourceId,
                                    @Valid @RequestBody reqBodyForUpdateDataSource body,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        boolean noTitle = (body.title() == null || body.title().isBlank());
        boolean noSummary = (body.summary() == null || body.summary().isBlank());
        if (noTitle && noSummary) throw new IllegalArgumentException("변경할 값이 없습니다. title 또는 summary 중 하나 이상을 전달하세요.");

        Integer updatedId = spaceArchiveDataSourceService.update(spaceId, principal.getMember(), dataSourceId, body.title(), body.summary());
        return ResponseEntity.ok(Map.of("status", 200, "msg", updatedId + "번 자료가 수정됐습니다.", "data", Map.of("dataSourceId", updatedId)));
    }

    @GetMapping("")
    @Operation(summary = "자료 검색", description = "해당 스페이스의 공유 아카이브에서 자료를 검색합니다.")
    public ResponseEntity<?> search(@PathVariable Integer spaceId,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String summary,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String folderName,
                                    @RequestParam(required = false, defaultValue = "true") Boolean isActive,
                                    @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        var cond = DataSourceSearchCondition.builder()
                .title(title).summary(summary).folderName(folderName).category(category).isActive(isActive).build();
        var page = spaceArchiveDataSourceService.search(spaceId, principal.getMember(), cond, pageable);

        var sorted = pageable.getSort().toString().replace(": ", ",");
        return ResponseEntity.ok(Map.of(
                "status", 200, "msg", "복수개의 자료가 조회됐습니다.",
                "data", page.getContent(),
                "pageInfo", Map.of(
                        "page", page.getNumber(), "size", page.getSize(),
                        "totalElements", page.getTotalElements(), "totalPages", page.getTotalPages(),
                        "first", page.isFirst(), "last", page.isLast(), "sorted", sorted
                )
        ));
    }
}
