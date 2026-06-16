package com.example.scanner.dto.request;

import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.PreferenceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a new consent based on user's cookie preferences")
public class CreateConsentRequest {

    @Schema(description = "Unique consent handle ID obtained from consent handle creation",
            example = "dd77a013CXahuXXXXX....",
            required = true)
    @NotBlank(message = "Consent handle ID is required")
    private String consentHandleId;

    @Schema(description = "User's preferred language for consent display. Must match one of the supported languages in the template",
            allowableValues = {"ASSAMESE", "BENGALI", "BODO", "DOGRI", "GUJARATI", "HINDI", "KANNADA", "KASHMIRI",
                    "KONKANI", "MAITHILI", "MALAYALAM", "MANIPURI", "MARATHI", "NEPALI", "ODIA",
                    "PUNJABI", "SANSKRIT", "SANTALI", "SINDHI", "TAMIL", "TELUGU", "URDU", "ENGLISH"})
    private LANGUAGE languagePreference;

    @Schema(
            description = "Map of category names",
            example = "{\"Necessary\": \"ACCEPTED\", \"Analytics\": \"ACCEPTED\"...}",
            required = true
    )
    Map<String, PreferenceStatus> preferencesStatus;
}
