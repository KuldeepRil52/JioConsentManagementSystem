package com.jio.digigov.notification.dto.provider;

import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Unified template creation request for provider abstraction layer.
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderTemplateRequest {

    private String eventType;
    private String language;
    private NotificationType notificationType;
    private NotificationChannel channelType;
    private String recipientType;

    // SMS-specific fields
    private String smsTemplate;
    private String smsTemplateDetails;
    private List<String> whitelistedNumbers;
    private List<String> operatorCountries;
    private String dltEntityId;
    private String dltTemplateId;
    private String smsFrom;
    private Map<String, String> smsArgumentsMap;

    // Email-specific fields
    private String emailSubject;
    private String emailBody;
    private String emailTemplateDetails;
    private String emailFromName;
    private String emailType; // HTML or TEXT
    private List<String> emailTo;
    private List<String> emailCc;
    private Map<String, String> emailSubjectArgumentsMap;
    private Map<String, String> emailBodyArgumentsMap;

    // Common fields
    private String tenantId;
    private String businessId;
    private String transactionId;
}
