package com.jio.consent.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.LANGUAGE;
import com.jio.consent.dto.PreferenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ConsentMetaResponse {

    private String consentMetaId;
    private LANGUAGE languagePreference;
    private Map<String, PreferenceStatus> preferencesStatus;
    private Boolean isParentalConsent;
    private String relation;
    private String consentHandleId;
    private String secId;
    private Map<String, Object> additionalInfo;
    private String message;

}

