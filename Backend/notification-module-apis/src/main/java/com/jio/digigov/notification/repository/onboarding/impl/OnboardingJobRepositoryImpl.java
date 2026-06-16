package com.jio.digigov.notification.repository.onboarding.impl;

import com.jio.digigov.notification.entity.onboarding.OnboardingJob;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import com.jio.digigov.notification.repository.onboarding.OnboardingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of OnboardingJobRepository using multi-tenant MongoTemplate approach.
 *
 * This repository handles onboarding job operations in a multi-tenant environment where
 * each tenant has its own dedicated MongoDB database. All methods accept a tenant-specific
 * MongoTemplate to ensure data isolation between tenants.
 *
 * Key Features:
 * - Multi-tenant data isolation using separate MongoTemplate instances
 * - Job management by business ID
 * - Support for filtering by status
 * - Pagination support for large datasets
 *
 * Thread Safety: This repository is thread-safe as it's stateless and relies on
 * thread-safe MongoTemplate operations.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class OnboardingJobRepositoryImpl implements OnboardingJobRepository {

    @Override
    public Optional<OnboardingJob> findByJobId(String jobId, MongoTemplate mongoTemplate) {
        log.debug("Finding onboarding job by jobId: {}", jobId);

        Query query = new Query(Criteria.where("jobId").is(jobId));
        OnboardingJob job = mongoTemplate.findOne(query, OnboardingJob.class);

        return Optional.ofNullable(job);
    }

    @Override
    public List<OnboardingJob> findByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding all onboarding jobs for businessId: {}", businessId);

        Query query = new Query(Criteria.where("businessId").is(businessId));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        return mongoTemplate.find(query, OnboardingJob.class);
    }

    @Override
    public Page<OnboardingJob> findAll(
            String businessId,
            OnboardingJobStatus status,
            Pageable pageable,
            MongoTemplate mongoTemplate) {

        log.debug("Finding onboarding jobs with filters: businessId={}, status={}, page={}, size={}",
                 businessId, status, pageable.getPageNumber(), pageable.getPageSize());

        // Build query criteria
        Query query = new Query(Criteria.where("businessId").is(businessId));

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        // Get total count before pagination
        long total = mongoTemplate.count(query, OnboardingJob.class);

        // Apply pagination
        query.with(pageable);

        // Execute query
        List<OnboardingJob> jobs = mongoTemplate.find(query, OnboardingJob.class);

        return new PageImpl<>(jobs, pageable, total);
    }
}
