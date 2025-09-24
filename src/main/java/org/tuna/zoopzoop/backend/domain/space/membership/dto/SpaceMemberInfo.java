package org.tuna.zoopzoop.backend.domain.space.membership.dto;

import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;

public record SpaceMemberInfo(
        Integer id,
        String name,
        String profileUrl,
        Authority authority
) {
}
