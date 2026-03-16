package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.LANGUAGE;
import com.jio.consent.dto.PreferenceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class CreateConsentMetaRequest {

    private LANGUAGE languagePreference;

    @Schema(description = "preferencesStatus", example = "{\"PreferenceId1\": \"ACCEPTED\", \"PreferenceId2\": \"NOTACCEPTED\"}")
    @NotEmpty(message = ErrorCodes.JCMP1025)
    Map<String, PreferenceStatus> preferencesStatus;

    @Schema(description = "Whether this is a parental consent. If true, 'relation' field becomes mandatory.", example = "false")
    @Builder.Default
    private Boolean isParentalConsent = false;

    @Schema(description = "Relation with the minor. Required when isParentalConsent is true, not required when isParentalConsent is false.", example = "Father")
    private String relation;

    private String consentHandleId;
    
    private String secId;
    
    private Map<String, Object> additionalInfo;

}

