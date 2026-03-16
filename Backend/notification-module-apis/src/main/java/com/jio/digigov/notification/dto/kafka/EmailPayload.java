package com.jio.digigov.notification.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Email-specific payload for Kafka notification messages in the DPDP system.
 *
 * This class contains comprehensive email delivery information including recipients,
 * content, attachments, and delivery preferences. It supports both simple text emails
 * and complex HTML emails with attachments for comprehensive stakeholder communication.
 *
 * Email Types Supported:
 * - Data Principal notifications (consent updates, privacy notices)
 * - Data Fiduciary communications (compliance reports, breach notifications)
 * - Data Processor notifications (processing agreements, audit reports)
 * - System notifications (error alerts, status updates)
 *
 * Template System Integration:
 * - HTML and text template support
 * - Dynamic content substitution
 * - Multi-language template resolution
 * - Responsive email design for mobile compatibility
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailPayload {

    /**
     * List of primary recipient email addresses.
     * These recipients are directly addressed in the email header.
     * All addresses must be valid email format.
     */
    private List<String> to;

    /**
     * List of carbon copy recipient email addresses.
     * These recipients receive a copy and are visible to all recipients.
     * Optional field for stakeholder notifications.
     */
    private List<String> cc;

    /**
     * List of blind carbon copy recipient email addresses.
     * These recipients receive a copy but are hidden from other recipients.
     * Used for internal notifications and compliance copies.
     */
    private List<String> bcc;

    /**
     * Email subject line after template resolution.
     * Contains the final subject with all placeholders substituted.
     * Should be concise and descriptive for the notification purpose.
     */
    private String subject;

    /**
     * Email body content after template resolution.
     * Contains the final message content with all placeholders substituted.
     * Can be plain text or HTML format based on template type.
     */
    private String template;

    /**
     * Template argument values for placeholder substitution.
     * Key-value pairs used to replace placeholders in email templates.
     * Example: {"customerName": "John Doe", "expiryDate": "2024-12-31"}
     * @deprecated Use templateArgsSubject and templateArgsBody instead
     */
    private Map<String, Object> templateArgs;

    /**
     * Template argument values for subject placeholder substitution.
     * Key-value pairs used to replace placeholders in email subject templates.
     * Maps to 'argsSubject' field in DigiGov API.
     */
    private Map<String, String> templateArgsSubject;

    /**
     * Template argument values for body placeholder substitution.
     * Key-value pairs used to replace placeholders in email body templates.
     * Maps to 'argsBody' field in DigiGov API.
     */
    private Map<String, String> templateArgsBody;

    /**
     * Alias for templateArgs to maintain backward compatibility.
     * Returns the same template arguments for placeholder substitution.
     * If new separate args are available, combines them; otherwise returns legacy templateArgs.
     */
    public Map<String, Object> getArguments() {
        if (templateArgsSubject != null || templateArgsBody != null) {
            Map<String, Object> combined = new HashMap<>();
            if (templateArgsSubject != null) {
                combined.putAll(templateArgsSubject);
            }
            if (templateArgsBody != null) {
                combined.putAll(templateArgsBody);
            }
            return combined;
        }
        return templateArgs != null ? new HashMap<>(templateArgs) : new HashMap<>();
    }

    /**
     * Template ID for the email subject line.
     * Used to resolve the subject template with dynamic arguments.
     */
    private String subjectTemplateId;

    /**
     * Template ID for the email body content.
     * Used to resolve the body template with dynamic arguments.
     */
    private String bodyTemplateId;

    /**
     * Email type classification for tracking and processing.
     * Examples: "CONSENT_NOTIFICATION", "BREACH_ALERT", "COMPLIANCE_REPORT"
     * Used for analytics and email categorization.
     */
    private String emailType;

    /**
     * Reply-to email address for recipient responses.
     * If specified, recipient replies will be directed to this address
     * instead of the sender address.
     */
    private String replyTo;

    /**
     * Whether the email content is HTML formatted.
     * True for HTML emails, false for plain text emails.
     */
    private boolean isHtml;

    /**
     * Sender email address for the notification.
     * The address that appears in the "From" field.
     */
    private String fromEmail;

    /**
     * Sender display name for the notification.
     * The name that appears with the "From" email address.
     */
    private String fromName;

    /**
     * Reply-to email address (alias for replyTo).
     * Maintains backward compatibility.
     */
    public String getReplyToEmail() {
        return replyTo;
    }

    /**
     * Email priority level for delivery.
     * Examples: "HIGH", "MEDIUM", "LOW"
     */
    private String priority;

    /**
     * List of CC email addresses (alias for cc).
     * Maintains backward compatibility.
     */
    public List<String> getCcEmails() {
        return cc;
    }

    /**
     * List of BCC email addresses (alias for bcc).
     * Maintains backward compatibility.
     */
    public List<String> getBccEmails() {
        return bcc;
    }

    /**
     * List of file attachments to include with the email.
     * Support for documents, reports, and compliance materials.
     * Each attachment includes content and metadata.
     */
    private List<Attachment> attachments;

    /**
     * Additional delivery options and preferences.
     * Contains email-specific delivery configurations and scheduling.
     */
    private DeliveryOptions deliveryOptions;

    /**
     * Email attachment representation with content and metadata.
     * Supports various file types for comprehensive communication.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {

        /**
         * Display name for the attachment file.
         * Shown to recipients in their email client.
         * Should include file extension for proper handling.
         */
        private String fileName;

        /**
         * MIME content type of the attachment.
         * Examples: "application/pdf", "text/csv", "image/png"
         * Used by email clients for proper file handling.
         */
        private String contentType;

        /**
         * Binary content of the attachment file.
         * Base64 encoded file data for transmission.
         * Size should be limited to prevent delivery issues.
         */
        private byte[] content;

        /**
         * Content disposition for the attachment.
         * "attachment" for downloadable files, "inline" for embedded content.
         */
        private String disposition;

        /**
         * Content ID for inline attachments.
         * Used to reference embedded images or documents within email content.
         */
        private String contentId;
    }

    /**
     * Email delivery configuration options.
     * Controls how and when the email message should be delivered.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryOptions {

        /**
         * Email priority level affecting delivery and display.
         * HIGH: Urgent notifications, MEDIUM: Normal, LOW: Informational
         */
        private Priority priority;

        /**
         * Scheduled delivery time for the email.
         * If null, email is sent immediately.
         * Future timestamp for delayed delivery.
         */
        private LocalDateTime scheduleTime;

        /**
         * Request read receipt from recipient.
         * Enables tracking when recipients open the email.
         * Not all email clients support this feature.
         */
        private boolean requestReadReceipt;

        /**
         * Request delivery receipt from mail server.
         * Confirms email was delivered to recipient's mail server.
         * Useful for compliance and audit requirements.
         */
        private boolean requestDeliveryReceipt;

        /**
         * Email importance flag for client display.
         * Affects how email clients present the message importance.
         */
        private String importance;

        /**
         * Sensitivity level for the email content.
         * Examples: "Normal", "Confidential", "Private"
         * Used by email clients for handling guidelines.
         */
        private String sensitivity;

        /**
         * Custom email headers for specific processing.
         * Additional metadata for email routing and processing.
         */
        private Map<String, String> customHeaders;
    }

    /**
     * Email priority levels for delivery and display.
     */
    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}