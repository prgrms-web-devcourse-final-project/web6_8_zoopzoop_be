package org.tuna.zoopzoop.backend.domain.member.dto.res;

import org.tuna.zoopzoop.backend.domain.member.entity.Member;

import java.time.LocalDateTime;

public record ResBodyForGetMemberInfoV2(
        Integer id,
        String name,
        String profileUrl,
        String provider,
        LocalDateTime createAt
) {
    public ResBodyForGetMemberInfoV2(Member member){
        this(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                member.getProvider().name(),
                member.getCreateDate()
        );
    }
}
