package com.example.scanner.entity;

import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.dto.Multilingual;
import com.example.scanner.dto.Preference;
import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.StaleStatus;
import com.example.scanner.enums.Status;
import com.example.scanner.enums.VersionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Document(collection = "cookie_consents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cookie consent entity with versioning support")
public class CookieConsent {

    @Id
    @JsonProperty("_id")
    @Schema(description = "Unique MongoDB document ID (changes with each version)",
            example = "507f1f77bcf86cd799439011")
    private String id; // Unique document ID (changes with each version)

    @Field("consentId")
    @JsonProperty("consentId")
    @Schema(description = "Logical consent ID (same across all versions)",
            example = "cst_123e4567-e89b-XXX....")
    private String consentId; // Logical consent ID (same across versions)

    @Field("consentHandleId")
    @JsonProperty("consentHandleId")
    @Schema(description = "Consent handle ID that created this consent",
            example = "cst_123e4567-e89b-XXX....-")
    private String consentHandleId; // Links to the handle that created this consent

    @Field("businessId")
    @JsonProperty("businessId")
    @Schema(description = "Business ID (immutable)",
            example = "bst_123e4567-e89b-XXX....")
    private String businessId; // IMMUTABLE: Business association

    @Field("templateId")
    @JsonProperty("templateId")
    @Schema(description = "Template ID (immutable)",
            example = "st_123e4567-e89b-XXX....")
    private String templateId; // IMMUTABLE: Template association

    @Field("templateVersion")
    @JsonProperty("templateVersion")
    @Schema(description = "Template version when consent was created", example = "1")
    private Integer templateVersion; // Template version when consent was created

    @Field("languagePreferences")
    @JsonProperty("languagePreferences")
    @Schema(description = "User's language preference", example = "ENGLISH")
    private LANGUAGE languagePreferences;

    @Field("multilingual")
    @JsonProperty("multilingual")
    @Schema(description = "Multilingual configuration from template")
    private Multilingual multilingual;

    @Field("customerIdentifiers")
    @JsonProperty("customerIdentifiers")
    @Schema(description = "Customer identification (immutable)")
    private CustomerIdentifiers customerIdentifiers; // IMMUTABLE: Customer association

    @Field("preferences")
    @JsonProperty("preferences")
    @Schema(description = "User's cookie preferences with statuses")
    private List<Preference> preferences; // User's preference choices

    @Field("status")
    @JsonProperty("status")
    @Schema(description = "Consent lifecycle status (ACTIVE, EXPIRED, REVOKED)",
            example = "ACTIVE")
    private Status status; // ACTIVE, EXPIRED, INACTIVE (consent lifecycle)

    @Field("consentStatus")
    @JsonProperty("consentStatus")
    @Schema(description = "Version status (ACTIVE, UPDATED)",
            example = "ACTIVE")
    private VersionStatus consentStatus; // ACTIVE, UPDATED (version status)

    @Field("version")
    @JsonProperty("version")
    @Schema(description = "Version number (increments with each update)",
            example = "1")
    private Integer version; // Version number (1, 2, 3...)

    @Field("consentJwtToken")
    @JsonProperty("consentJwtToken")
    @Schema(description = "JWT token for this consent version")
    private String consentJwtToken; // JWT token for this consent version

    @Field("startDate")
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Schema(description = "Consent start date",
            example = "2025-01-15T10:30:00")
    private LocalDateTime startDate;

    @Field("endDate")
    @JsonProperty("endDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Schema(description = "Consent expiry date (earliest preference expiry)",
            example = "2026-01-15T10:30:00")
    private LocalDateTime endDate;

    @Field("createdAt")
    @JsonProperty("createdAt")
    @Schema(description = "Timestamp when this version was created",
            example = "2025-01-15T10:30:00.123Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant createdAt;

    @Field("updatedAt")
    @JsonProperty("updatedAt")
    @Schema(description = "Timestamp when this version was last updated",
            example = "2025-01-15T10:30:00.123Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant updatedAt;

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

    // Constructor for new consent creation (version 1)
    public CookieConsent(String consentHandleId, String businessId, String templateId, Integer templateVersion,
                         LANGUAGE languagePreferences, Multilingual multilingual, CustomerIdentifiers customerIdentifiers,
                         List<Preference> preferences, Status status, LocalDateTime startDate, LocalDateTime endDate) {
        this.consentId = UUID.randomUUID().toString(); // Generate logical consent ID
        this.consentHandleId = consentHandleId;
        this.businessId = businessId;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.languagePreferences = languagePreferences;
        this.multilingual = multilingual;
        this.customerIdentifiers = customerIdentifiers;
        this.preferences = preferences;
        this.status = status;
        this.consentStatus = VersionStatus.ACTIVE; // First version is always active
        this.version = 1; // First version
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CookieConsent that = (CookieConsent) o;

        return Objects.equals(consentId, that.consentId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(templateId, that.templateId) &&
                Objects.equals(templateVersion, that.templateVersion) &&
                Objects.equals(businessId, that.businessId) &&
                Objects.equals(customerIdentifiers, that.customerIdentifiers) &&
                Objects.equals(preferences, that.preferences) &&
                Objects.equals(status, that.status) &&
                Objects.equals(consentStatus, that.consentStatus) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consentId, version, templateId, templateVersion,
                businessId, customerIdentifiers, status, consentStatus);
    }

    public CookieConsent(CookieConsent other) {
        this.consentId = other.consentId;
        this.consentHandleId = other.consentHandleId;
        this.templateId = other.templateId;
        this.templateVersion = other.templateVersion;
        this.businessId = other.businessId;
        this.languagePreferences = other.languagePreferences;
        this.multilingual = other.multilingual;
        this.preferences = other.preferences;
        this.customerIdentifiers = other.customerIdentifiers;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.status = other.status;
        this.consentStatus = other.consentStatus;
    }
}