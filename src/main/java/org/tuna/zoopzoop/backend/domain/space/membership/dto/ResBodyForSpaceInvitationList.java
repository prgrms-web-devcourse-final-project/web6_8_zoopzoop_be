package org.tuna.zoopzoop.backend.domain.space.membership.dto;

import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForGetMemberInfo;

import java.util.List;

public record ResBodyForSpaceInvitationList(
        Integer spaceId,
        List<ResBodyForGetMemberInfo> invitedUsers
) {
}
