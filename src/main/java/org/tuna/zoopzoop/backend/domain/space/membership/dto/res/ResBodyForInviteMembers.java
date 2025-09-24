package org.tuna.zoopzoop.backend.domain.space.membership.dto.res;

import org.tuna.zoopzoop.backend.domain.member.dto.etc.SimpleUserInfo;

import java.util.List;

public record ResBodyForInviteMembers(
        Integer spaceId,
        String spaceName,
        List<SimpleUserInfo> invitedUsers
) {
}
