package com.jio.digigov.notification.repository.onboarding;

import com.jio.digigov.notification.entity.onboarding.OnboardingJob;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OnboardingJob operations.
 *
 * This repository handles CRUD operations for onboarding jobs in a multi-tenant environment.
 * All methods accept MongoTemplate parameter to ensure tenant-specific database access.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
public interface OnboardingJobRepository {

    /**
     * Find job by job ID
     *
     * @param jobId         Unique job identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the job if found
     */
    Optional<OnboardingJob> findByJobId(String jobId, MongoTemplate mongoTemplate);

    /**
     * Find all jobs for a specific business
     *
     * @param businessId    Business identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of jobs for the business, ordered by creation date (newest first)
     */
    List<OnboardingJob> findByBusinessId(String businessId, MongoTemplate mongoTemplate);

    /**
     * Find jobs with optional filters and pagination
     *
     * @param businessId    Business identifier
     * @param status        Optional status filter (can be null)
     * @param pageable      Pagination and sorting parameters
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Page of jobs matching the criteria
     */
    Page<OnboardingJob> findAll(
        String businessId,
        OnboardingJobStatus status,
        Pageable pageable,
        MongoTemplate mongoTemplate
    );
}
