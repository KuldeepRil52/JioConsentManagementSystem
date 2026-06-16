package com.jio.digigov.notification.dto.onboarding.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Simple DTO for template data from JSON.
 * Contains only the core data (no styling/HTML).
 *
 * @author DPDP Notification Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateDataFile {

    /**
     * Event type (e.g., GRIEVANCE_RAISED, CONSENT_CREATED)
     */
    private String eventType;

    /**
     * Recipient type (e.g., DATA_PRINCIPAL, DATA_PROTECTION_OFFICER)
     */
    private String recipientType;

    /**
     * SMS template configuration
     */
    private SmsData sms;

    /**
     * Email template configuration
     */
    private EmailData email;

    /**
     * SMS template data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SmsData {
        /**
         * SMS template text with placeholders
         */
        private String template;

        /**
         * DLT Entity ID (required for DigiGov)
         */
        private String dltEntityId;

        /**
         * DLT Template ID (required for DigiGov)
         */
        private String dltTemplateId;

        /**
         * SMS sender/from field
         */
        private String from;

        /**
         * Argument mappings (placeholder → master label)
         */
        private Map<String, String> arguments;
    }

    /**
     * Email template data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmailData {
        /**
         * Email subject line with placeholders
         */
        private String subject;

        /**
         * From name for the email
         */
        private String fromName;

        /**
         * From for the email
         */
        private String from;

        /**
         * Reply-to email address for recipient responses
         */
        private String replyTo;

        /**
         * Subject argument mappings (placeholder → master label)
         */
        private Map<String, String> subjectArguments;

        /**
         * Body argument mappings (placeholder → master label)
         */
        private Map<String, String> bodyArguments;

        /**
         * PRIORITY 1: Complete HTML content (optional).
         * If provided, this will be used directly without any generation.
         */
        private String completeHtmlContent;

        /**
         * PRIORITY 2: Component configuration for generic generator (optional).
         * Used only if completeHtmlContent is null/empty.
         */
        private ComponentConfig components;
    }

    /**
     * Component configuration for generic HTML generation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComponentConfig {
        /**
         * Main email title/heading
         */
        private String title;

        /**
         * Greeting message (e.g., "Dear <#ARG1>")
         */
        private String greeting;

        /**
         * Main message content
         */
        private String mainMessage;

        /**
         * Alert banner configuration
         */
        private AlertBanner alertBanner;

        /**
         * Info box configuration
         */
        private InfoBox infoBox;

        /**
         * Action button configuration
         */
        private ActionButton actionButton;

        /**
         * Footer message
         */
        private String footerMessage;

        /**
         * Additional message after main content
         */
        private String additionalMessage;
    }

    /**
     * Alert banner configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlertBanner {
        /**
         * Whether to show alert banner
         */
        private Boolean show;

        /**
         * Alert level: info, warning, critical
         */
        private String level;

        /**
         * Alert message text
         */
        private String message;

        /**
         * Icon/emoji to display
         */
        private String icon;
    }

    /**
     * Info box configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoBox {
        /**
         * Whether to show info box
         */
        private Boolean show;

        /**
         * List of field configurations
         */
        private java.util.List<InfoField> fields;
    }

    /**
     * Info field in info box.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoField {
        /**
         * Field label
         */
        private String label;

        /**
         * Field value (placeholder like <#ARG1>)
         */
        private String value;

        /**
         * Whether this is a status badge
         */
        private Boolean isStatusBadge;

        /**
         * Badge color (for status badges): success, warning, danger, info
         */
        private String badgeColor;
    }

    /**
     * Action button configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionButton {
        /**
         * Whether to show action button
         */
        private Boolean show;

        /**
         * Button text
         */
        private String text;

        /**
         * Button URL (placeholder like <#ARG6>)
         */
        private String url;
    }
}
