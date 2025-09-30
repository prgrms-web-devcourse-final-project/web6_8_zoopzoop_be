package org.tuna.zoopzoop.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResultData implements Serializable {
    private String accessToken;
    private String sessionId;
}