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

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/member")
@Tag(name = "ApiV1MemberController", description = "사용자 REST API 컨트롤러")
public class ApiV1MemberController {
    private final MemberService memberService;
    /// api/v1/member/me : 사용자 정보 조회 (GET)
    /// api/v1/member/edit : 사용자 닉네임 수정 (PUT)
    /// api/v1/member : 사용자 탈퇴 (DELETE)
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
}
