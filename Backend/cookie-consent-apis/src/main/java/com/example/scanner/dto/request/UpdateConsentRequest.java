package com.example.scanner.dto.request;

import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.PreferenceStatus;
import com.example.scanner.enums.Status;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to update an existing consent (creates new version)")
public class UpdateConsentRequest {

    @Schema(description = "Consent handle ID for this update operation",
            example = "handle_123e4-XXXXXX.......", required = true)
    @NotNull(message = "Consent handle ID is required for updates")
    private String consentHandleId;

    @Schema(
            description = "Updated language preference - must be one of LANGUAGE enum values",
            allowableValues = {"ASSAMESE", "BENGALI", "BODO", "DOGRI", "GUJARATI", "HINDI", "KANNADA", "KASHMIRI", "KONKANI", "MAITHILI", "MALAYALAM", "MANIPURI", "MARATHI", "NEPALI", "ODIA", "PUNJABI", "SANSKRIT", "SANTALI", "SINDHI", "TAMIL", "TELUGU", "URDU", "ENGLISH"}
    )
    private LANGUAGE languagePreference;

    @Schema(
            description = "Map of category names",
            example = "{\"Necessary\": \"ACCEPTED\", \"Analytics\": \"ACCEPTED\"...}"
    )
    private Map<String, PreferenceStatus> preferencesStatus;

    @Schema(
            description = "Optional status to revoke consent. Only REVOKED is allowed. When provided, only the status is updated without creating a new version.",
            allowableValues = {"REVOKED"}
    )
    private Status status;

    public boolean hasUpdates() {
        return languagePreference != null ||
                (preferencesStatus != null && !preferencesStatus.isEmpty()) ||
                status != null;
    }

    public boolean hasPreferenceUpdates() {
        return preferencesStatus != null && !preferencesStatus.isEmpty();
    }

}