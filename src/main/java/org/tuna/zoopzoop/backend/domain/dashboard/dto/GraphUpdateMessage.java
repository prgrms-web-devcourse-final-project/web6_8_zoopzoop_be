package org.tuna.zoopzoop.backend.domain.dashboard.dto;

public record GraphUpdateMessage(
        Integer dashboardId,
        String requestBody
){
}
