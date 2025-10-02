package org.tuna.zoopzoop.backend.domain.dashboard.dto;

import java.util.List;
import java.util.Map;

public record ReqBodyForLiveblocksAuth(
        String userId,
        UserInfo userInfo,
        Map<String, List<String>>permissions
) {
    public record UserInfo(
            String name,
            String avatar
    ) {}
}
