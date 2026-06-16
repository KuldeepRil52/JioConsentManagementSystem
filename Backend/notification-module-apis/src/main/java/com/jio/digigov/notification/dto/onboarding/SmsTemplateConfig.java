package com.jio.digigov.notification.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration for SMS notification template.
 * Used during onboarding to define default SMS templates.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplateConfig {

    /**
     * Language for the template (e.g., "english")
     */
    private String language;

    /**
     * List of whitelisted mobile numbers for template testing
     */
    private List<String> whiteListedNumbers;

    /**
     * SMS template text with placeholders (<#ARG1>, <#ARG2>, etc.)
     * Example: "Dear user (<#ARG1>), your consent (ID: <#ARG2>) was created on <#ARG3>."
     */
    private String template;

    /**
     * Brief description of the template's purpose
     */
    private String templateDetails;

    /**
     * List of operator countries (ISO codes)
     */
    private List<String> oprCountries;

    /**
     * DLT Entity ID for regulatory compliance
     */
    private String dltEntityId;

    /**
     * DLT Template ID registered with telecom authorities
     */
    private String dltTemplateId;

    /**
     * Sender ID displayed to the recipient
     */
    private String from;

    /**
     * Mapping of placeholders to master label identifiers
     * Example: {"<#ARG1>": "MASTER_LABEL_USER_IDENTIFIER", "<#ARG2>": "MASTER_LABEL_CONSENT_ID"}
     */
    private Map<String, String> argumentsMap;
}
