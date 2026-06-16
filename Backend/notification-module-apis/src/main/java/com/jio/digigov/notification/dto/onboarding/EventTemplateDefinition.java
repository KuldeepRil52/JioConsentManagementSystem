package com.jio.digigov.notification.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines notification templates for a specific event type.
 * Contains both SMS and Email template configurations.
 *
 * Used by OnboardingTemplateProvider to define default templates
 * that are created during tenant/business onboarding.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTemplateDefinition {

    /**
     * Event type (e.g., CONSENT_CREATED, GRIEVANCE_RAISED)
     */
    private String eventType;

    /**
     * Recipient type (DATA_PRINCIPAL, DATA_PROTECTION_OFFICER, etc.)
     * Defaults to DATA_PRINCIPAL if not specified for backward compatibility
     */
    private String recipientType;

    /**
     * SMS template configuration
     */
    private SmsTemplateConfig sms;

    /**
     * Email template configuration
     */
    private EmailTemplateConfig email;
}
