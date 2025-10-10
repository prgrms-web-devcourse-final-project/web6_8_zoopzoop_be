package org.tuna.zoopzoop.backend.domain.news.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.news.dto.req.ReqBodyForKeyword;
import org.tuna.zoopzoop.backend.domain.news.dto.res.ResBodyForNaverNews;
import org.tuna.zoopzoop.backend.domain.news.service.NewsAPIService;
import org.tuna.zoopzoop.backend.domain.news.service.NewsService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news")
@Tag(name = "ApiV1NewsController", description = "뉴스 API 기반 검색 컨트롤러")
public class ApiV1NewsController {
    private final NewsAPIService newsSearchService;
    private final NewsService newsService;


    /**
     * 최신 뉴스 목록을 조회하는 API
     * 한번에 100개를 조회 합니다.
     * HTTP METHOD: GET
     */
    @GetMapping
    @Operation(summary = "최신 뉴스 목록 조회")
    public Mono<ResponseEntity<RsData<ResBodyForNaverNews>>> searchRecentNews(
    ) {
        // Naver 뉴스 API에선 Non-keyword 방식의 검색을 지원하지 않음.
        // 그래서 일단 그냥 검색 쿼리를 '뉴스'라고 지정하고 해 보았는데, 꽤나 좋은 결과를 받아옴. (목표하던 기능과 비슷함.)
        return newsSearchService.searchNews("뉴스", "date")
                .map(response -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new RsData<>(
                                "200",
                                "최신 뉴스 목록을 조회했습니다.",
                                response
                        )));
    }

    /**
     * 입력한 키워드 기반으로 뉴스 목록을 조회하는 API
     * HTTP METHOD: GET
     * 한번에 100개를 조회 합니다.
     * @param dto 키워드를 받아오는 reqDto
     */
    @PostMapping("/keywords")
    @Operation(summary = "키워드 기반 뉴스 조회")
    public Mono<ResponseEntity<RsData<ResBodyForNaverNews>>> searchNewsByKeywords(
            @RequestBody ReqBodyForKeyword dto
    ) {
        String query = String.join(" ", dto.keywords());
        // AND, OR 연산 쿼리를 지원한다고는 하는데, 정확한지는 모름.
        // String query = String.join("+", dto.keywords()); // AND 연산 키워드 검색
        // String query = String.join("|", dto.keywords()); // OR 연산 키워드 검색
        return newsSearchService.searchNews(query, "sim")
                .map(response -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new RsData<>(
                                "200",
                                "키워드 기반 뉴스 목록을 조회했습니다.",
                                response
                        )));
    }

    /**
     * 개인 아카이브의 폴더 내부의 자료를 기반으로 키워드를 추천해서 검색하는 뉴스 API
     * HTTP METHOD: GET
     * 한번에 100개를 조회 합니다.
     * @param userDetails 로그인한 사용자
     * @param folderId 대상 폴더 id
     */
    @GetMapping("/recommends/personal/{folderId}")
    @Operation(summary = "개인 아카이브 뉴스 추천")
    public Mono<ResponseEntity<RsData<ResBodyForNaverNews>>> searchNewsByRecommends(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer folderId
    ) {
        Member member = userDetails.getMember();
        List<String> frequency = newsService.getTagFrequencyFromFiles(member.getId(), folderId);
        String query = String.join(" ", frequency);

        return newsSearchService.searchNews(query, "sim")
                .map(response -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new RsData<>(
                                "200",
                                "키워드 기반 뉴스 목록을 조회했습니다.",
                                response
                        )));
    }

    /**
     * 공유 아카이브의 폴더 내부의 자료를 기반으로 키워드를 추천해서 검색하는 뉴스 API
     * HTTP METHOD: GET
     * 한번에 100개를 조회 합니다.
     * @param userDetails 로그인한 사용자
     * @param spaceId 대상 스페이스 id
     * @param folderId 대상 폴더 id
     */
    @GetMapping("/recommends/shared/{spaceId}/{folderId}")
    @Operation(summary = "공유 아카이브 뉴스 추천")
    public Mono<ResponseEntity<RsData<ResBodyForNaverNews>>> searchNewsByRecommends(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId,
            @PathVariable Integer folderId
    ) {
        Member member = userDetails.getMember();
        List<String> frequency = newsService.getTagFrequencyFromFilesInSharing(spaceId, member.getId(), folderId);
        String query = String.join(" ", frequency);

        return newsSearchService.searchNews(query, "sim")
                .map(response -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new RsData<>(
                                "200",
                                "키워드 기반 뉴스 목록을 조회했습니다.",
                                response
                        )));
    }
}
