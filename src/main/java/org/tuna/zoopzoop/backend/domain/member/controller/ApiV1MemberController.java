package org.tuna.zoopzoop.backend.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tuna.zoopzoop.backend.domain.auth.service.RefreshTokenService;
import org.tuna.zoopzoop.backend.domain.member.dto.req.ReqBodyForEditMember;
import org.tuna.zoopzoop.backend.domain.member.dto.req.ReqBodyForEditMemberName;
import org.tuna.zoopzoop.backend.domain.member.dto.res.*;
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
    private final RefreshTokenService refreshTokenService;
    /// api/v1/member/me : 사용자 정보 조회 (GET)
    /// api/v1/member/edit : 사용자 닉네임 수정 (PUT)
    /// api/v1/member : 사용자 탈퇴 (DELETE)
    ///
    /// 아래 기능은 혹시 몰라 추가적으로 구현한 조회 기능입니다.
    /// api/v1/member/all : 모든 사용자 목록 조회 (GET)
    /// api/v1/member/{id} : id 기반 사용자 조회 (GET)
    /// api/v1/member?name={name} : 이름 기반 사용자 조회 (GET)

    /**
     * 현재 로그인한 사용자의 정보를 조회하는 API
     * HTTP METHOD: GET
     * @param userDetails @AuthenticationPrincipal로 받아오는 현재 사용자 정보
     */
    @GetMapping("/me")
    @Operation(summary = "사용자 정보 조회")
    public ResponseEntity<RsData<ResBodyForGetMemberInfoV2>> getMemberInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "사용자 정보를 조회했습니다.",
                                new ResBodyForGetMemberInfoV2(member)
                        )
                );
    }

    /**
     * 현재 로그인한 사용자의 이름을 변경하는 API
     * HTTP METHOD: PUT
     * @param userDetails @AuthenticationPrincipal로 받아오는 현재 사용자 정보
     * @param reqBodyForEditMemberName 수정할 닉네임을 받아오는 reqDto
     */
    @PutMapping("/edit/name")
    @Operation(summary = "사용자 닉네임 수정")
    public ResponseEntity<RsData<ResBodyForEditMemberName>> editMemberName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReqBodyForEditMemberName reqBodyForEditMemberName
            ) {
        Member member = userDetails.getMember();
        memberService.updateMemberName(member, reqBodyForEditMemberName.newName());
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

    /**
     * 현재 로그인한 사용자의 프로필 이미지를 변경하는 API
     * HTTP METHOD: PUT
     * @param userDetails @AuthenticationPrincipal로 받아오는 현재 사용자 정보
     * @param file 수정할 프로필 이미지를 받아오는 MultipartFile
     */
    @PutMapping("/edit/image")
    @Operation(summary = "사용자 닉네임 수정")
    public ResponseEntity<RsData<ResBodyForEditMemberProfileImage>> editMemberProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file
            ) {
        Member member = userDetails.getMember();
        memberService.updateMemberProfileUrl(member, file);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "사용자의 프로필 이미지를 변경했습니다.",
                                new ResBodyForEditMemberProfileImage(member.getProfileImageUrl())
                        )
                );
    }

    /**
     * 현재 로그인한 사용자의 닉네임과 프로필 이미지를 변경하는 API
     * HTTP METHOD: PUT
     * @param userDetails @AuthenticationPrincipal로 받아오는 현재 사용자 정보
     * @param reqBodyForEditMember 수정할 프로필 정보를 받아오는 dto
     */
    @PutMapping("/edit")
    @Operation(summary = "사용자 프로필 수정")
    public ResponseEntity<RsData<ResBodyForEditMember>> editMemberProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReqBodyForEditMember reqBodyForEditMember
    ) {
        Member member = userDetails.getMember();
        memberService.updateMemberProfile(member, reqBodyForEditMember.newName(), reqBodyForEditMember.file());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new RsData<>(
                                "200",
                                "사용자의 프로필을 변경했습니다.",
                                new ResBodyForEditMember(member.getName(), member.getProfileImageUrl())
                        )
                );
    }


    /**
     * 현재 로그인한 사용자를 삭제하는 API
     * 사용할 지 모르겠음.
     * HTTP METHOD: DELETE
     * @param userDetails @AuthenticationPrincipal로 받아오는 현재 사용자 정보
     */

    @DeleteMapping
    @Operation(summary = "사용자 삭제")
    public ResponseEntity<RsData<Void>> deleteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        refreshTokenService.deleteByMember(member);
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

    /**
     * 모든 사용자의 정보를 조회하는 API
     * HTTP METHOD: GET
     */
    @GetMapping("/all")
    @Operation(summary = "모든 사용자 정보 조회")
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
                                "모든 사용자 정보를 조회했습니다.",
                                memberDtos
                        )
                );
    }

    /**
     * ID 기반으로 사용자의 정보를 조회하는 API
     * HTTP METHOD: GET
     * @param id 조회할 사용자의 ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "id 기반 사용자 정보 조회")
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

    /**
     * 이름 기반으로 사용자의 정보를 조회하는 API
     * HTTP METHOD: GET
     * @param name 조회할 사용자의 name
     */
    @GetMapping
    @Operation(summary = "이름 기반 사용자 정보 조회")
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
