package org.tuna.zoopzoop.backend.domain.datasource.controller;

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
import org.tuna.zoopzoop.backend.domain.datasource.service.PersonalArchiveDataSourceService;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
@Tag(name = "ApiV1DataSource", description = "개인 아카이브의 파일 CRUD")
public class DatasourceController {

    private final PersonalArchiveDataSourceService personal;

    /** 자료 등록 */
    @PostMapping("")
    @Operation(summary = "자료 등록", description = "내 PersonalArchive 안에 자료를 등록합니다.")
    public ResponseEntity<?> createDataSource(
            @Valid @RequestBody reqBodyForCreateDataSource rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        int id = personal.create(user.getMember().getId(), rq.sourceUrl(), rq.folderId());
        return ResponseEntity.ok(new ApiResponse<>(200, "새로운 자료가 등록됐습니다.", id));
    }

    /** 단건 삭제 */
    @DeleteMapping("/{dataSourceId}")
    @Operation(summary = "자료 단건 삭제", description = "내 PersonalArchive 안에 자료를 단건 삭제합니다.")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Integer dataSourceId, @AuthenticationPrincipal CustomUserDetails user
    ) {
        int deletedId = personal.deleteOne(user.getMember().getId(), dataSourceId);
        return ResponseEntity.ok(Map.of("status", 200, "msg", deletedId + "번 자료가 삭제됐습니다.", "data", Map.of("dataSourceId", deletedId)));
    }

    /** 다건 삭제 */
    @PostMapping("/delete")
    @Operation(summary = "자료 다건 삭제", description = "내 PersonalArchive 안에 자료를 다건 삭제합니다.")
    public ResponseEntity<Map<String, Object>> deleteMany(
            @Valid @RequestBody reqBodyForDeleteMany body, @AuthenticationPrincipal CustomUserDetails user
    ) {
        personal.deleteMany(user.getMember().getId(), body.dataSourceId());
        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "복수개의 자료가 삭제됐습니다.");
        res.put("data", null); // ← 테스트에서 null 기대
        return ResponseEntity.ok(res);
    }

    /** 다건 임시 삭제 */
    @PatchMapping("/soft-delete")
    @Operation(summary = "자료 다건 임시 삭제", description = "내 PersonalArchive 안에 자료들을 임시 삭제합니다.")
    public ResponseEntity<?> softDelete(@Valid @RequestBody IdsRequest req, @AuthenticationPrincipal CustomUserDetails user) {
        personal.softDelete(user.getMember().getId(), req.ids());
        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "자료들이 임시 삭제됐습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    /** 다건 복원 */
    @PatchMapping("/restore")
    @Operation(summary = "자료 다건 복원", description = "내 PersonalArchive 안에 자료들을 복원합니다.")
    public ResponseEntity<?> restore(@Valid @RequestBody IdsRequest req, @AuthenticationPrincipal CustomUserDetails user) {
        personal.restore(user.getMember().getId(), req.ids());
        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "자료들이 복구됐습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    /** 단건 이동 */
    @PatchMapping("/{dataSourceId}/move")
    @Operation(summary = "자료 단건 이동", description = "내 PersonalArchive 안에 자료를 단건 이동합니다.")
    public ResponseEntity<?> moveDataSource(
            @PathVariable Integer dataSourceId, @Valid @RequestBody reqBodyForMoveDataSource rq, @AuthenticationPrincipal CustomUserDetails user
    ) {
        var result = personal.moveOne(user.getMember().getId(), dataSourceId, rq.folderId());
        var body = new resBodyForMoveDataSource(result.datasourceId(), result.folderId());
        return ResponseEntity.ok(Map.of("status", 200, "msg", body.dataSourceId()+"번 자료가 "+body.folderId()+"번 폴더로 이동했습니다.",
                "data", Map.of("folderId", body.folderId(), "dataSourceId", body.dataSourceId())));
    }

    /** 다건 이동 */
    @PatchMapping("/move")
    @Operation(summary = "자료 다건 이동", description = "내 PersonalArchive 안에 자료를 다건 이동합니다.")
    public ResponseEntity<?> moveMany(
            @Valid @RequestBody reqBodyForMoveMany rq, @AuthenticationPrincipal CustomUserDetails user
    ) {
        personal.moveMany(user.getMember().getId(), rq.folderId(), rq.dataSourceId());
        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "복수 개의 자료를 이동했습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }

    /** 수정 */
    @PatchMapping("/{dataSourceId}")
    @Operation(summary = "자료 수정", description = "내 PersonalArchive 안에 자료를 수정합니다.")
    public ResponseEntity<?> updateDataSource(
            @PathVariable Integer dataSourceId, @Valid @RequestBody reqBodyForUpdateDataSource body, @AuthenticationPrincipal CustomUserDetails user
    ) {
        boolean noTitle = (body.title() == null || body.title().isBlank());
        boolean noSummary = (body.summary() == null || body.summary().isBlank());
        if (noTitle && noSummary) throw new IllegalArgumentException("변경할 값이 없습니다. title 또는 summary 중 하나 이상을 전달하세요.");

        Integer updatedId = personal.update(user.getMember().getId(), dataSourceId, body.title(), body.summary());
        return ResponseEntity.ok(new ApiResponse<>(200, updatedId + "번 자료가 수정됐습니다.", new resBodyForUpdateDataSource(updatedId)));
    }

    /** 검색 */
    @GetMapping("")
    @Operation(summary = "자료 검색", description = "내 PersonalArchive 안에 자료들을 검색합니다.")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        var cond = DataSourceSearchCondition.builder()
                .title(title).summary(summary).folderName(folderName).category(category).isActive(isActive).build();
        var page = personal.search(user.getMember().getId(), cond, pageable);

        var sorted = pageable.getSort().toString().replace(": ", ",");
        Map<String, Object> res  = new LinkedHashMap<>();
        res.put("status", 200);
        res.put("msg", "복수개의 자료가 조회됐습니다.");
        res.put("data", page.getContent());
        res.put("pageInfo", Map.of(
                "page", page.getNumber(), "size", page.getSize(),
                "totalElements", page.getTotalElements(), "totalPages", page.getTotalPages(),
                "first", page.isFirst(), "last", page.isLast(), "sorted", sorted
        ));
        return ResponseEntity.ok(res);
    }

    record ApiResponse<T>(int status, String msg, T data) {}
}
