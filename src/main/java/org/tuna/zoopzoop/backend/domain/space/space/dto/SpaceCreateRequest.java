package org.tuna.zoopzoop.backend.domain.space.space.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record SpaceCreateRequest(
        @NotBlank
        @Length(max = 20)
        String name
) { }
