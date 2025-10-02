package org.tuna.zoopzoop.backend.domain.datasource.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;


public record IdsRequest (
        @NotEmpty(message = "dataSourceId 배열은 비어있을 수 없습니다.")
        List<Integer> ids
){}
