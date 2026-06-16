package com.example.scanner.dto;

import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.Status;
import com.example.scanner.enums.VersionStatus;
import com.example.scanner.enums.StaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Signable version of CookieConsent - Used ONLY for JWS signing and verification
 * Does NOT contain encryption-related fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignableConsent {

    private String consentId;
    private String consentHandleId;
    private String businessId;
    private String templateId;
    private Integer templateVersion;
    private LANGUAGE languagePreferences;
    private Multilingual multilingual;
    private CustomerIdentifiers customerIdentifiers;
    private List<Preference> preferences;
    private Status status;
    private VersionStatus consentStatus;
    private Integer version;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Instant createdAt;
    private Instant updatedAt;
    private StaleStatus staleStatus;
}