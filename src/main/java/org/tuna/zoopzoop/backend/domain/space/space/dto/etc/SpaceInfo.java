package org.tuna.zoopzoop.backend.domain.space.space.dto.etc;

import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;

public record SpaceInfo(
        Integer id,
        String name,
        String thumbnailUrl,
        Authority authority
) {
}
