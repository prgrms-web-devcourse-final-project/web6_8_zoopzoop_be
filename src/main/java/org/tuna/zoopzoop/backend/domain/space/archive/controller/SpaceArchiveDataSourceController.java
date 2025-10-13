package org.tuna.zoopzoop.backend.domain.space.archive.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.domain.space.archive.service.SpaceDataSourceService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/space/{spaceId}/archive/datasources")
@RequiredArgsConstructor
@Tag(name = "ApiV1DataSource(Space)", description = "공유 아카이브 자료 API")
public class SpaceArchiveDataSourceController {

    private final SpaceDataSourceService spaceApp;

    // ===== 단건 삭제 =====
    @Operation(summary = "공유 자료 단건 삭제")
    @DeleteMapping("/{dataSourceId}")
    public ResponseEntity<RsData<Map<String, Integer>>> deleteOne(
            @PathVariable String spaceId,
            @PathVariable Integer dataSourceId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        int deleted = spaceApp.deleteOne(user.getMember().getId(), spaceId, dataSourceId);
        return ResponseEntity.ok(
                new RsData<>("200", deleted + "번 자료가 삭제됐습니다.", Map.of("dataSourceId", deleted))
        );
    }

    // ==== 다건 삭제 =====
    @Operation(summary = "공유 자료 다건 삭제")
    @DeleteMapping("/delete")
    public ResponseEntity<RsData<Void>> deleteMany(
            @PathVariable String spaceId,
            @RequestBody @Valid reqBodyForDeleteMany rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        spaceApp.deleteMany(user.getMember().getId(), spaceId, rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "복수개의 자료가 삭제됐습니다.", null));
    }

    // ===== 임시 삭제 =====
    @Operation(summary = "공유 자료 다건 임시 삭제")
    @PatchMapping("/soft-delete")
    public ResponseEntity<RsData<Void>> softDelete(
            @PathVariable String spaceId,
            @RequestBody @Valid IdsRequest rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        spaceApp.softDelete(user.getMember().getId(), spaceId, rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "자료들이 임시 삭제됐습니다.", null));
    }

    // ===== 복원 =====
    @Operation(summary = "공유 자료 다건 복원")
    @PatchMapping("/restore")
    public ResponseEntity<RsData<Void>> restore(
            @PathVariable String spaceId,
            @RequestBody @Valid IdsRequest rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        spaceApp.restore(user.getMember().getId(), spaceId, rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "자료들이 복구됐습니다.", null));
    }

    // ===== 이동 =====
    @Operation(summary = "공유 자료 단건 이동")
    @PatchMapping("/{dataSourceId}/move")
    public ResponseEntity<RsData<Map<String, Integer>>> moveOne(
            @PathVariable String spaceId,
            @PathVariable Integer dataSourceId,
            @RequestBody @Valid reqBodyForMoveDataSource rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        var result = spaceApp.moveOne(user.getMember().getId(), spaceId, dataSourceId, rq.folderId());
        String msg = result.dataSourceId() + "번 자료가 " + result.folderId() + "번 폴더로 이동했습니다.";
        return ResponseEntity.ok(
                new RsData<>("200", msg, Map.of("folderId", result.folderId(), "dataSourceId", result.dataSourceId()))
        );
    }

    @Operation(summary = "공유 자료 다건 이동")
    @PatchMapping("/move")
    public ResponseEntity<RsData<Void>> moveMany(
            @PathVariable String spaceId,
            @RequestBody @Valid reqBodyForMoveMany rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        spaceApp.moveMany(user.getMember().getId(), spaceId, rq.folderId(), rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "복수 개의 자료를 이동했습니다.", null));
    }

    // ===== 수정 =====
    // JSON만 수정
    @Operation(summary = "공유 자료 수정")
    @PatchMapping(path = "/{dataSourceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RsData<Map<String, Integer>>> updateSpaceJson(
            @PathVariable String spaceId,
            @PathVariable Integer dataSourceId,
            @RequestBody @Valid reqBodyForUpdateDataSource body,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        boolean anyPresent =
                (body.title() != null && body.title().isPresent()) ||
                        (body.summary() != null && body.summary().isPresent()) ||
                        (body.sourceUrl() != null && body.sourceUrl().isPresent()) ||
                        (body.imageUrl() != null && body.imageUrl().isPresent()) ||
                        (body.source() != null && body.source().isPresent()) ||
                        (body.tags() != null && body.tags().isPresent()) ||
                        (body.category() != null && body.category().isPresent());
        if (!anyPresent) throw new IllegalArgumentException("변경할 값이 없습니다.");

        var cmd = DataSourceService.UpdateCmd.builder()
                .title(body.title())
                .summary(body.summary())
                .source(body.source())
                .sourceUrl(body.sourceUrl())
                .imageUrl(body.imageUrl())
                .category(body.category())
                .tags(body.tags())
                .build();

        int updatedId = spaceApp.update(user.getMember().getId(), spaceId, dataSourceId, cmd);

        return ResponseEntity.ok(
                new RsData<>("200", updatedId + "번 자료가 수정됐습니다.", Map.of("dataSourceId", updatedId))
        );
    }

    // 이미지 포함 멀티파트 수정
    @Operation(summary = "공유 자료 수정(멀티파트: 이미지+JSON)")
    @PatchMapping(path = "/{dataSourceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RsData<Map<String, Object>>> updateSpaceMultipart(
            @PathVariable String spaceId,
            @PathVariable Integer dataSourceId,
            @RequestPart("payload") @Valid reqBodyForUpdateDataSource body, // JSON 파트
            @RequestPart(value = "image", required = false) MultipartFile image, // 파일 파트
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        boolean anyPresent =
                (body.title() != null && body.title().isPresent()) ||
                        (body.summary() != null && body.summary().isPresent()) ||
                        (body.sourceUrl() != null && body.sourceUrl().isPresent()) ||
                        (body.imageUrl() != null && body.imageUrl().isPresent()) ||
                        (body.source() != null && body.source().isPresent()) ||
                        (body.tags() != null && body.tags().isPresent()) ||
                        (body.category() != null && body.category().isPresent()) ||
                        (image != null && !image.isEmpty());
        if (!anyPresent) throw new IllegalArgumentException("변경할 값이 없습니다.");

        var baseCmd = DataSourceService.UpdateCmd.builder()
                .title(body.title())
                .summary(body.summary())
                .source(body.source())
                .sourceUrl(body.sourceUrl())
                .imageUrl(body.imageUrl())
                .category(body.category())
                .tags(body.tags())
                .build();

        var outcome = spaceApp.updateWithImage(
                user.getMember().getId(), spaceId, dataSourceId, baseCmd, image
        );

        Map<String, Object> data = new HashMap<>();
        data.put("dataSourceId", outcome.dataSourceId());
        data.put("imageUrl", outcome.imageUrl());

        return ResponseEntity.ok(new RsData<>("200", "자료가 수정됐습니다.", data));
    }


    // ===== 불러오기(개인 → 공유) =====
    @Operation(summary = "개인 → 공유: 자료 단건 불러오기")
    @PostMapping("/{dataSourceId}/import")
    public ResponseEntity<RsData<Map<String, Integer>>> importOne(
            @PathVariable String spaceId,
            @PathVariable Integer dataSourceId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Integer targetFolderId = (body == null) ? null : (Integer) body.get("targetFolderId"); // 0/null = default 처리 내부에서
        int createdId = spaceApp.importFromPersonal(user.getMember().getId(), spaceId, dataSourceId, targetFolderId);

        return ResponseEntity.ok(
                new RsData<>("200", createdId + "번 자료를 불러오기에 성공하였습니다.", Map.of("dataSourceId", createdId))
        );
    }

    // ===== 다건 불러오기(개인 → 공유) =====
    @Operation(summary = "개인 → 공유: 자료 다건 불러오기")
    @PostMapping("/import/batch")
    public ResponseEntity<RsData<Map<String, List<Integer>>>> importBatch(
            @PathVariable String spaceId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("dataSourceId");
        Integer targetFolderId = (Integer) body.get("targetFolderId");
        List<Integer> results = spaceApp.importManyFromPersonal(
                user.getMember().getId(), spaceId, ids, targetFolderId);
        return ResponseEntity.ok(
                new RsData<>("200", results.size() + "건의 자료 불러오기에 성공하였습니다.",
                        Map.of("results", results))
        );
    }

    // ===== 검색 =====
    @Operation(summary = "공유 자료 검색")
    @GetMapping("")
    public ResponseEntity<RsData<SearchResponse<DataSourceSearchItem>>> search(
            @PathVariable String spaceId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer folderId,
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Category categoryEnum = category != null ? Category.from(category) : null;
        var cond = DataSourceSearchCondition.builder()
                .title(title).summary(summary).category(categoryEnum).folderId(folderId)
                .folderName(folderName).isActive(isActive).keyword(keyword).build();

        Page<DataSourceSearchItem> page = spaceApp.search(user.getMember().getId(), spaceId, cond, pageable);
        String sorted = pageable.getSort().toString().replace(": ", ",");

        var pageInfo = new PageInfo(
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast(), sorted
        );
        var body = new SearchResponse<>(page.getContent(), pageInfo);

        return ResponseEntity.ok(new RsData<>("200", "복수개의 자료가 조회됐습니다.", body));
    }
}
