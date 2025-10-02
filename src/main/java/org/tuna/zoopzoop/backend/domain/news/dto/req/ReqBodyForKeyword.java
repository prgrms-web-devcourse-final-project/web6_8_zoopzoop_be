package org.tuna.zoopzoop.backend.domain.news.dto.req;

import java.util.List;

public record ReqBodyForKeyword (
        List<String> keywords
) {
}
