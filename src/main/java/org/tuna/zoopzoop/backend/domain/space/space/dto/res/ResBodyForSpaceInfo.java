package org.tuna.zoopzoop.backend.domain.space.space.dto.res;

public record ResBodyForSpaceInfo (
        Integer spaceId,
        String spaceName,
        String thumbnailUrl,
        String userAuthority,
        Integer sharingArchiveId,
        Integer dashboardId
){
}
