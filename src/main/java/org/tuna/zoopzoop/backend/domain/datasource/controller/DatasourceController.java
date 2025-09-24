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

    record ApiResponse<T>(int status, String msg, T data) {}
}
