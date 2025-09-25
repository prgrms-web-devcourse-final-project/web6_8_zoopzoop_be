package org.tuna.zoopzoop.backend.domain.datasource.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.global.security.StubAuthUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
public class DatasourceController {

    private final DataSourceService dataSourceService;

    /**
     * 자료 등록
     * sourceUrl 등록할 자료 url
     * folderId  등록될 폴더 위치(null 이면 default)
     */
    @PostMapping("")
    public ResponseEntity<?> createDataSource(@Valid @RequestBody reqBodyForCreateDataSource rq) {

        //임시 인증 정보
        Integer currentMemberId = StubAuthUtil.currentMemberId();
        int rs = dataSourceService.createDataSource(currentMemberId, rq.sourceUrl(), rq.folderId());
        return ResponseEntity.ok()
                .body(
                        new ApiResponse<>(200, "새로운 자료가 등록됐습니다.", rs)
                );
    }

    /**
     * 자료 단건 삭제
     */
    @DeleteMapping("/{dataSourceId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer dataSourceId) {
        int deletedId = dataSourceService.deleteById(dataSourceId);
        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "msg", deletedId + "번 자료가 삭제됐습니다.",
                        "data", Map.of("dataSourceId", deletedId)
                )
        );
    }

    /**
     * 자료 다건 삭제
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteMany(
            @Valid @RequestBody reqBodyForDeleteMany body
    ) {
        dataSourceService.deleteMany(body.dataSourceId());

        // Map.of 는 null 불가 → LinkedHashMap 사용
        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("status", 200);
        res.put("msg", "복수개의 자료가 삭제됐습니다.");
        res.put("data", null);

        return ResponseEntity.ok(res);
    }

    /**
     * 자료 단건 이동
     *  folderId=null 이면 default 폴더
     */
    @PatchMapping("/{dataSourceId}/move")
    public ResponseEntity<?> moveDataSource(
            @PathVariable Integer dataSourceId,
            @Valid @RequestBody reqBodyForMoveDataSource rq
    ) {
        Integer currentMemberId = StubAuthUtil.currentMemberId();

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
    @PatchMapping("/move")
    public ResponseEntity<?> moveMany(@Valid @RequestBody reqBodyForMoveMany rq) {
        Integer currentMemberId = StubAuthUtil.currentMemberId();

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
    @PatchMapping("/{dataSourceId}")
    public ResponseEntity<?> updateDataSource(
            @PathVariable Integer dataSourceId,
            @Valid @RequestBody reqBodyForUpdateDataSource body
    ) {
        // title, summary 둘 다 비어있으면 의미 없는 요청 → 400
        boolean noTitle = (body.title() == null || body.title().isBlank());
        boolean noSummary = (body.summary() == null || body.summary().isBlank());
        if (noTitle && noSummary) {
            throw new IllegalArgumentException("변경할 값이 없습니다. title 또는 summary 중 하나 이상을 전달하세요.");
        }

        Integer updatedId = dataSourceService.updateDataSource(dataSourceId, body.title(), body.summary());
        String msg = updatedId + "번 자료가 수정됐습니다.";
        return ResponseEntity.ok(
                new ApiResponse<>(200, msg, new resBodyForUpdateDataSource(updatedId))
        );
    }

    record ApiResponse<T>(int status, String msg, T data) {}
}
