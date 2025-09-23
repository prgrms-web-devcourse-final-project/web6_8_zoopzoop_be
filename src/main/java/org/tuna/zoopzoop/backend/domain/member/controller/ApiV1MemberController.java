package org.tuna.zoopzoop.backend.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.member.dto.req.ReqBodyForEditMemberName;
import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForEditMemberName;
import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForGetMemberInfo;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/member")
@Tag(name = "ApiV1MemberController", description = "사용자 REST API 컨트롤러")
public class ApiV1MemberController {
    private final MemberService memberService;
    /// api/v1/member/me : 사용자 정보 조회 (GET)
    /// api/v1/member/edit : 사용자 닉네임 수정 (PUT)
    /// api/v1/member : 사용자 탈퇴 (DELETE)
    ///
    /// 아래 기능은 혹시 몰라 추가적으로 구현한 조회 기능입니다.
    /// api/v1/member : 모든 사용자 목록 조회 (GET)
    /// api/v1/member/{id} : id 기반 사용자 조회 (GET)
    /// api/v1/member?name={name} : 이름 기반 사용자 조회
    @GetMapping("/me")
    @Operation(summary = "사용자 정보 조회")
    public ResponseEntity<RsData<ResBodyForGetMemberInfo>> getMemberInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "사용자 정보를 조회했습니다.",
                                new ResBodyForGetMemberInfo(member)
                        )
                );
    }

    @PutMapping("/edit")
    @Operation(summary = "사용자 닉네임 수정")
    public ResponseEntity<RsData<ResBodyForEditMemberName>> editMemberName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReqBodyForEditMemberName reqBodyForEditMemberName
            ) {
        Member member = userDetails.getMember();
        member.updateName(reqBodyForEditMemberName.newName());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "사용자의 닉네임을 변경했습니다.",
                                new ResBodyForEditMemberName(member.getName())
                        )
                );
    }

    @DeleteMapping
    @Operation(summary = "사용자 삭제")
    public ResponseEntity<RsData<Void>> deleteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        memberService.hardDeleteMember(member);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "정상적으로 탈퇴되었습니다.",
                                null
                        )
                );
    }

    @GetMapping
    @Operation(summary = "사용자 정보 조회 - all")
    public ResponseEntity<RsData<List<ResBodyForGetMemberInfo>>> getMemberInfoAll(
    ) {
        List<Member> members = memberService.findAll();
        List<ResBodyForGetMemberInfo> memberDtos = members.stream()
                .map(ResBodyForGetMemberInfo::new)
                .toList();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "사용자 정보를 조회했습니다.",
                                memberDtos
                        )
                );
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 정보 조회")
    public ResponseEntity<RsData<ResBodyForGetMemberInfo>> getMemberInfoById(
            @PathVariable Integer id
    ) {
        Member member = memberService.findById(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                id + " id를 가진 사용자 정보를 조회했습니다.",
                                new ResBodyForGetMemberInfo(member)
                        )
                );
    }

    @GetMapping
    @Operation(summary = "사용자 정보 조회")
    public ResponseEntity<RsData<ResBodyForGetMemberInfo>> getMemberInfoByName(
            @RequestParam String name
    ) {
        Member member = memberService.findByName(name);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                name + " 이름을 가진 사용자 정보를 조회했습니다.",
                                new ResBodyForGetMemberInfo(member)
                        )
                );
    }
}
