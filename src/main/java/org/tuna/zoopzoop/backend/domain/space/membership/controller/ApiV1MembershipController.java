package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForGetMemberInfo;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.ResBodyForSpaceInvitationList;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/space/member")
@RequiredArgsConstructor
@Tag(name = "ApiV1MembershipController", description = "스페이스 멤버 관리 API")
public class ApiV1MembershipController {
    private final MembershipService membershipService;
    private final SpaceService spaceService;

    @GetMapping("/invite/{spaceId}")
    @Operation(summary = "스페이스 초대 목록 조회")
    public RsData<ResBodyForSpaceInvitationList> getInvites(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId
    ) throws AccessDeniedException {
        Member member = userDetails.getMember();
        Space space = spaceService.findById(spaceId);

        // 스페이스에 멤버가 속해있는지 확인
        if(!membershipService.isMemberJoinedSpace(member, space)) {
            throw new AccessDeniedException("액세스가 거부되었습니다.");
        }

        // 멤버십(초대) 목록 조회
        List<Membership> invitations = membershipService.findInvitationsBySpace(space);
        List<ResBodyForGetMemberInfo> invitationInfos = invitations.stream()
                .map(membership -> new ResBodyForGetMemberInfo(
                        membership.getMember().getId(),
                        membership.getMember().getName(),
                        membership.getMember().getProfileImageUrl()
                ))
                .toList();

        return new RsData<>(
                "200",
                "스페이스 초대 목록을 조회했습니다.",
                new ResBodyForSpaceInvitationList(
                        space.getId(),
                        invitationInfos
                )
        );
    }
}
