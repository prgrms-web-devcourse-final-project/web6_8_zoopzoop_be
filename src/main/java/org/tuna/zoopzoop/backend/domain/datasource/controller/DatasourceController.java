package org.tuna.zoopzoop.backend.domain.datasource.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.global.security.StubAuthUtil;

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

        // ✅ Map.of 는 null 불가 → LinkedHashMap 사용
        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("status", 200);
        res.put("msg", "복수개의 자료가 삭제됐습니다.");
        res.put("data", null);

        return ResponseEntity.ok(res);
    }

    record ApiResponse<T>(int status, String msg, T data) {}
}
