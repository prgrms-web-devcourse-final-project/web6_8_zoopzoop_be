package org.tuna.zoopzoop.backend.domain.member.dto.res;

import org.tuna.zoopzoop.backend.domain.member.entity.MemberDocument;

public record ResBodyForSearchMember(
    int id,
    String name
) {
    public ResBodyForSearchMember(int id, String name) {
        this.id = id;
        this.name = name;
    }
    public ResBodyForSearchMember(MemberDocument memberDocument){
        this(memberDocument.getId(), memberDocument.getName());
    }
}