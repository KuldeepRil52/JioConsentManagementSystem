package com.jio.digigov.notification.entity.template;

import com.jio.digigov.notification.entity.BaseEntity;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.TemplateStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Unified Notification Template Entity
 * Stored in tenant-specific database: tenant_db_{tenantId}
 * Supports multiple notification providers (DigiGov, SMTP, etc.)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_templates")
@CompoundIndexes({
    @CompoundIndex(name = "unique_template_idx",
                  def = "{'businessId': 1, 'eventType': 1, 'language': 1, 'type': 1, 'channelType': 1, 'recipientType': 1, 'providerType': 1}",
                  unique = true),
    @CompoundIndex(name = "business_status_idx",
                  def = "{'businessId': 1, 'status': 1}"),
    @CompoundIndex(name = "business_event_idx",
                  def = "{'businessId': 1, 'eventType': 1, 'status': 1}"),
    @CompoundIndex(name = "business_provider_idx",
                  def = "{'businessId': 1, 'providerType': 1, 'status': 1}")
})
public class NotificationTemplate extends BaseEntity {

    // Template ID (from DigiGov for DIGIGOV provider, or generated UUID for SMTP)
    @Indexed(unique = true)
    @Field("templateId")
    private String templateId; // DigiGov: "TEMPL000006296347", SMTP: UUID

    // Hierarchy fields - business scope only for templates
    @NotBlank(message = "Business ID is required")
    @Field("businessId")
    private String businessId; // Always required for template operations

    @NotNull
    @Field("scopeLevel")
    private ScopeLevel scopeLevel; // Always BUSINESS for templates

    // Template classification
    @NotBlank(message = "Event type is required")
    @Field("eventType")
    private String eventType; // CONSENT_GRANTED, OTP_VERIFICATION, etc.

    @NotBlank(message = "Language is required")
    @Field("language")
    private String language; // english, hindi, etc. (from config default)

    @NotNull
    @Field("type")
    private NotificationType type; // NOTIFICATION or OTPVALIDATOR

    @NotNull(message = "Channel type is required")
    @Field("channelType")
    private NotificationChannel channelType; // SMS or EMAIL - determines which config is active

    @Field("recipientType")
    @Builder.Default
    private String recipientType = com.jio.digigov.notification.enums.RecipientType.DATA_PRINCIPAL.name(); // DATA_PRINCIPAL, DATA_FIDUCIARY, DATA_PROCESSOR, DATA_PROTECTION_OFFICER

    // Provider type - determines which notification service to use
    @NotNull(message = "Provider type is required")
    @Field("providerType")
    @Builder.Default
    private ProviderType providerType = ProviderType.DIGIGOV; // Default to DigiGov for backward compatibility

    // Channel configurations - at least one must be non-null
    @Field("smsConfig")
    private SmsConfig smsConfig; // null if SMS not configured

    @Field("emailConfig")
    private EmailConfig emailConfig; // null if Email not configured

    // Common fields
    @NotNull
    @Field("status")
    private TemplateStatus status; // PENDING, ACTIVE, INACTIVE, FAILED

    @Builder.Default
    @Field("version")
    private Integer version = 1; // For future versioning support
    
    // Embedded classes matching DigiGov API requirements
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsConfig {
        @Field("whiteListedNumber")
        private List<String> whiteListedNumber; // Required: Whitelisted mobile numbers

        @Field("template")
        private String template; // Required: SMS content with <#ARG> placeholders

        @Field("templateDetails")
        private String templateDetails; // Required: Description of template purpose

        @Field("oprCountries")
        private List<String> oprCountries; // Required: Country codes (e.g., ["IN"])

        @Field("dltEntityId")
        private String dltEntityId; // Required: DLT Entity ID

        @Field("dltTemplateId")
        private String dltTemplateId; // Required: DLT Template ID

        @Field("from")
        private String from; // Optional: Sender name

        @Field("argumentsMap")
        private Map<String, String> argumentsMap; // Optional: <#ARG1> -> MASTER_LABEL_USER_NAME
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailConfig {
        @Field("to")
        private List<String> to; // Required: Email addresses

        @Field("cc")
        private List<String> cc; // Required: CC email addresses

        @Field("templateDetails")
        private String templateDetails; // Required: Description of template purpose

        @Field("templateBody")
        private String templateBody; // Required: Email body with <#ARG> placeholders

        @Field("templateSubject")
        private String templateSubject; // Required: Subject line with <#ARG> placeholders

        @Field("templateFromName")
        private String templateFromName; // Optional: From name display

        @Field("emailType")
        private String emailType; // Required: "HTML" or "TEXT"

        @Field("from")
        private String from; // Optional: Sender email address

        @Field("replyTo")
        private String replyTo; // Optional: Reply-to email address

        @Field("argumentsSubjectMap")
        private Map<String, String> argumentsSubjectMap; // Optional: <#ARG1> -> MASTER_LABEL_USER_NAME

        @Field("argumentsBodyMap")
        private Map<String, String> argumentsBodyMap; // Optional: <#ARG1> -> MASTER_LABEL_USER_NAME
    }
}