package org.tuna.zoopzoop.backend.domain.news.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.news.dto.req.ReqBodyForKeyword;
import org.tuna.zoopzoop.backend.domain.news.dto.res.ResBodyForNaverNews;
import org.tuna.zoopzoop.backend.domain.news.service.NewsSearchService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news")
@Tag(name = "ApiV1NewsController", description = "뉴스 API 기반 검색 컨트롤러")
public class ApiV1NewsController {
    private final NewsSearchService newsSearchService;

    @GetMapping
    @Operation(summary = "최신 뉴스 목록 조회")
    public Mono<ResponseEntity<RsData<ResBodyForNaverNews>>> searchRecentNews(
            @RequestParam(defaultValue = "10") int display
    ) {
        return newsSearchService.searchNews("뉴스", display, 1, "date")
                .map(response -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new RsData<>(
                                "200",
                                "최신 뉴스 목록을 조회했습니다.",
                                response
                        )));
    }

    @PostMapping("/keywords")
    @Operation(summary = "최신 뉴스 목록 조회")
    public Mono<ResponseEntity<RsData<ResBodyForNaverNews>>> searchNewsByKeywords(
            @RequestParam(defaultValue = "10") int display,
            @RequestBody ReqBodyForKeyword dto
    ) {
        String query = String.join(" ", dto.keywords());
        // AND, OR 연산 쿼리를 지원한다고는 하는데, 정확한지는 모름.
        // String query = String.join("+", dto.keywords()); // AND 연산 키워드 검색
        // String query = String.join("|", dto.keywords()); // OR 연산 키워드 검색
        return newsSearchService.searchNews(query, display, 1, "sim")
                .map(response -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new RsData<>(
                                "200",
                                "키워드 기반 뉴스 목록을 조회했습니다.",
                                response
                        )));
    }
}
