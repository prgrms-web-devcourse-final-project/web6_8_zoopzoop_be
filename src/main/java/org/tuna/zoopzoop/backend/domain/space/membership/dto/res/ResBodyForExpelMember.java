package org.tuna.zoopzoop.backend.domain.space.membership.dto.res;

import org.tuna.zoopzoop.backend.domain.member.dto.res.ResBodyForGetMemberInfo;

public record ResBodyForExpelMember(
        Integer spaceId,
        String spaceName,
        ResBodyForGetMemberInfo expelledMemberInfo
) {
}
