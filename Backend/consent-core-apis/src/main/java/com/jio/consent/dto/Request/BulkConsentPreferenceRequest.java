package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.BulkConsentPreferenceAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkConsentPreferenceRequest {

    @NotBlank(message = ErrorCodes.JCMP1007)
    @Schema(description = "Preference ID", example = "276767289982892889")
    private String preferenceId;

    @NotNull(message = ErrorCodes.JCMP1053)
    @Schema(description = "Action for the preference", example = "ACCEPT", allowableValues = {"ACCEPT", "REJECT"})
    private BulkConsentPreferenceAction action;
}
