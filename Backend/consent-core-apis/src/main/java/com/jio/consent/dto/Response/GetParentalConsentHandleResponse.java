package com.jio.consent.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class GetParentalConsentHandleResponse {

    private String consentHandleId;
    private String templateId;
    private String templateName;
    private int templateVersion;
    private String businessId;
    private Multilingual multilingual;
    private List<HandlePreference> preferences;
    private UiConfig uiConfig;
    private DocumentMeta documentMeta;
    private CustomerIdentifiers customerIdentifiers;
    private ConsentHandleStatus status;
    private ConsentHandleRemarks remarks;

    // Parental consent fields
    private Boolean isParental;
    private String parentIdentity;
    private String parentIdentityType;
    private String parentName;
}

