package com.jio.digigov.notification.dto.response.onboarding;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for onboarding setup initiation.
 *
 * This DTO contains the job details returned immediately after creating
 * an onboarding job (202 Accepted response).
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSetupResponseDto {

    /**
     * Unique job identifier for tracking
     */
    private String jobId;

    /**
     * Initial status (always QUEUED when created)
     */
    private OnboardingJobStatus status;

    /**
     * Business identifier
     */
    private String businessId;

    /**
     * When the job was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    /**
     * Estimated completion time (approximately 5 minutes from creation)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime estimatedCompletionTime;

    /**
     * URL to check job status
     */
    private String statusCheckUrl;
}
