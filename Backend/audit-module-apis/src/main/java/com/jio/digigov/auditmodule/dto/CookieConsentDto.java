package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.digigov.auditmodule.enumeration.CookieConsentStatus;
import com.jio.digigov.auditmodule.enumeration.LANGUAGE;
import com.jio.digigov.auditmodule.enumeration.StaleStatus;
import com.jio.digigov.auditmodule.enumeration.VersionStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CookieConsentDto {

    @JsonProperty("_id")
    private String id;

    private String consentId;
    private String consentHandleId;
    private String businessId;
    private String templateId;
    private Integer templateVersion;

    private LANGUAGE languagePreferences;
    private Multilingual multilingual;
    private CustomerIdentifiersForCookie customerIdentifiers;

    private List<Preference> preferences;

    private CookieConsentStatus status;
    private StaleStatus staleStatus;

    private String consentJsonString;
    private String encryptedReferenceId;
    private String payloadHash;
    private String currentChainHash;

    private VersionStatus consentStatus;
    private Integer version;
    private String consentJwtToken;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    private LocalDateTime endDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant updatedAt;
}