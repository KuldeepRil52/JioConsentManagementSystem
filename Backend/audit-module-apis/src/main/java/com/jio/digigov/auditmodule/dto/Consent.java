package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.auditmodule.enumeration.ConsentStatus;
import com.jio.digigov.auditmodule.enumeration.LANGUAGE;
import com.jio.digigov.auditmodule.enumeration.StaleStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class Consent  {

    @Id
    private ObjectId id;

    private String consentId;
    private String consentHandleId;
    private String templateId;
    private String templateName;
    private int templateVersion;
    private String businessId;
    private LANGUAGE languagePreferences;
    private Multilingual multilingual;
    private List<HandlePreference> preferences;
    CustomerIdentifiers customerIdentifiers;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime endDate;

    private ConsentStatus status;
    private StaleStatus staleStatus;
    private String consentJsonString;
    private String encryptedReferenceId;
    private String payloadHash;
    private String currentChainHash;

    private String parentalKyc;
    private String parentalReferenceId;
    @Builder.Default
    private Boolean isParentalConsent = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

