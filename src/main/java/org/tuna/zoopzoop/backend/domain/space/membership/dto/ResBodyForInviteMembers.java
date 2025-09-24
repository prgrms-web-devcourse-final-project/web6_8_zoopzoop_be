package org.tuna.zoopzoop.backend.domain.space.membership.dto;

import org.tuna.zoopzoop.backend.domain.member.dto.etc.SimpleUserInfo;
import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForGetMemberInfo;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;

import java.util.List;

public record ResBodyForInviteMembers(
        Integer spaceId,
        String spaceName,
        List<SimpleUserInfo> invitedUsers
) {
}
