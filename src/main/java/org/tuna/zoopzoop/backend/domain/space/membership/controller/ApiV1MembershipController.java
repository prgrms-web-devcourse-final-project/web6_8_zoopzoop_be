package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.tuna.zoopzoop.backend.domain.member.dto.etc.SimpleUserInfo;
import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForGetMemberInfo;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.etc.SpaceMemberInfo;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.req.ReqBodyForChangeMemberAuthority;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.req.ReqBodyForExpelMember;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.req.ReqBodyForInviteMembers;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.res.*;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.dto.ResBodyForSpaceSave;
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

        Membership changeResult = membershipService.changeMemberAuthority(
                requester,
                space,
                reqBody.memberId(),
                reqBody.newAuthority()
        );

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

    @PostMapping("/{spaceId}")
    @Operation(summary = "스페이스 멤버 초대")
    public RsData<ResBodyForInviteMembers> inviteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId,
            @RequestBody @Valid ReqBodyForInviteMembers reqBody
    ) throws AccessDeniedException {
        Member requester = userDetails.getMember();
        Space space = spaceService.findById(spaceId);

        membershipService.checkAdminAuthority(requester, space);

        List<Membership> inviteResults = membershipService.inviteMembersToSpace(
                space,
                reqBody.memberNames()
        );

        List<SimpleUserInfo> invitedMemberInfos = inviteResults.stream()
                .map(membership -> new SimpleUserInfo(
                        membership.getMember().getId(),
                        membership.getMember().getName()
                ))
                .toList();

        return new RsData<>(
                "200",
                "사용자를 스페이스에 초대했습니다.",
                new ResBodyForInviteMembers(
                        space.getId(),
                        space.getName(),
                        invitedMemberInfos
                )
        );
    }

    @DeleteMapping("/{spaceId}")
    @Operation(summary = "스페이스 멤버 퇴출")
    public RsData<ResBodyForExpelMember> expelMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId,
            @RequestBody @Valid ReqBodyForExpelMember reqBody
    ) throws AccessDeniedException {
        Member requester = userDetails.getMember();
        Space space = spaceService.findById(spaceId);
        Member targetMember = memberService.findById(reqBody.memberId());

        membershipService.checkAdminAuthority(requester, space);

        // 본인 강퇴 방지
        if(requester.equals(targetMember)) {
            throw new AccessDeniedException("본인은 강퇴할 수 없습니다.");
        }

        membershipService.expelMemberFromSpace(targetMember, space);

        ResBodyForGetMemberInfo expelledMemberInfo = new ResBodyForGetMemberInfo(
                targetMember.getId(),
                targetMember.getName(),
                targetMember.getProfileImageUrl()
        );
        return new RsData<>(
                "200",
                "멤버를 스페이스에서 퇴출했습니다.",
                new ResBodyForExpelMember(
                        space.getId(),
                        space.getName(),
                        expelledMemberInfo
                )
        );
    }

    @DeleteMapping("/me/{spaceId}")
    @Operation(summary = "스페이스 탈퇴")
    public RsData<ResBodyForSpaceSave> leaveSpace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId
    ) {
        Member requester = userDetails.getMember();
        Space space = spaceService.findById(spaceId);

        membershipService.leaveSpace(requester, space);

        return new RsData<>(
                "200",
                "스페이스에서 탈퇴했습니다.",
                new ResBodyForSpaceSave(
                        space.getId(),
                        space.getName()
                )
        );
    }
}
