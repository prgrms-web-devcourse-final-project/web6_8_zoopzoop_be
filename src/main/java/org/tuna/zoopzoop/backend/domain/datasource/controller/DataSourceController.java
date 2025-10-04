package org.tuna.zoopzoop.backend.domain.datasource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.domain.datasource.service.PersonalDataSourceService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
@Tag(name = "ApiV1DataSource(Personal)", description = "개인 아카이브 자료 API")
public class DataSourceController {

    private final PersonalDataSourceService personalApp;

    // ===== 등록 (개인만) =====
    // DataSourceController

    @Operation(summary = "자료 등록", description = "내 PersonalArchive 안에 자료를 등록합니다.")
    @PostMapping("")
    public ResponseEntity<RsData<Map<String, Integer>>> createDataSource(
            @Valid @RequestBody reqBodyForCreateDataSource rq,
            @AuthenticationPrincipal CustomUserDetails user
    )  throws IOException {
        int id = personalApp.create(
                user.getMember().getId(),
                rq.sourceUrl(),
                rq.folderId(),
                DataSourceService.CreateCmd.builder().build()
        );
        return ResponseEntity.ok(
                new RsData<>("200", "새로운 자료가 등록됐습니다.", Map.of("dataSourceId", id))
        );
    }


    // ===== 단건 삭제 =====
    @Operation(summary = "자료 단건 삭제", description = "내 PersonalArchive 안에 자료를 단건 삭제합니다.")
    @DeleteMapping("/{dataSourceId}")
    public ResponseEntity<RsData<Map<String, Integer>>> delete(
            @PathVariable Integer dataSourceId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        int deletedId = personalApp.deleteOne(user.getMember().getId(), dataSourceId);
        return ResponseEntity.ok(
                new RsData<>("200", deletedId + "번 자료가 삭제됐습니다.", Map.of("dataSourceId", deletedId))
        );
    }

    // ===== 다건 삭제 =====
    @Operation(summary = "자료 다건 삭제", description = "내 PersonalArchive 안에 자료를 다건 삭제합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<RsData<Void>> deleteMany(
            @Valid @RequestBody reqBodyForDeleteMany rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        personalApp.deleteMany(user.getMember().getId(), rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "복수개의 자료가 삭제됐습니다.", null));
    }

    // ===== 소프트 삭제/복원 =====
    @Operation(summary = "자료 다건 임시 삭제", description = "내 PersonalArchive 안에 자료들을 임시 삭제합니다.")
    @PatchMapping("/soft-delete")
    public ResponseEntity<RsData<Void>> softDelete(@RequestBody @Valid IdsRequest rq,
                                                   @AuthenticationPrincipal CustomUserDetails user) {
        personalApp.softDelete(user.getMember().getId(), rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "자료들이 임시 삭제됐습니다.", null));
    }

    @Operation(summary = "자료 다건 복원", description = "내 PersonalArchive 안에 자료들을 복원합니다.")
    @PatchMapping("/restore")
    public ResponseEntity<RsData<Void>> restore(@RequestBody @Valid IdsRequest rq,
                                                @AuthenticationPrincipal CustomUserDetails user) {
        personalApp.restore(user.getMember().getId(), rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "자료들이 복구됐습니다.", null));
    }

    // ===== 이동 =====
    @Operation(summary = "자료 단건 이동", description = "내 PersonalArchive 안에 자료를 단건 이동합니다.")
    @PatchMapping("/{dataSourceId}/move")
    public ResponseEntity<RsData<Map<String, Integer>>> moveDataSource(
            @PathVariable Integer dataSourceId,
            @Valid @RequestBody reqBodyForMoveDataSource rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        var result = personalApp.moveOne(user.getMember().getId(), dataSourceId, rq.folderId());
        String msg = result.dataSourceId() + "번 자료가 " + result.folderId() + "번 폴더로 이동했습니다.";
        return ResponseEntity.ok(
                new RsData<>("200", msg,
                        Map.of("folderId", result.folderId(), "dataSourceId", result.dataSourceId()))
        );
    }

    @Operation(summary = "자료 다건 이동", description = "내 PersonalArchive 안에 자료들을 다건 이동합니다.")
    @PatchMapping("/move")
    public ResponseEntity<RsData<Void>> moveMany(
            @Valid @RequestBody reqBodyForMoveMany rq,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        personalApp.moveMany(user.getMember().getId(), rq.folderId(), rq.dataSourceId());
        return ResponseEntity.ok(new RsData<>("200", "복수 개의 자료를 이동했습니다.", null));
    }

    // ===== 수정 =====
    @Operation(summary = "자료 수정", description = "내 PersonalArchive 안에 자료를 수정합니다.")
    @PatchMapping("/{dataSourceId}")
    public ResponseEntity<RsData<Map<String, Integer>>> updateDataSource(
            @PathVariable Integer dataSourceId,
            @RequestBody reqBodyForUpdateDataSource body,
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


        var catNullable = body.category();

        // category enum 변환 시도
        JsonNullable<Category> enumCat = null;
        if (catNullable != null && catNullable.isPresent()) {
            String raw = catNullable.get();
            try {
                // 필요하면 대소문자 허용 로직 추가
                enumCat = JsonNullable.of(Category.valueOf(raw.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + raw);
            }
        }

        int updatedId = personalApp.update(
                user.getMember().getId(),
                dataSourceId,
                DataSourceService.UpdateCmd.builder()
                        .title(body.title()).summary(body.summary()).sourceUrl(body.sourceUrl())
                        .imageUrl(body.imageUrl()).source(body.source())
                        .tags(body.tags()).category(enumCat)
                        .build()
        );

        return ResponseEntity.ok(
                new RsData<>("200", updatedId + "번 자료가 수정됐습니다.", Map.of("dataSourceId", updatedId))
        );
    }

    // ===== 검색 =====
    @Operation(summary = "자료 검색", description = "내 PersonalArchive 안에 자료들을 검색합니다.")
    @GetMapping("")
    public ResponseEntity<RsData<SearchResponse<DataSourceSearchItem>>> search(
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
        var cond = DataSourceSearchCondition.builder()
                .title(title).summary(summary).category(category).folderId(folderId)
                .folderName(folderName).isActive(isActive).keyword(keyword).build();

        Page<DataSourceSearchItem> page = personalApp.search(user.getMember().getId(), cond, pageable);
        String sorted = pageable.getSort().toString().replace(": ", ",");

        var pageInfo = new PageInfo(
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast(), sorted
        );
        var body = new SearchResponse<>(page.getContent(), pageInfo);

        return ResponseEntity.ok(new RsData<>("200", "복수개의 자료가 조회됐습니다.", body));
    }
}