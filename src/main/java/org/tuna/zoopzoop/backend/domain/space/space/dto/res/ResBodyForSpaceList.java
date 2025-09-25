package org.tuna.zoopzoop.backend.domain.space.space.dto.res;

import org.tuna.zoopzoop.backend.domain.space.space.dto.etc.SpaceMembershipInfo;

import java.util.List;

public record ResBodyForSpaceList(
        List<SpaceMembershipInfo> spaces
) {
}
