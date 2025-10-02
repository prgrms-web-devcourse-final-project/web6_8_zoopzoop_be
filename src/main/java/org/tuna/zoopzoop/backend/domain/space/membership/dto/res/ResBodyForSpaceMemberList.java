package org.tuna.zoopzoop.backend.domain.space.membership.dto.res;

import org.tuna.zoopzoop.backend.domain.space.membership.dto.etc.SpaceMemberInfo;

import java.util.List;

public record ResBodyForSpaceMemberList(
        Integer spaceId,
        String spaceName,
        List<SpaceMemberInfo> members
) {
}
