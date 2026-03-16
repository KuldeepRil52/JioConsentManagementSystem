package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response containing consent handle ID and secure code details")
public class GetConsentHandleAndSecureCodeResponse {

    @Schema(
            description = "Unique consent handle ID",
            example = "9bb14c63-7ec8-47f5-86b5-4a8c848012c1"
    )
    private String consentHandleId;

    @Schema(
            description = "Secure code from external API",
            example = "fa0b0f24-6419-4d80-bd2b-d9493bc48b6c"
    )
    private String secureCode;

    @Schema(
            description = "Identity value used for secure code",
            example = "9324901354"
    )
    private String identity;

    @Schema(
            description = "Expiry timestamp of secure code in milliseconds",
            example = "1763831397284"
    )
    private Long expiry;

    @Schema(
            description = "Template ID",
            example = "tpl_123e4567-e89b-12d3-a456-426614174000"
    )
    private String templateId;

    @Schema(
            description = "Template version",
            example = "1"
    )
    private int templateVersion;

    @Schema(
            description = "Success message",
            example = "Consent handle and secure code created successfully"
    )
    private String message;
}