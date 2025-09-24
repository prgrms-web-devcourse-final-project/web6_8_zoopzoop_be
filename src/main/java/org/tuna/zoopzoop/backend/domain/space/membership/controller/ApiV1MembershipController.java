package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForGetMemberInfo;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.*;
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
    private final MemberService memberService;

    @GetMapping("/invite/{spaceId}")
    @Operation(summary = "스페이스에 초대된 유저 목록 조회")
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

    @GetMapping("/{spaceId}")
    @Operation(summary = "스페이스의 멤버 목록 조회")
    public RsData<ResBodyForSpaceMemberList> getMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId
    ) throws AccessDeniedException {
        Member member = userDetails.getMember();
        Space space = spaceService.findById(spaceId);

        // 스페이스에 멤버가 속해있는지 확인
        if(!membershipService.isMemberJoinedSpace(member, space)) {
            throw new AccessDeniedException("액세스가 거부되었습니다.");
        }

        // 멤버십(멤버) 목록 조회
        List<Membership> memberships = membershipService.findMembersBySpace(space);
        List<SpaceMemberInfo> memberInfos = memberships.stream()
                .map(membership -> new SpaceMemberInfo(
                        membership.getMember().getId(),
                        membership.getMember().getName(),
                        membership.getMember().getProfileImageUrl(),
                        membership.getAuthority()
                ))
                .toList();

        return new RsData<>(
                "200",
                "스페이스 멤버 목록을 조회했습니다.",
                new ResBodyForSpaceMemberList(
                        space.getId(),
                        space.getName(),
                        memberInfos
                )
        );
    }

    @PutMapping("/{spaceId}")
    @Operation(summary = "스페이스 멤버 권한 변경")
    public RsData<ResBodyForChangeMemberAuthority> changeMemberAuthority(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId,
            @RequestBody @Valid ReqBodyForChangeMemberAuthority reqBody
    ) throws AccessDeniedException {
        Member requester = userDetails.getMember();
        Space space = spaceService.findById(spaceId);

        // 스페이스에 요청자가 속해있는지 확인
        if(!membershipService.isMemberJoinedSpace(requester, space)) {
            throw new AccessDeniedException("액세스가 거부되었습니다.");
        }

        // 요청자의 권한이 멤버 관리 권한이 있는지 확인
        Membership requesterMembership = membershipService.findByMemberAndSpace(requester, space);
        if(!requesterMembership.getAuthority().canManageMembers()) {
            throw new AccessDeniedException("액세스가 거부되었습니다.");
        }

        // 변경 대상 멤버가 사용자 본인인지 확인
        Member requestedMember = memberService.findById(reqBody.memberId());
        if(requestedMember.equals(requester)) {
            return new RsData<>(
                    "400",
                    "본인의 권한은 변경할 수 없습니다.",
                    null
            );
        }


        // 변경 대상 멤버의 권한이 요청된 권한과 같은지 확인
        Membership requestedMembership = membershipService.findByMemberAndSpace(requestedMember, space);
        if(requestedMembership.getAuthority().equals(reqBody.newAuthority())) {
            return new RsData<>(
                    "409",
                    "이미 요청된 권한을 가지고 있습니다.",
                    null
            );
        }
        // 멤버 권한 변경
        Membership changeResult = membershipService.changeAuthority(requestedMembership, reqBody.newAuthority());

        SpaceMemberInfo memberInfo = new SpaceMemberInfo(
                changeResult.getMember().getId(),
                changeResult.getMember().getName(),
                changeResult.getMember().getProfileImageUrl(),
                changeResult.getAuthority()
        );

        return new RsData<>(
                "200",
                "멤버 권한을 변경했습니다.",
                new ResBodyForChangeMemberAuthority(
                        space.getId(),
                        space.getName(),
                        memberInfo
                )
        );
    }
}
