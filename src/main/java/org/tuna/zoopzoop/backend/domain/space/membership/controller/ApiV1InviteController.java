package org.tuna.zoopzoop.backend.domain.space.membership.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
@Tag(name = "ApiV1MembershipController", description = "사용자에게 온 스페이스 초대 관리 API")
public class ApiV1InviteController {
    private final MembershipService membershipService;

}
