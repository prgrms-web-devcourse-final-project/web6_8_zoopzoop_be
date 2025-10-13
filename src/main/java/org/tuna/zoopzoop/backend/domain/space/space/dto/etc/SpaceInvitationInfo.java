package org.tuna.zoopzoop.backend.domain.space.space.dto.etc;

public record SpaceInvitationInfo(
        Integer spaceId,
        String spaceName,
        String spaceThumbnailUrl,
        Integer inviteId
) {
}
