package org.tuna.zoopzoop.backend.domain.space.archive.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.domain.space.archive.service.SpaceDataSourceService;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spaces/{spaceId}/archive/datasources")
@RequiredArgsConstructor
@Tag(name = "ApiV1DataSource(Space)", description = "공유 아카이브 자료 API")
public class SpaceArchiveDataSourceController {

    private final SpaceDataSourceService spaceApp;

    // 생성: 공유는 “불러오기(import)”만 지원(필요 시 별도 POST 추가 가능)

    // 삭제
    @Operation(summary = "공유 자료 단건 삭제")
    @DeleteMapping("/{dataSourceId}")
    public ResponseEntity<?> deleteOne(@PathVariable String spaceId,
                                       @PathVariable Integer dataSourceId,
                                       @AuthenticationPrincipal CustomUserDetails user) {
        int deleted = spaceApp.deleteOne(user.getMember().getId(), spaceId, dataSourceId);
        return ResponseEntity.ok(Map.of("status", 200, "msg", deleted + "번 자료가 삭제됐습니다.", "data", Map.of("dataSourceId", deleted)));
    }

    @Operation(summary = "공유 자료 다건 삭제")
    @PostMapping("/delete")
    public ResponseEntity<?> deleteMany(@PathVariable String spaceId,
                                        @RequestBody @Valid reqBodyForDeleteMany rq,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        spaceApp.deleteMany(user.getMember().getId(), spaceId, rq.dataSourceId());
        return ResponseEntity.ok(Map.of("status", 200, "msg", "복수개의 자료가 삭제됐습니다.", "data", null));
    }

    // 소프트 삭제/복원
    @Operation(summary = "공유 자료 다건 임시 삭제")
    @PatchMapping("/soft-delete")
    public ResponseEntity<?> softDelete(@PathVariable String spaceId,
                                        @RequestBody @Valid IdsRequest rq,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        spaceApp.softDelete(user.getMember().getId(), spaceId, rq.ids());
        return ResponseEntity.ok(Map.of("status", 200, "msg", "자료들이 임시 삭제됐습니다.", "data", null));
    }

    @Operation(summary = "공유 자료 다건 복원")
    @PatchMapping("/restore")
    public ResponseEntity<?> restore(@PathVariable String spaceId,
                                     @RequestBody @Valid IdsRequest rq,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        spaceApp.restore(user.getMember().getId(), spaceId, rq.ids());
        return ResponseEntity.ok(Map.of("status", 200, "msg", "자료들이 복구됐습니다.", "data", null));
    }

    // 이동
    @Operation(summary = "공유 자료 단건 이동")
    @PatchMapping("/{dataSourceId}/move")
    public ResponseEntity<?> moveOne(@PathVariable String spaceId,
                                     @PathVariable Integer dataSourceId,
                                     @RequestBody @Valid reqBodyForMoveDataSource rq,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        var result = spaceApp.moveOne(user.getMember().getId(), spaceId, dataSourceId, rq.folderId());
        String msg = result.dataSourceId() + "번 자료가 " + result.folderId() + "번 폴더로 이동했습니다.";
        return ResponseEntity.ok(Map.of("status", 200, "msg", msg, "data",
                Map.of("folderId", result.folderId(), "dataSourceId", result.dataSourceId())));
    }

    @Operation(summary = "공유 자료 다건 이동")
    @PatchMapping("/move")
    public ResponseEntity<?> moveMany(@PathVariable String spaceId,
                                      @RequestBody @Valid reqBodyForMoveMany rq,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        spaceApp.moveMany(user.getMember().getId(), spaceId, rq.folderId(), rq.dataSourceId());
        return ResponseEntity.ok(Map.of("status", 200, "msg", "복수 개의 자료를 이동했습니다.", "data", null));
    }

    // 수정
    @Operation(summary = "공유 자료 수정")
    @PatchMapping("/{dataSourceId}")
    public ResponseEntity<?> update(@PathVariable String spaceId,
                                    @PathVariable Integer dataSourceId,
                                    @RequestBody reqBodyForUpdateDataSource body,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        boolean anyPresent =
                body.title().isPresent() || body.summary().isPresent() || body.sourceUrl().isPresent() ||
                        body.imageUrl().isPresent() || body.source().isPresent() || body.tags().isPresent() ||
                        body.category().isPresent();
        if (!anyPresent) throw new IllegalArgumentException("변경할 값이 없습니다.");

        // 권한 및 소속 검증은 AppService 내부에서 수행되므로, 여기선 단순 위임해도 됨.
        var cmd = DataSourceService.UpdateCmd.builder()
                .title(body.title()).summary(body.summary()).sourceUrl(body.sourceUrl())
                .imageUrl(body.imageUrl()).source(body.source())
                .tags(body.tags()).category(body.category()).build();

        // AppService에 'update'가 필요하면 추가 구현. 여기서는 간략화하여 moveOne/soft/restore처럼 domain.update를 활용하려면
        // 먼저 공유 소속 검증 + domain.update 호출 구조를 AppService에 추가하세요.
        // (지면상 생략 시, 추후 동일 패턴으로 spaceApp.update(...) 구현 권장)
        throw new UnsupportedOperationException("공유 update는 spaceApp.update(...) 구현 후 사용하세요.");
    }

    // 검색
    @Operation(summary = "공유 자료 검색")
    @GetMapping("")
    public ResponseEntity<?> search(@PathVariable String spaceId,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String summary,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) Integer folderId,
                                    @RequestParam(required = false) String folderName,
                                    @RequestParam(required = false, defaultValue = "true") Boolean isActive,
                                    @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                    @AuthenticationPrincipal CustomUserDetails user) {

        var cond = DataSourceSearchCondition.builder()
                .title(title).summary(summary).category(category)
                .folderId(folderId).folderName(folderName).isActive(isActive).build();

        Page<DataSourceSearchItem> page = spaceApp.search(user.getMember().getId(), spaceId, cond, pageable);
        String sorted = pageable.getSort().toString().replace(": ", ",");

        Map<String, Object> res = new LinkedHashMap<>();
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
}

