package com.example.scanner.dto.response;

import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.dto.Multilingual;
import com.example.scanner.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response after successfully creating a consent")
public class ConsentCreateResponse {
    private String id;

    @Schema(
            description = "Logical consent ID (remains same across versions)",
            example = "123e4567-XXXX...."
    )
    private String consentId;

    private String secureCode;
    private String templateId;
    private Integer templateVersion;
    private String businessId;
    private String languagePreferences;
    private Multilingual multilingual;
    private List<String> preferences;
    private CustomerIdentifiers customerIdentifiers;

    @Schema(
            description = "Start date of consent",
            example = "2025-10-07T07:33:33.554Z"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime startDate;

    @Schema(
            description = "End date of consent",
            example = "2026-10-07T07:33:33.554Z"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime endDate;

    private Status status;

    @Schema(
            description = "JWT token for this consent - use for validation",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX"
    )
    private String consentJwtToken;

    @Schema(
            description = "Timestamp when consent was created",
            example = "2025-09-29T10:30:00.123Z"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;

    private String className;

    @Schema(
            description = "Success message",
            example = "Consent created successfully!"
    )
    private String message;

    @Schema(
            description = "Expiry date and time of this consent",
            example = "2026-09-29T10:30:00.123Z"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime consentExpiry;

    @Schema(description = "JWS token from vault service for consent verification (NOT stored in DB)",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX.....")
    private String jwsToken;
}