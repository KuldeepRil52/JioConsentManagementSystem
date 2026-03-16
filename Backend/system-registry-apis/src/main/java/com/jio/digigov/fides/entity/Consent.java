package com.jio.digigov.fides.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.fides.dto.*;
import com.jio.digigov.fides.enumeration.ConsentStatus;
import com.jio.digigov.fides.enumeration.LANGUAGE;
import com.jio.digigov.fides.enumeration.StaleStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@org.springframework.data.mongodb.core.mapping.Document("consents")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class Consent extends BaseEntity implements Serializable {

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

    public Consent(Consent other) {
        this.consentId = other.consentId;
        this.consentHandleId = other.consentHandleId;
        this.templateId = other.templateId;
        this.templateName = other.templateName;
        this.templateVersion = other.templateVersion;
        this.businessId = other.businessId;
        this.languagePreferences = other.languagePreferences;
        this.multilingual = other.multilingual;
        this.preferences = other.preferences;
        this.customerIdentifiers = other.customerIdentifiers;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.status = other.status;
        this.parentalKyc = other.parentalKyc;
        this.parentalReferenceId = other.parentalReferenceId;
        this.isParentalConsent = other.isParentalConsent;
    }
}
