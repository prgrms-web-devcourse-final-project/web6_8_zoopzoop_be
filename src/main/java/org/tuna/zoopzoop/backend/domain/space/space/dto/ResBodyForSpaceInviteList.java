package org.tuna.zoopzoop.backend.domain.space.space.dto;

import java.util.List;

public record ResBodyForSpaceInviteList(
        List<SpaceMembershipInfoWithoutAuthority> spaces
) {
}
