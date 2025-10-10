package org.tuna.zoopzoop.backend.domain.member.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForSearchMember;
import org.tuna.zoopzoop.backend.domain.member.entity.MemberDocument;
import org.tuna.zoopzoop.backend.domain.member.service.MemberSearchService;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/member")
public class ApiV1MemberSearchController {
    private final MemberSearchService memberSearchService;
    private final MemberService memberService;

    @GetMapping("/search")
    public ResponseEntity<RsData<List<ResBodyForSearchMember>>> searchMembers(
            @RequestParam String keyword
    ) {
        List<MemberDocument> memberDocuments = memberSearchService.searchByName(keyword);
        List<ResBodyForSearchMember> memberDtos = memberDocuments.stream()
                .map(ResBodyForSearchMember::new)
                .toList();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "검색 조건에 맞는 사용자들을 조회 했습니다.",
                                memberDtos
                        )
                );
    }
}
