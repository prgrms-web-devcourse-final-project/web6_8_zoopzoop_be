package org.tuna.zoopzoop.backend.domain.datasource.controller;

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
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
@Tag(name = "ApiV1DataSource", description = "개인 아카이브의 파일 CRUD")
public class DatasourceController {

    private final DataSourceService dataSourceService;

    /**
     * 자료 등록
     * sourceUrl 등록할 자료 url
     * folderId  등록될 폴더 위치(null 이면 default)
     */
    @Operation(summary = "자료 등록", description = "내 PersonalArchive 안에 자료를 등록합니다.")
    @PostMapping("")
    public ResponseEntity<?> createDataSource(
            @Valid @RequestBody reqBodyForCreateDataSource rq,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 로그인된 멤버 Id 사용
        Member member = userDetails.getMember();
        Integer currentMemberId = member.getId();

        int rs = dataSourceService.createDataSource(currentMemberId, rq.sourceUrl(), rq.folderId());
        return ResponseEntity.ok()
                .body(
                        new ApiResponse<>(200, "새로운 자료가 등록됐습니다.", rs)
                );
    }

    /**
     * 자료 단건 완전 삭제
     */
    @Operation(summary = "자료 단건 삭제", description = "내 PersonalArchive 안에 자료를 단건 삭제합니다.")
    @DeleteMapping("/{dataSourceId}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Integer dataSourceId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        int deletedId = dataSourceService.deleteById(member.getId(), dataSourceId);
        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "msg", deletedId + "번 자료가 삭제됐습니다.",
                        "data", Map.of("dataSourceId", deletedId)
                )
        );
    }

    /**
     * 자료 다건 완전 삭제
     */
    @Operation(summary = "자료 다건 삭제", description = "내 PersonalArchive 안에 자료를 다건 삭제합니다.")
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteMany(
            @Valid @RequestBody reqBodyForDeleteMany body,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        dataSourceService.deleteMany(member.getId(), body.dataSourceId());

        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("status", 200);
        res.put("msg", "복수개의 자료가 삭제됐습니다.");
        res.put("data", null);

        return ResponseEntity.ok(res);
    }

    /**
     * 자료 다건 소프트 삭제
     */
    @Operation(summary = "자료 다건 임시 삭제", description = "내 PersonalArchive 안에 자료들을 임시 삭제합니다.")
    @PatchMapping("/soft-delete")
    public ResponseEntity<?> softDelete(
            @RequestBody @Valid IdsRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {

        int cnt = dataSourceService.softDelete(user.getMember().getId(), req.ids());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", 200);
        res.put("msg", "자료들이 임시 삭제됐습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }
    /**
     * 자료 다건 복원
     */
    @Operation(summary = "자료 다건 복원", description = "내 PersonalArchive 안에 자료들을 복원합니다.")
    @PatchMapping("/restore")
    public ResponseEntity<?> restore(
            @RequestBody @Valid IdsRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {

        int cnt = dataSourceService.restore(user.getMember().getId(), req.ids());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", 200);
        res.put("msg", "자료들이 복구됐습니다.");
        res.put("data", null);
        return ResponseEntity.ok(res);
    }
    /**
     * 자료 단건 이동
     *  folderId=null 이면 default 폴더
     */
    @Operation(summary = "자료 단건 이동", description = "내 PersonalArchive 안에 자료를 단건 이동합니다.")
    @PatchMapping("/{dataSourceId}/move")
    public ResponseEntity<?> moveDataSource(
            @PathVariable Integer dataSourceId,
            @Valid @RequestBody reqBodyForMoveDataSource rq,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        Integer currentMemberId = member.getId();

        DataSourceService.MoveResult result =
                dataSourceService.moveDataSource(currentMemberId, dataSourceId, rq.folderId());
        resBodyForMoveDataSource body =
                new resBodyForMoveDataSource(result.datasourceId(), result.folderId());
        String msg = body.dataSourceId() + "번 자료가 " + body.folderId() + "번 폴더로 이동했습니다.";

        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "msg", msg,
                        "data", java.util.Map.of(
                                "folderId", body.folderId(),
                                "dataSourceId", body.dataSourceId()
                        )
                )
        );
    }

    /**
     * 자료 다건 이동
     */
    @Operation(summary = "자료 다건 이동", description = "내 PersonalArchive 안에 자료들를 다건 이동합니다..")
    @PatchMapping("/move")
    public ResponseEntity<?> moveMany(
            @Valid @RequestBody reqBodyForMoveMany rq,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        Integer currentMemberId = member.getId();

        dataSourceService.moveDataSources(currentMemberId, rq.folderId(), rq.dataSourceId());

        Map<String, Object> res = new HashMap<>();
        res.put("status", 200);
        res.put("msg", "복수 개의 자료를 이동했습니다.");
        res.put("data", null);

        return ResponseEntity.ok(res);
    }

    /**
     *  파일 수정
     * @param dataSourceId  수정할 파일 Id
     * @param body          수정할 내용
     */
    @Operation(summary = "자료 수정", description = "내 PersonalArchive 안에 자료를 수정합니다.")
    @PatchMapping("/{dataSourceId}")
    public ResponseEntity<?> updateDataSource(
            @PathVariable Integer dataSourceId,
            @Valid @RequestBody reqBodyForUpdateDataSource body,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // title, summary 둘 다 비어있으면 의미 없는 요청 → 400
        boolean noTitle = (body.title() == null || body.title().isBlank());
        boolean noSummary = (body.summary() == null || body.summary().isBlank());
        if (noTitle && noSummary) {
            throw new IllegalArgumentException("변경할 값이 없습니다. title 또는 summary 중 하나 이상을 전달하세요.");
        }

        Member member = userDetails.getMember();
        Integer updatedId = dataSourceService.updateDataSource(member.getId(), dataSourceId, body.title(), body.summary()); // CHANGED
        String msg = updatedId + "번 자료가 수정됐습니다.";
        return ResponseEntity.ok(
                new ApiResponse<>(200, msg, new resBodyForUpdateDataSource(updatedId))
        );
    }

    /**
     * 자료 검색
     */
    @Operation(summary = "자료 검색", description = "내 PersonalArchive 안에 자료들을 검색합니다.")
    @GetMapping("")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer folderId,
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer memberId = userDetails.getMember().getId();

        DataSourceSearchCondition cond = DataSourceSearchCondition.builder()
                .title(title)
                .summary(summary)
                .category(category)
                .folderId(folderId)
                .folderName(folderName)
                .isActive(isActive)
                .build();

        Page<DataSourceSearchItem> page = dataSourceService.search(memberId, cond, pageable);
        String sorted = pageable.getSort().toString().replace(": ", ",");

        Map<String, Object> res  = new LinkedHashMap<>();
        res.put("status", 200);
        res.put("msg", "복수개의 자료가 조회됐습니다.");
        res.put("data", page.getContent());
        res.put("pageInfo", Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "first", page.isFirst(),
                "last", page.isLast(),
                "sorted", sorted
        ));
        return ResponseEntity.ok(res);
    }

    record ApiResponse<T>(int status, String msg, T data) {}
}
