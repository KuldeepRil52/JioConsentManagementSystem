package com.jio.digigov.notification.dto.request.onboarding;

import com.jio.digigov.notification.enums.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified request DTO for initiating onboarding process.
 *
 * This DTO contains minimal configuration parameters for copying shared
 * notification templates, event configs, and master labels to tenant database.
 *
 * Notification configuration must be created separately via the configuration API.
 * Template DLT IDs are pre-configured in the shared templates JSON file.
 *
 * @author Notification Service Team
 * @version 2.0
 * @since 2025-01-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSetupRequestDto {

    /**
     * Provider type for notification delivery (DIGIGOV or SMTP).
     * This determines which templates to use, but does not create the configuration.
     * Tenants must create notification configuration separately.
     * Default: DIGIGOV
     */
    @Builder.Default
    private ProviderType providerType = ProviderType.DIGIGOV;

    /**
     * Whether to copy default notification templates (SMS and Email) from shared JSON.
     * Templates are copied as-is with pre-configured DLT IDs.
     * Default: true
     */
    @Builder.Default
    private Boolean createTemplates = true;

    /**
     * Whether to copy default event configurations from shared JSON.
     * Event configs define which roles (DF, DP, DPO, etc.) receive notifications.
     * Default: true
     */
    @Builder.Default
    private Boolean createEventConfigurations = true;

    /**
     * Whether to copy ALL master list labels from shared JSON.
     * Master labels are used for dynamic data resolution in templates.
     * Default: true
     */
    @Builder.Default
    private Boolean createMasterList = true;
}
