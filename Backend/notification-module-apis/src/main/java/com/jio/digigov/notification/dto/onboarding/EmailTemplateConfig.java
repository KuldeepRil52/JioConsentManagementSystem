package com.jio.digigov.notification.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration for Email notification template.
 * Used during onboarding to define default Email templates.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateConfig {

    /**
     * Language for the template (e.g., "english")
     */
    private String language;

    /**
     * List of primary recipient email addresses for testing
     */
    private List<String> toRecipients;

    /**
     * List of CC recipient email addresses
     */
    private List<String> ccRecipients;

    /**
     * Brief description of the template's purpose
     */
    private String templateDetails;

    /**
     * Email body HTML content with placeholders
     * Example: "<html><body><h2>Dear user (<#ARG1>)</h2><p>Your consent...</p></body></html>"
     */
    private String templateBody;

    /**
     * Email subject line with placeholders (<#ARG1>, <#ARG2>, etc.)
     * Example: "Consent Created – <#ARG1>"
     */
    private String templateSubject;

    /**
     * Display name shown as the sender
     */
    private String templateFromName;

    /**
     * Email content format type (HTML or TEXT)
     */
    private String emailType;

    /**
     * Sender email address
     */
    private String from;

    /**
     * Mapping of placeholders in subject to master label identifiers
     * Example: {"<#ARG1>": "MASTER_LABEL_ORGANIZATION_NAME"}
     */
    private Map<String, String> argumentsSubjectMap;

    /**
     * Mapping of placeholders in body to master label identifiers
     * Example: {"<#ARG1>": "MASTER_LABEL_USER_IDENTIFIER", "<#ARG2>": "MASTER_LABEL_CONSENT_ID"}
     */
    private Map<String, String> argumentsBodyMap;
}
