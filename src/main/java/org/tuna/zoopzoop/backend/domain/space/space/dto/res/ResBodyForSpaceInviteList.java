package org.tuna.zoopzoop.backend.domain.space.space.dto.res;

import org.tuna.zoopzoop.backend.domain.space.space.dto.etc.SpaceMembershipInfoWithoutAuthority;

import java.util.List;

public record ResBodyForSpaceInviteList(
        List<SpaceMembershipInfoWithoutAuthority> spaces
) {
}
