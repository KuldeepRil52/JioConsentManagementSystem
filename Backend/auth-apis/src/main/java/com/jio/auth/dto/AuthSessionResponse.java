package com.jio.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSessionResponse {
    private String tenantId;
    private String businessId;
    private String userId;
    private String sessionId;
    private String jwt;
    private String refreshToken;
    private Date expiresAt;
}
