package org.tuna.zoopzoop.backend.domain.space.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.JoinState;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.dto.req.ReqBodyForSpaceSave;
import org.tuna.zoopzoop.backend.domain.space.space.dto.res.ResBodyForSpaceInfo;
import org.tuna.zoopzoop.backend.domain.space.space.dto.res.ResBodyForSpaceList;
import org.tuna.zoopzoop.backend.domain.space.space.dto.etc.SpaceMembershipInfo;
import org.tuna.zoopzoop.backend.domain.space.space.dto.res.ResBodyForSpaceSave;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.global.rsData.RsData;
import org.tuna.zoopzoop.backend.global.security.jwt.CustomUserDetails;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/space")
@RequiredArgsConstructor
@Tag(name = "ApiV1SpaceController", description = "스페이스 관련 API")
public class ApiV1SpaceController {
    private final SpaceService spaceService;
    private final MembershipService membershipService;

    @PostMapping
    @Operation(summary = "스페이스 생성")
    public RsData<ResBodyForSpaceSave> createClub(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReqBodyForSpaceSave reqBody
    ){
        Space newSpace = spaceService.createSpace(reqBody.name());

        // ADMIN으로 입력
        Member member = userDetails.getMember();
        membershipService.addMemberToSpace(member, newSpace, Authority.ADMIN);

        return new RsData<>(
                "201",
                String.format("%s - 스페이스가 생성됐습니다.", newSpace.getName()),
                new ResBodyForSpaceSave(
                        newSpace.getId(),
                        newSpace.getName()
                )
        );
    }


    @DeleteMapping("/{spaceId}")
    @Operation(summary = "스페이스 삭제")
    public RsData<Void> deleteSpace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId
    ) throws AccessDeniedException {
        // ADMIN 권한 체크
        Member member = userDetails.getMember();
        if(!membershipService.isMemberAdminInSpace(member, spaceService.findById(spaceId)))
            throw new AccessDeniedException("스페이스의 ADMIN 권한이 필요합니다.");

        String deletedSpaceName = spaceService.deleteSpace(spaceId);

        return new RsData<>(
                "200",
                String.format("%s - 스페이스가 삭제됐습니다.", deletedSpaceName),
                null
        );
    }

    @PutMapping("/{spaceId}")
    @Operation(summary = "스페이스 이름 변경")
    public RsData<ResBodyForSpaceSave> updateSpaceName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId,
            @Valid @RequestBody ReqBodyForSpaceSave reqBody
    ) throws AccessDeniedException {
        // ADMIN 권한 체크
        Member member = userDetails.getMember();
        if(!membershipService.isMemberAdminInSpace(member, spaceService.findById(spaceId)))
            throw new AccessDeniedException("스페이스의 ADMIN 권한이 필요합니다.");

        Space updatedSpace = spaceService.updateSpaceName(spaceId, reqBody.name());

        return new RsData<>(
                "200",
                String.format("%s - 스페이스 이름이 변경됐습니다.", updatedSpace.getName()),
                new ResBodyForSpaceSave(
                        updatedSpace.getId(),
                        updatedSpace.getName()
                )
        );
    }

    @PutMapping(path = "/thumbnail/{spaceId}", consumes = {"multipart/form-data"})
    @Operation(summary = "스페이스 썸네일 이미지 갱신")
    public RsData<Void> updateSpaceThumbnail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Member member = userDetails.getMember();

        spaceService.updateSpaceThumbnail(spaceId, member, image);

        return new RsData<>(
                "200",
                "스페이스 썸네일 이미지가 갱신됐습니다.",
                null
        );
    }

    @GetMapping
    @Operation(summary = "나의 스페이스 목록 조회")
    public RsData<ResBodyForSpaceList> getAllSpaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) JoinState state
    ) {
        // 현재 로그인한 사용자 정보 가져오기
        Member member = userDetails.getMember();

        // 멤버가 속한 스페이스 목록 조회
        List<Membership> memberships;
        if (state == null) {
            memberships = membershipService.findByMember(member, "ALL");
        }
        else {
            memberships = membershipService.findByMember(member, state.name());
        }

        // 반환 값 생성
        List<SpaceMembershipInfo> spaceInfos = memberships.stream()
                .map(membership -> new SpaceMembershipInfo(
                        membership.getSpace().getId(),
                        membership.getSpace().getName(),
                        membership.getSpace().getThumbnailUrl(),
                        membership.getAuthority()
                ))
                .collect(Collectors.toList());
        ResBodyForSpaceList resBody = new ResBodyForSpaceList(spaceInfos);

        return new RsData<>(
                "200",
                "스페이스 목록이 조회됐습니다.",
                resBody
        );
    }

    @GetMapping("/{spaceId}")
    @Operation(summary = "스페이스 단건 조회")
    public RsData<ResBodyForSpaceInfo> getSpace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer spaceId
    ) {
        Member member = userDetails.getMember();
        Space space = spaceService.findById(spaceId);

        // 해당 스페이스에 속한 멤버인지 확인
        Membership membership = membershipService.findByMemberAndSpace(member, space);

        ResBodyForSpaceInfo resBody = new ResBodyForSpaceInfo(
                space.getId(),
                space.getName(),
                space.getThumbnailUrl(),
                membership.getAuthority().name(),
                space.getSharingArchive().getId()
        );

        return new RsData<>(
                "200",
                String.format("%s - 스페이스가 조회됐습니다.", space.getName()),
                resBody
        );
    }


}
