package com.example.scanner.entity;

import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.enums.ConsentHandleStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cookie_consent_handles")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CookieConsentHandle {

    @Id
    private String id;

    @Field("consentHandleId")
    private String consentHandleId;

    @Field("businessId")
    private String businessId;

    @Field("txnId")
    private String txnId;

    @Field("templateId")
    private String templateId;

    @Field("url")
    private String url;

    @Field("templateVersion")
    private int templateVersion;

    @Field("customerIdentifiers")
    private CustomerIdentifiers customerIdentifiers;

    @Field("status")
    private ConsentHandleStatus status;

    @Field("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant createdAt;

    @Field("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant updatedAt;

    @Field("expiresAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant expiresAt;

    @Field("_class")
    private String className;

    public CookieConsentHandle(String consentHandleId, String businessId, String txnId,
                               String templateId, int templateVersion, String url, CustomerIdentifiers customerIdentifiers,
                               ConsentHandleStatus status, int expiryMinutes) {
        this.consentHandleId = consentHandleId;
        this.businessId = businessId;
        this.txnId = txnId;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.url = url;
        this.customerIdentifiers = customerIdentifiers;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.expiresAt = Instant.now().plus(Duration.ofMinutes(expiryMinutes));
        this.className = "com.example.scanner.entity.ConsentHandle";
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}