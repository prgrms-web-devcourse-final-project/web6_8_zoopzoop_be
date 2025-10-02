package org.tuna.zoopzoop.backend.domain.space.membership.dto.res;

import org.tuna.zoopzoop.backend.domain.space.membership.dto.etc.SpaceMemberInfo;

public record ResBodyForChangeMemberAuthority(
        Integer spaceId,
        String spaceName,
        SpaceMemberInfo member
) {
}
