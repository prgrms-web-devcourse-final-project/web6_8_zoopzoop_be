package org.tuna.zoopzoop.backend.domain.space.space.dto;


import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ReqBodyForSpaceSave(
        @NotBlank
        @Length(max = 50)
        String name
) {
}
