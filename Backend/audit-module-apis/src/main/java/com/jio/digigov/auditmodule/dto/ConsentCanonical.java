package com.jio.digigov.auditmodule.dto;

import com.jio.digigov.auditmodule.enumeration.ConsentStatus;
import com.jio.digigov.auditmodule.enumeration.LANGUAGE;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentCanonical {

    private String consentId;
    private String consentHandleId;
    private String templateId;
    private String templateName;
    private int templateVersion;
    private String businessId;
    private LANGUAGE languagePreferences;
    private Multilingual multilingual;
    private List<HandlePreference> preferences;
    private CustomerIdentifiers customerIdentifiers;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ConsentStatus status;
    private String parentalKyc;
    private String parentalReferenceId;
    @Builder.Default
    private Boolean isParentalConsent = false;

    public static ConsentCanonical from(Consent c) {
        return ConsentCanonical.builder()
                .consentId(c.getConsentId())
                .consentHandleId(c.getConsentHandleId())
                .templateId(c.getTemplateId())
                .templateName(c.getTemplateName())
                .templateVersion(c.getTemplateVersion())
                .businessId(c.getBusinessId())
                .languagePreferences(c.getLanguagePreferences())
                .multilingual(c.getMultilingual())
                .preferences(c.getPreferences())
                .customerIdentifiers(c.getCustomerIdentifiers())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .status(c.getStatus())
                .parentalKyc(c.getParentalKyc())
                .parentalReferenceId(c.getParentalReferenceId())
                .isParentalConsent(c.getIsParentalConsent())
                .build();
    }
}
