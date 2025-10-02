package org.tuna.zoopzoop.backend.domain.space.membership.dto.etc;

import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;

import java.time.LocalDateTime;

public record SpaceMemberInfo(
        Integer id,
        String name,
        String profileUrl,
        Authority authority
) {
}
