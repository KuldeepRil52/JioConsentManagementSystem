package com.jio.digigov.notification.service.onboarding;

import com.jio.digigov.notification.dto.request.onboarding.OnboardingSetupRequestDto;
import com.jio.digigov.notification.dto.response.onboarding.OnboardingJobStatusResponseDto;
import com.jio.digigov.notification.dto.response.onboarding.OnboardingJobSummaryDto;
import com.jio.digigov.notification.dto.response.onboarding.OnboardingSetupResponseDto;
import com.jio.digigov.notification.enums.ScopeLevel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for tenant/business onboarding operations.
 *
 * Provides methods to:
 * - Initiate onboarding (with validation)
 * - Check job status
 * - List onboarding jobs
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
public interface OnboardingService {

    /**
     * Initiates the onboarding process for a tenant/business.
     *
     * This method performs synchronous validation to ensure no templates
     * or event configurations exist for the business, then creates an
     * onboarding job and triggers async processing.
     *
     * @param tenantId Tenant identifier (determines database: tenant_db_{tenantId})
     * @param businessId Business identifier (filters data within tenant DB)
     * @param scopeLevel Scope level for created templates (BUSINESS or TENANT)
     * @param request Onboarding setup request with flags
     * @param transactionId Transaction ID for tracking
     * @param httpRequest HTTP servlet request for IP extraction and audit logging
     * @return OnboardingSetupResponseDto with job ID and status
     * @throws com.jio.digigov.notification.exception.OnboardingException if validation fails
     */
    OnboardingSetupResponseDto initiateOnboarding(
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel,
            OnboardingSetupRequestDto request,
            String transactionId,
            HttpServletRequest httpRequest
    );

    /**
     * Retrieves the status of an onboarding job.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param jobId Job identifier
     * @return OnboardingJobStatusResponseDto with detailed job status
     * @throws com.jio.digigov.notification.exception.OnboardingException if job not found
     */
    OnboardingJobStatusResponseDto getJobStatus(
            String tenantId,
            String businessId,
            String jobId
    );

    /**
     * Lists onboarding jobs with pagination and optional filtering.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier (optional - if null, shows all for tenant)
     * @param status Job status filter (optional)
     * @param pageable Pagination parameters
     * @return Page of OnboardingJobSummaryDto
     */
    Page<OnboardingJobSummaryDto> listJobs(
            String tenantId,
            String businessId,
            com.jio.digigov.notification.enums.OnboardingJobStatus status,
            Pageable pageable
    );
}
