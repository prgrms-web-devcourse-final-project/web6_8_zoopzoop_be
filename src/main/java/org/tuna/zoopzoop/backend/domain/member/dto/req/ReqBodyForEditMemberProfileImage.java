package org.tuna.zoopzoop.backend.domain.member.dto.req;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ReqBodyForEditMemberProfileImage (
        @NotNull(message = "파일을 선택해주세요.")
        MultipartFile file
) {
    public ReqBodyForEditMemberProfileImage(MultipartFile file) {
        this.file = file;
    }
}
