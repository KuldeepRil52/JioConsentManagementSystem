package com.jio.digigov.notification.dto.response.onboarding;

import com.jio.digigov.notification.enums.OnboardingStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Progress information for an onboarding job.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingProgressDto {

    /**
     * Current processing step
     */
    private OnboardingStep currentStep;

    /**
     * Total number of steps (always 5)
     */
    @Builder.Default
    private int totalSteps = 5;

    /**
     * Number of completed steps
     */
    private int completedSteps;

    /**
     * Progress percentage (0-100)
     */
    private int percentComplete;
}
