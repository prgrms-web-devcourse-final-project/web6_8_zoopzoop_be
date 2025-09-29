package org.tuna.zoopzoop.backend.domain.member.dto.res;

public record ResBodyForEditMember(
        String name,
        String profileUrl
) {
    public ResBodyForEditMember(String name, String profileUrl) {
        this.name = name;
        this.profileUrl = profileUrl;
    }
}
