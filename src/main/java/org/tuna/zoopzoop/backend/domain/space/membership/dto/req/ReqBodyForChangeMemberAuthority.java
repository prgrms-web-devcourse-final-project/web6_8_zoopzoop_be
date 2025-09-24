package org.tuna.zoopzoop.backend.domain.space.membership.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;

public record ReqBodyForChangeMemberAuthority(
        @NotNull
        Authority newAuthority,

        @NotNull
        @PositiveOrZero
        Integer memberId
) {
}
