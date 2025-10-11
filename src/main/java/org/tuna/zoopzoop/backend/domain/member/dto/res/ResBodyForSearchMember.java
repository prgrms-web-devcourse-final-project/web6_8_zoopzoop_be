package org.tuna.zoopzoop.backend.domain.member.dto.res;

import org.tuna.zoopzoop.backend.domain.member.entity.MemberDocument;

public record ResBodyForSearchMember(
    int id,
    String name,
    String profileImageUrl
) {
    public ResBodyForSearchMember(int id, String name, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }
    public ResBodyForSearchMember(MemberDocument memberDocument){
        this(memberDocument.getId(), memberDocument.getName(), memberDocument.getProfileImageUrl());
    }
}