package com.jio.digigov.notification.dto.response.onboarding;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed status response for an onboarding job.
 *
 * Contains complete information about job status, progress, results, and errors.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingJobStatusResponseDto {

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
     * Progress information
     */
    private OnboardingProgressDto progress;

    /**
     * Results summary (null if not yet completed)
     */
    private OnboardingResultsDto results;

    /**
     * When the job started processing
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startedAt;

    /**
     * When the job completed (null if still in progress)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime completedAt;

    /**
     * Human-readable duration (e.g., "3m37s")
     */
    private String duration;

    /**
     * List of error messages (empty if no errors)
     */
    private List<String> errors;
}
