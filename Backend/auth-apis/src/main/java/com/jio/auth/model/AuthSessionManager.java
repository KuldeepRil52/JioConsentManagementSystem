package com.jio.auth.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "auth_session_manager")
public class AuthSessionManager {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String businessId;

    @Indexed
    private String userId;

    @Indexed
    private String identityType;           // e.g. "email", "mobile"
    @Indexed
    private String identityValue;          // normalized (lowercase, trimmed)

    @Indexed(unique = true)
    private UUID sessionId;                // Unique per login session

    @Indexed(unique = true)
    private String refreshTokenHash;       // Hashed (bcrypt/HMAC) token

    private String previousRefreshTokenHash;

    private Date createdAt;
    private Date lastRotatedAt;

    private Date absoluteExpiry;           // login + 7 days

    private Integer accessExpirySeconds;   // e.g. 900 (15m)

    private Boolean revoked;
    private String revokedReason;
    private Integer rotationCount;

    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;

    private Metadata metadata;

    // ---- Nested Metadata class ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String createdBy;
        private String notes;
    }

    // ---- Optional utility log helper ----
    public void logSessionInfo() {
        log.info("Session [{}] for user [{}] (tenant: {}, business: {}) status: {}",
                sessionId, userId, tenantId, businessId,
                revoked != null && revoked ? "REVOKED" : "ACTIVE");
    }
}
