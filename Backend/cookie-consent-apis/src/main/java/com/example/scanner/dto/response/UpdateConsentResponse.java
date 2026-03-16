package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for consent update operation")
public class UpdateConsentResponse {

    @Schema(description = "Logical consent ID (same across versions)",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX-XXX....")
    private String consentId;

    @Schema(description = "New document ID for this version",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX.....")
    private String newVersionId;

    @Schema(description = "New version number", example = "2")
    private Integer newVersion;

    @Schema(description = "Previous version number", example = "1")
    private Integer previousVersion;

    @Schema(description = "JWT token for the new consent version")
    private String consentJwtToken;

    @Schema(
            description = "New expiry date for updated consent",
            example = "2026-09-29T10:30:00"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime consentExpiry;

    @Schema(
            description = "Success message",
            example = "Consent updated successfully. New version 2 created."
    )
    private String message;

    @Schema(
            description = "Timestamp of the update",
            example = "2025-09-29T10:30:00.123Z"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "JWS token from vault service for consent verification (NOT stored in DB)",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX.....")
    private String jwsToken;

    public static UpdateConsentResponse success(String consentId, String newVersionId,
                                                Integer newVersion, Integer previousVersion,
                                                String jwtToken, LocalDateTime expiry, String jwsToken) {
        return UpdateConsentResponse.builder()
                .consentId(consentId)
                .newVersionId(newVersionId)
                .newVersion(newVersion)
                .previousVersion(previousVersion)
                .consentJwtToken(jwtToken)
                .consentExpiry(expiry)
                .message("Consent updated successfully. New version " + newVersion + " created.")
                .updatedAt(Instant.now())
                .jwsToken(jwsToken)
                .build();
    }
}