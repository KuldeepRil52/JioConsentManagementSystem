package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jio.digigov.auditmodule.enumeration.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CookieConsentCanonical {

    private String consentId;
    private String consentHandleId;
    private String businessId;
    private String templateId;
    private int templateVersion;
    private LANGUAGE languagePreferences;
    private Multilingual multilingual;
    private CustomerIdentifiersForCookie customerIdentifiers;
    private List<Preference> preferences;
    private CookieConsentStatus status;
    private VersionStatus consentStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    private LocalDateTime endDate;


    // ===== Factory Method =====
    public static CookieConsentCanonical from(CookieConsentDto c) {
        return CookieConsentCanonical.builder()
                .consentId(c.getConsentId())
                .consentHandleId(c.getConsentHandleId())
                .businessId(c.getBusinessId())
                .templateId(c.getTemplateId())
                .templateVersion(c.getTemplateVersion())
                .languagePreferences(c.getLanguagePreferences())
                .multilingual(c.getMultilingual())
                .preferences(c.getPreferences())
                .customerIdentifiers(c.getCustomerIdentifiers())
                .status(c.getStatus())
                .consentStatus(c.getConsentStatus())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .build();
    }


    // ===== Convert LocalDateTime to UTC =====
    private LocalDateTime convertToUtc(LocalDateTime time) {
        if (time == null) return null;
        return time.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }


    // ===== Apply conversion to all fields =====
    private void applyUtcConversion() {

        this.startDate = convertToUtc(this.startDate);
        this.endDate = convertToUtc(this.endDate);

        if (this.preferences != null) {
            this.preferences.forEach(pref -> {
                if (pref.getStartDate() != null) {
                    pref.setStartDate(convertToUtc(pref.getStartDate()));
                }
                if (pref.getEndDate() != null) {
                    pref.setEndDate(convertToUtc(pref.getEndDate()));
                }
            });
        }
    }
}