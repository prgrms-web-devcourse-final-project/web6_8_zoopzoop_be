package org.tuna.zoopzoop.backend.domain.space.membership.dto;

import java.util.List;

public record ResBodyForSpaceMemberList(
        Integer spaceId,
        String spaceName,
        List<SpaceMemberInfo> members
) {
}
