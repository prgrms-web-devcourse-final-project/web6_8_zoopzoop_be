package org.tuna.zoopzoop.backend.domain.member.dto.res;

import org.tuna.zoopzoop.backend.domain.member.entity.Member;

public record ResBodyForGetMemberInfo(
        Integer id,
        String name,
//        String email,
        String profileUrl
) {
    public ResBodyForGetMemberInfo(Member member){
        this(
                member.getId(),
                member.getName(),
//                member.getEmail(),
                member.getProfileImageUrl()
        );
    }
}
