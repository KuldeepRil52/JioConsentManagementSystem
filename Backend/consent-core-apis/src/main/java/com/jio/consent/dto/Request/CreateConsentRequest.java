package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.LANGUAGE;
import com.jio.consent.dto.ParentalKycType;
import com.jio.consent.dto.PreferenceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class  CreateConsentRequest {

    @NotBlank(message = ErrorCodes.JCMP1023)
    private String consentHandleId;

    private LANGUAGE languagePreference;

    @Schema(description = "preferencesStatus", example = "{\"PreferenceId1\": \"ACCEPTED\", \"PreferenceId2\": \"NOTACCEPTED\"}")
    @NotEmpty(message = ErrorCodes.JCMP1025)
    Map<String, PreferenceStatus> preferencesStatus;

    @Schema(description = "Whether this is a parental consent", example = "false")
    @Builder.Default
    private Boolean isParentalConsent = false;

    @Schema(description = "Type of parental KYC", example = "DIGILOCKER")
    private ParentalKycType parentalKYCType;

    @Schema(description = "Code for parental KYC verification", example = "abc123")
    private String code;

    @Schema(description = "State for parental KYC verification", example = "xyz789")
    private String state;

    @AssertTrue(message = ErrorCodes.JCMP1042)
    private boolean isValidParentalConsentFields() {
        // If isParentalConsent is explicitly set to true, then parentalKYCType, code, and state must be provided
        if (Boolean.TRUE.equals(isParentalConsent)) {
            return parentalKYCType != null &&
                   code != null && !code.trim().isEmpty() &&
                   state != null && !state.trim().isEmpty();
        }
        return true; // If isParentalConsent is false, null, or not provided, validation passes
    }

}
