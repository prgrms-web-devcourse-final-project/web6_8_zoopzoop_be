package org.tuna.zoopzoop.backend.domain.space.space.dto.etc;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.tuna.zoopzoop.backend.domain.space.membership.dto.etc.SpaceMemberInfo;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpaceInfo(
        Integer id,
        String name,
        String thumbnailUrl,
        Authority authority,
        List<SpaceMemberInfo> members
) {
    public SpaceInfo(Integer id, String name, String thumbnailUrl, Authority authority) {
        this(id, name, thumbnailUrl, authority, null);
    }
}
