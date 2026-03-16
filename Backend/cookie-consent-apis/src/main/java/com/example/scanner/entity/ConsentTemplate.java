package com.example.scanner.entity;

import com.example.scanner.dto.*;
import com.example.scanner.dto.request.CreateTemplateRequest;
import com.example.scanner.enums.TemplateStatus;
import com.example.scanner.enums.VersionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(collection = "cookie_consent_templates")
@Data
public class ConsentTemplate {

    @Id
    @JsonProperty("_id")
    private String id; // Unique document ID (changes with each version)

    @Field("templateId")
    @JsonProperty("templateId")
    private String templateId; // Logical template ID (same across versions)

    @Field("scanId")
    @JsonProperty("scanId")
    private String scanId; // IMMUTABLE: Links to original scan

    @Field("templateName")
    @JsonProperty("templateName")
    private String templateName;

    @Field("businessId")
    @JsonProperty("businessId")
    private String businessId; // IMMUTABLE: Business association

    @Field("status")
    @JsonProperty("status")
    private TemplateStatus status; // DRAFT, PUBLISHED (template lifecycle)

    @Field("templateStatus")
    @JsonProperty("templateStatus")
    private VersionStatus templateStatus; // ACTIVE, UPDATED (version status)

    @Field("multilingual")
    @JsonProperty("multilingual")
    private Multilingual multilingual;

    @Field("uiConfig")
    @JsonProperty("uiConfig")
    private UiConfig uiConfig;

    @Field("documentMeta")
    @JsonProperty("documentMeta")
    private DocumentMeta documentMeta;

    @Field("privacyPolicyDocument")
    @JsonProperty("privacyPolicyDocument")
    private String privacyPolicyDocument;

    @Field("preferences")
    @JsonProperty("preferences")
    private List<Preference> preferences;

    @Field("version")
    @JsonProperty("version")
    private Integer version; // Version number (1, 2, 3...)

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

    public ConsentTemplate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.className = "com.example.scanner.entity.ConsentTemplate";
        this.version = 1;
        this.status = TemplateStatus.DRAFT;
        this.templateStatus = VersionStatus.ACTIVE; // New templates are active by default
    }

    // Helper method to create from CreateTemplateRequest
    public static ConsentTemplate fromCreateRequest(CreateTemplateRequest request, String scanId, String businessId) {
        ConsentTemplate template = new ConsentTemplate();
        template.setTemplateId(UUID.randomUUID().toString()); // Generate logical template ID
        template.setScanId(scanId);
        template.setTemplateName(request.getTemplateName());
        template.setBusinessId(businessId);
        template.setStatus(request.getStatus() != null ? request.getStatus() : TemplateStatus.DRAFT);
        template.setTemplateStatus(VersionStatus.ACTIVE); // First version is always active
        template.setMultilingual(request.getMultilingual());
        template.setUiConfig(request.getUiConfig());
        template.setPrivacyPolicyDocument(request.getPrivacyPolicyDocument());
        template.setDocumentMeta(request.getDocumentMeta());
        template.setPreferences(request.getPreferences());
        return template;
    }
}