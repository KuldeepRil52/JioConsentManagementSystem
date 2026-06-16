package com.jio.digigov.fides.service.impl;

import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.entity.ConsentWithdrawalJob;
import com.jio.digigov.fides.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final MultiTenantMongoConfig mongoConfig;

    @Override
    public ConsentWithdrawalJob getJob(String jobId, String tenantId, String businessId) {
        log.info("Fetching ConsentWithdrawalJob with ID: {} for tenantId: {}", jobId, tenantId);
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        ConsentWithdrawalJob job = mongoTemplate.findById(jobId, ConsentWithdrawalJob.class);
        if (job == null) {
            log.error("ConsentWithdrawalJob not found: {}", jobId);
            throw new IllegalArgumentException("Job not found");
        }

        if (!job.getBusinessId().equals(businessId)) {
            log.error("Unauthorized access attempt for jobId: {} with businessId: {}", jobId, businessId);
            throw new SecurityException("Unauthorized access to job");
        }

        return job;
    }
}