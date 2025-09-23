package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.dto.ResBodyForSpaceSave;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/api/v1/invite")
@RequiredArgsConstructor
@Tag(name = "ApiV1MembershipController", description = "사용자에게 온 스페이스 초대 관리 API")
public class ApiV1InviteController {
    private final MembershipService membershipService;

    @PostMapping("/{inviteId}/accept")
    @Operation(summary = "스페이스 초대 수락")
    public RsData<ResBodyForSpaceSave> acceptInvite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer inviteId
    ) throws AccessDeniedException {
        Member member = userDetails.getMember();

        // membership 가져오기
        Membership membership = membershipService.findById(inviteId);
        membershipService.validateMembershipInvitation(membership, member); // 초대 수락 가능 여부 검증

        membershipService.acceptInvitation(membership); // 초대 수락 처리

        return new RsData<>(
                "200",
                "스페이스 초대가 수락됐습니다.",
                new ResBodyForSpaceSave(
                        membership.getSpace().getId(),
                        membership.getSpace().getName()
                )
        );
    }

    @PostMapping("/{inviteId}/reject")
    @Operation(summary = "스페이스 초대 거절")
    public RsData<ResBodyForSpaceSave> rejectInvite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer inviteId
    ) throws AccessDeniedException {
        Member member = userDetails.getMember();

        // membership 가져오기
        Membership membership = membershipService.findById(inviteId);
        membershipService.validateMembershipInvitation(membership, member); // 초대 거절 가능 여부 검증
        membershipService.rejectInvitation(membership); // 초대 거절 처리
        return new RsData<>(
                "200",
                "스페이스 초대가 거절됐습니다.",
                new ResBodyForSpaceSave(
                        membership.getSpace().getId(),
                        membership.getSpace().getName()
                )
        );
    }

}
