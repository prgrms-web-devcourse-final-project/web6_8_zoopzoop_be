package org.tuna.zoopzoop.backend.domain.member.dto.req;

import jakarta.validation.constraints.NotBlank;

public record ReqBodyForEditMemberName(
        @NotBlank(message = "잘못된 요청입니다.") //MethodArgumentException
        String newName
) {
    public ReqBodyForEditMemberName(String newName){
        this.newName = newName;
    }
}