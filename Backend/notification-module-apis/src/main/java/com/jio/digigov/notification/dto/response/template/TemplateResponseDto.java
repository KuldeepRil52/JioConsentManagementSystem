package com.jio.digigov.notification.dto.response.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "Template details response")
public class TemplateResponseDto {
    
    @Schema(description = "Internal MongoDB ID", example = "68f0e41630cd435574b792a0")
    private String id;

    @Schema(description = "Template ID", example = "TEMPL000006296347")
    private String templateId;
    
    @Schema(description = "Tenant ID", example = "tenant_001")
    private String tenantId;
    
    @Schema(description = "Business ID", example = "business_001")
    private String businessId;
    
    @Schema(description = "Scope level", example = "BUSINESS")
    private String scopeLevel;
    
    @Schema(description = "Event type", example = "CONSENT_GRANTED")
    private String eventType;

    @Schema(description = "Recipient type", example = "DATA_PRINCIPAL",
            allowableValues = {"DATA_PRINCIPAL", "DATA_FIDUCIARY", "DATA_PROCESSOR", "DATA_PROTECTION_OFFICER"})
    private String recipientType;

    @Schema(description = "Language", example = "english")
    private String language;
    
    @Schema(description = "Template type", example = "NOTIFICATION")
    private String type;
    
    @Schema(description = "Channel type", example = "SMS")
    private String channel;
    
    @Schema(description = "Template status", example = "ACTIVE")
    private String status;
    
    @Schema(description = "Template version", example = "1")
    private Integer version;
    
    @Schema(description = "Template content for SMS")
    private String template;
    
    @Schema(description = "Template details/description")
    private String templateDetails;
    
    @Schema(description = "SMS configuration")
    private SmsConfigResponse smsConfig;
    
    @Schema(description = "Email configuration")
    private EmailConfigResponse emailConfig;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class SmsConfigResponse {
        private List<String> whiteListedNumber;
        private List<String> oprCountries;
        private String dltEntityId;
        private String dltTemplateId;
        private String from;
        private Map<String, String> argumentsMap;
    }
    
    @Data
    @Builder
    public static class EmailConfigResponse {
        private List<String> to;
        private List<String> cc;
        private String subject;
        private String body;
        private String fromName;
        private String from;
        private String replyTo;
        private String emailType;
        private Map<String, String> argumentsSubjectMap;
        private Map<String, String> argumentsBodyMap;
    }
}