package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.BulkConsentPreferenceAction;
import com.jio.consent.dto.BulkConsentUploadStatus;
import com.jio.consent.dto.IdentityType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class BulkConsentUpload {

    private String txnId;

    private String templateId;

    private String templateVersion;

    private IdentityType identityType;

    private String identityValue;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime requestTimestamp;

    private List<BulkConsentPreference> preferences;

    @Builder.Default
    private BulkConsentUploadStatus status = BulkConsentUploadStatus.PENDING;

    private String consentHandleId;

    private String consentId;

    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BulkConsentPreference {
        private String preferenceId;
        private BulkConsentPreferenceAction action;
    }
}
