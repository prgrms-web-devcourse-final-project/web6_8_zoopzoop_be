package org.tuna.zoopzoop.backend.domain.member.dto.res;

import org.tuna.zoopzoop.backend.domain.member.entity.Member;

public record ResBodyForGetMemberInfoV2(
        Integer id,
        String name,
        String profileUrl,
        String provider
) {
    public ResBodyForGetMemberInfoV2(Member member){
        this(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                member.getProvider().name()
        );
    }
}
