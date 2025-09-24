package org.tuna.zoopzoop.backend.domain.space.membership.dto;

import java.util.List;

public record ReqBodyForInviteMembers(
        List<String> memberNames
) {
}
