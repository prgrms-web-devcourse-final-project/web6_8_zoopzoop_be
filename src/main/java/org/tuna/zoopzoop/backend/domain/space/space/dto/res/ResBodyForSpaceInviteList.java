package org.tuna.zoopzoop.backend.domain.space.space.dto.res;

import org.tuna.zoopzoop.backend.domain.space.space.dto.etc.SpaceInfoWithoutAuthority;
import org.tuna.zoopzoop.backend.domain.space.space.dto.etc.SpaceInvitationInfo;

import java.util.List;

public record ResBodyForSpaceInviteList(
        List<SpaceInvitationInfo> spaces
) {
}
