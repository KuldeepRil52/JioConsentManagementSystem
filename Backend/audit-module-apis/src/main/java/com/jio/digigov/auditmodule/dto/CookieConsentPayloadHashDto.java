package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CookieConsentPayloadHashDto {

    private String consentId;
    private String consentHandleId;
    private String businessId;
    private String templateId;
    private Integer templateVersion;
    private Object languagePreferences;
    private Multilingual multilingual;
    private Object customerIdentifiers;
    private Object preferences;
    private Object status;
    private Object consentStatus;
    private Integer version;
    private Object consentJwtToken;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // MUST stay Instant — keeps nanosecond precision
    private Instant createdAt;
    private Instant updatedAt;

    private Object staleStatus;

    public static CookieConsentPayloadHashDto from(CookieConsentDto consent) {

        return CookieConsentPayloadHashDto.builder()
                .consentId(consent.getConsentId())
                .consentHandleId(consent.getConsentHandleId())
                .businessId(consent.getBusinessId())
                .templateId(consent.getTemplateId())
                .templateVersion(consent.getTemplateVersion())
                .languagePreferences(consent.getLanguagePreferences())
                .multilingual(consent.getMultilingual())
                .customerIdentifiers(consent.getCustomerIdentifiers())
                .preferences(consent.getPreferences())
                .status(consent.getStatus())
                .staleStatus(consent.getStaleStatus())
                .consentStatus(consent.getConsentStatus())
                .version(consent.getVersion())
                .consentJwtToken(consent.getConsentJwtToken())
                .startDate(consent.getStartDate())
                .endDate(consent.getEndDate())
                .createdAt(consent.getCreatedAt())    // keep Instant untouched
                .updatedAt(consent.getUpdatedAt())    // keep Instant untouched
                .build();

    }

    // ---------------- UTC Conversion Helpers ----------------

    private LocalDateTime convertToUtc(LocalDateTime time) {
        if (time == null) return null;
        return time.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    @SuppressWarnings("unchecked")
    private void applyUtcConversion() {

        // Convert only LocalDateTime fields
        if (startDate != null)
            startDate = convertToUtc(startDate);

        if (endDate != null)
            endDate = convertToUtc(endDate);

        // DO NOT convert createdAt or updatedAt — they are Instant now

        // Convert preference timestamps
        if (preferences instanceof List<?>) {
            List<?> prefList = (List<?>) preferences;

            prefList.forEach(pref -> {
                if (pref instanceof Preference) {
                    Preference p = (Preference) pref;

                    if (p.getStartDate() != null)
                        p.setStartDate(convertToUtc(p.getStartDate()));

                    if (p.getEndDate() != null)
                        p.setEndDate(convertToUtc(p.getEndDate()));
                }
            });
        }
    }
}