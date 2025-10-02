package org.tuna.zoopzoop.backend.domain.member.dto.res;

public record ResBodyForEditMemberProfileImage(
        String profileUrl
) {
    public ResBodyForEditMemberProfileImage(String profileUrl) {
        this.profileUrl = profileUrl;
    }
}
