package org.tuna.zoopzoop.backend.domain.member.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ReqBodyForEditMember (
        @NotBlank(message = "잘못된 요청입니다.") //MethodArgumentException
        String newName,
        @NotNull(message = "파일을 선택해주세요.")
        MultipartFile file
) {
    public ReqBodyForEditMember(String newName, MultipartFile file) {
        this.newName = newName;
        this.file = file;
    }
}