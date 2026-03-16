package com.jio.digigov.notification.dto.response.onboarding;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary DTO for onboarding job (used in list view).
 *
 * Contains essential job information without detailed results.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingJobSummaryDto {

    /**
     * Unique job identifier
     */
    private String jobId;

    /**
     * Current job status
     */
    private OnboardingJobStatus status;

    /**
     * Business identifier
     */
    private String businessId;

    /**
     * Progress percentage (0-100)
     */
    private int progressPercentage;

    /**
     * When the job was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    /**
     * When the job completed (null if still in progress)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime completedAt;
}
