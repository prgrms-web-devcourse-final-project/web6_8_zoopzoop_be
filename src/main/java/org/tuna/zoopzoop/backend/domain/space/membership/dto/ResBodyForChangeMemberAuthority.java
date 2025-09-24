package org.tuna.zoopzoop.backend.domain.space.membership.dto;

public record ResBodyForChangeMemberAuthority(
        Integer spaceId,
        String spaceName,
        SpaceMemberInfo member
) {
}
