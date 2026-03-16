package com.jio.schedular.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "cookie_consents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieConsent {

    @Id
    @JsonProperty("_id")
    private String id; // Unique document ID (changes with each version)

    @Field("consentId")
    @JsonProperty("consentId")
    private String consentId; // Logical consent ID (same across versions)

    @Field("consentHandleId")
    @JsonProperty("consentHandleId")
    private String consentHandleId; // Links to the handle that created this consent

    @Field("businessId")
    @JsonProperty("businessId")
    private String businessId; // IMMUTABLE: Business association

    @Field("templateId")
    @JsonProperty("templateId")
    private String templateId; // IMMUTABLE: Template association

    @Field("templateVersion")
    @JsonProperty("templateVersion")
    private Integer templateVersion; // Template version when consent was created

    @Field("languagePreferences")
    @JsonProperty("languagePreferences")
    private LANGUAGE languagePreferences;

    @Field("multilingual")
    @JsonProperty("multilingual")
    private Multilingual multilingual;

    @Field("customerIdentifiers")
    @JsonProperty("customerIdentifiers")
    private CustomerIdentifiers customerIdentifiers; // IMMUTABLE: Customer association

    @Field("preferences")
    @JsonProperty("preferences")
    private List<Preference> preferences; // User's preference choices

    @Field("status")
    @JsonProperty("status")
    private CookieConsentStatus status; // ACTIVE, EXPIRED, INACTIVE (consent lifecycle)

    @Field("consentStatus")
    @JsonProperty("consentStatus")
    private VersionStatus consentStatus; // ACTIVE, UPDATED (version status)

    @Field("version")
    @JsonProperty("version")
    private Integer version; // Version number (1, 2, 3...)

    @Field("consentJwtToken")
    @JsonProperty("consentJwtToken")
    private String consentJwtToken; // JWT token for this consent version

    @Field("startDate")
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @Field("endDate")
    @JsonProperty("endDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    @Field("createdAt")
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant createdAt;

    @Field("updatedAt")
    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant updatedAt;

    @Field("_class")
    @JsonProperty("_class")
    private String className;

    @Field("encryptionTime")
    @JsonProperty("encryptionTime")
    @Schema(description = "Timestamp when consent was encrypted by vault service")
    private String encryptionTime;

    @Field("encryptedReferenceId")
    @JsonProperty("encryptedReferenceId")
    @Schema(description = "Reference ID from vault encryption API")
    private String encryptedReferenceId;

    @Field("encryptedString")
    @JsonProperty("encryptedString")
    @Schema(description = "Encrypted payload string from vault")
    private String encryptedString;

    @Field("payloadHash")
    @JsonProperty("payloadHash")
    @Schema(description = "SHA-256 hash of consent data (for tamper detection)")
    private String payloadHash;

    @Field("staleStatus")
    @JsonProperty("staleStatus")
    @Schema(description = "Indicates if this version is current (NOT_STALE) or outdated (STALE)")
    private StaleStatus staleStatus;

    @Field("currentChainHash")
    @JsonProperty("currentChainHash")
    @Schema(description = "SHA-256 hash of consent data (for tamper detection)")
    private String currentChainHash;

    public CookieConsent(CookieConsent source) {
        this.id = source.id;
        this.consentId = source.consentId;
        this.consentHandleId = source.consentHandleId;
        this.businessId = source.businessId;
        this.templateId = source.templateId;
        this.templateVersion = source.templateVersion;
        this.languagePreferences = source.languagePreferences;

        // Deep copy multilingual if present
        this.multilingual = source.multilingual != null ?
                new Multilingual(source.multilingual) : null;

        // Deep copy customerIdentifiers if present
        this.customerIdentifiers = source.customerIdentifiers != null ?
                new CustomerIdentifiers(source.customerIdentifiers) : null;

        // Deep copy preferences list
        this.preferences = source.preferences != null ?
                new ArrayList<>(source.preferences) : null;

        this.status = source.status;
        this.consentStatus = source.consentStatus;
        this.version = source.version;
        this.consentJwtToken = source.consentJwtToken;
        this.startDate = source.startDate;
        this.endDate = source.endDate;
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
        this.className = source.className;
        this.encryptionTime = source.encryptionTime;
        this.encryptedReferenceId = source.encryptedReferenceId;
        this.encryptedString = source.encryptedString;
        this.payloadHash = source.payloadHash;
        this.staleStatus = source.staleStatus;
    }

}