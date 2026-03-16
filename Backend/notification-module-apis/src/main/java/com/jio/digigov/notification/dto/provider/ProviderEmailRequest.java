package com.jio.digigov.notification.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Unified email request DTO for provider abstraction layer.
 * This DTO is used across all notification providers (DigiGov, SMTP, etc.)
 * to standardize the email sending interface.
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderEmailRequest {

    /**
     * Template ID - DigiGov templateId or SMTP template identifier
     */
    private String templateId;

    /**
     * Recipient email addresses
     */
    private List<String> to;

    /**
     * CC email addresses (optional)
     */
    private List<String> cc;

    /**
     * BCC email addresses (optional)
     */
    private List<String> bcc;

    /**
     * Email subject (resolved with arguments for SMTP, ignored for DigiGov)
     */
    private String subject;

    /**
     * Email body content (resolved with arguments for SMTP, ignored for DigiGov)
     */
    private String body;

    /**
     * Email type: HTML or TEXT
     */
    private String emailType;

    /**
     * From email address (used by SMTP, ignored for DigiGov)
     */
    private String fromEmail;

    /**
     * From name (used by SMTP, can be used for DigiGov templateFromName)
     */
    private String fromName;

    /**
     * Arguments for subject placeholders (DigiGov: argsSubject, SMTP: pre-resolved)
     */
    private Map<String, String> subjectArguments;

    /**
     * Arguments for body placeholders (DigiGov: argsBody, SMTP: pre-resolved)
     */
    private Map<String, String> bodyArguments;

    /**
     * Message details / description
     */
    private String messageDetails;

    /**
     * Transaction ID for tracking
     */
    private String transactionId;

    /**
     * Tenant ID for multi-tenant support
     */
    private String tenantId;

    /**
     * Business ID for business-level configuration
     */
    private String businessId;

    /**
     * Event ID for tracking
     */
    private String eventId;

    /**
     * Notification ID for tracking
     */
    private String notificationId;

    /**
     * Additional headers (provider-specific)
     */
    private Map<String, String> additionalHeaders;

    /**
     * Attachment file paths (optional, for future use)
     */
    private List<String> attachments;
}
