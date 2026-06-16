package com.jio.digigov.notification.service.onboarding.impl;

import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.dto.request.onboarding.OnboardingSetupRequestDto;
import com.jio.digigov.notification.dto.response.onboarding.*;
import com.jio.digigov.notification.entity.onboarding.OnboardingJob;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.exception.OnboardingException;
import com.jio.digigov.notification.repository.event.EventConfigurationRepository;
import com.jio.digigov.notification.repository.onboarding.OnboardingJobRepository;
import com.jio.digigov.notification.repository.template.NotificationTemplateRepositoryCustom;
import com.jio.digigov.notification.service.audit.AuditEventService;
import com.jio.digigov.notification.service.onboarding.OnboardingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementation of OnboardingService.
 *
 * Handles synchronous validation and job creation for onboarding operations.
 * Delegates actual processing to OnboardingAsyncService.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Service
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final MongoTemplateProvider mongoTemplateProvider;
    private final OnboardingJobRepository jobRepository;
    private final NotificationTemplateRepositoryCustom templateRepository;
    private final EventConfigurationRepository eventConfigRepository;
    private final OnboardingAsyncService asyncService;
    private final AuditEventService auditEventService;

    public OnboardingServiceImpl(
            MongoTemplateProvider mongoTemplateProvider,
            OnboardingJobRepository jobRepository,
            @Qualifier("notificationTemplateRepositoryImpl") NotificationTemplateRepositoryCustom templateRepository,
            EventConfigurationRepository eventConfigRepository,
            OnboardingAsyncService asyncService,
            AuditEventService auditEventService) {
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.jobRepository = jobRepository;
        this.templateRepository = templateRepository;
        this.eventConfigRepository = eventConfigRepository;
        this.asyncService = asyncService;
        this.auditEventService = auditEventService;
    }

    @Override
    public OnboardingSetupResponseDto initiateOnboarding(
            String tenantId,
            String businessId,
            com.jio.digigov.notification.enums.ScopeLevel scopeLevel,
            OnboardingSetupRequestDto request,
            String transactionId,
            HttpServletRequest httpRequest) {

        log.info("Initiating onboarding: tenant={}, business={}, scope={}, transaction={}, providerType={}",
                tenantId, businessId, scopeLevel, transactionId, request.getProviderType());

        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        validatePrerequisites(mongoTemplate, businessId, request.getProviderType());

        String jobId = generateJobId();
        OnboardingJob job = OnboardingJob.builder()
                .jobId(jobId)
                .businessId(businessId)
                .status(OnboardingJobStatus.QUEUED)
                .transactionId(transactionId)
                .requestParams(OnboardingJob.OnboardingRequestParams.builder()
                        .createTemplates(request.getCreateTemplates())
                        .createEventConfigurations(request.getCreateEventConfigurations())
                        .createMasterList(request.getCreateMasterList())
                        .build())
                .progressPercentage(0)
                .build();

        LocalDateTime now = LocalDateTime.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        mongoTemplate.save(job);

        log.info("Onboarding job created: jobId={}, tenant={}, business={}, scope={}",
                jobId, tenantId, businessId, scopeLevel);

        // Audit onboarding initiation
        auditEventService.auditTenantOnboarding(
                jobId,
                "INITIATED",
                tenantId,
                businessId,
                transactionId
        );

        asyncService.processOnboarding(jobId, tenantId, businessId, scopeLevel, request);

        // Calculate estimated completion time (5 minutes)
        LocalDateTime estimatedCompletion = now.plusMinutes(5);

        return OnboardingSetupResponseDto.builder()
                .jobId(jobId)
                .status(OnboardingJobStatus.QUEUED)
                .businessId(businessId)
                .createdAt(now)
                .estimatedCompletionTime(estimatedCompletion)
                .statusCheckUrl("/v1/onboarding/jobs/" + jobId)
                .build();
    }

    @Override
    public OnboardingJobStatusResponseDto getJobStatus(
            String tenantId,
            String businessId,
            String jobId) {

        log.debug("Getting job status: tenant={}, business={}, jobId={}",
                tenantId, businessId, jobId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Find job
        OnboardingJob job = jobRepository.findByJobId(jobId, mongoTemplate)
                .orElseThrow(() -> new OnboardingException(
                        "JDNM1002",
                        String.format("Onboarding job not found: %s", jobId)
                ));

        // Verify business ID matches
        if (!job.getBusinessId().equals(businessId)) {
            throw new OnboardingException(
                    "JDNM1003",
                    String.format("Job %s does not belong to business %s", jobId, businessId)
            );
        }

        // Calculate duration if completed
        String duration = null;
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            long seconds = java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).getSeconds();
            duration = formatDuration(seconds);
        }

        // Build progress DTO
        OnboardingProgressDto progress = OnboardingProgressDto.builder()
                .currentStep(job.getCurrentStep())
                .totalSteps(4)  // EXTRACT_LABELS, CREATE_MASTER_LIST, CREATE_TEMPLATES, CREATE_EVENT_CONFIGS
                .completedSteps(calculateCompletedSteps(job))
                .percentComplete(job.getProgressPercentage())
                .build();

        // Build results DTO
        OnboardingResultsDto results = null;
        if (job.getResults() != null) {
            OnboardingResultsDto.MasterListResultDto masterListDto = null;
            if (job.getResults().getMasterListLabels() != null) {
                masterListDto = OnboardingResultsDto.MasterListResultDto.builder()
                        .created(job.getResults().getMasterListLabels().getCreated())
                        .failed(job.getResults().getMasterListLabels().getFailed())
                        .build();
            }

            OnboardingResultsDto.TemplateResultDto templatesDto = null;
            if (job.getResults().getTemplates() != null) {
                templatesDto = OnboardingResultsDto.TemplateResultDto.builder()
                        .created(job.getResults().getTemplates().getCreated())
                        .failed(job.getResults().getTemplates().getFailed())
                        .smsCount(job.getResults().getTemplates().getSmsCount())
                        .emailCount(job.getResults().getTemplates().getEmailCount())
                        .build();
            }

            OnboardingResultsDto.EventConfigResultDto eventConfigsDto = null;
            if (job.getResults().getEventConfigurations() != null) {
                eventConfigsDto = OnboardingResultsDto.EventConfigResultDto.builder()
                        .created(job.getResults().getEventConfigurations().getCreated())
                        .failed(job.getResults().getEventConfigurations().getFailed())
                        .build();
            }

            results = OnboardingResultsDto.builder()
                    .masterListLabels(masterListDto)
                    .templates(templatesDto)
                    .eventConfigurations(eventConfigsDto)
                    .build();
        }

        return OnboardingJobStatusResponseDto.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .businessId(job.getBusinessId())
                .progress(progress)
                .results(results)
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .duration(duration)
                .errors(job.getErrors())
                .build();
    }

    @Override
    public Page<OnboardingJobSummaryDto> listJobs(
            String tenantId,
            String businessId,
            OnboardingJobStatus status,
            Pageable pageable) {

        log.debug("Listing jobs: tenant={}, business={}, status={}, page={}",
                tenantId, businessId, status, pageable.getPageNumber());

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Find jobs with filters
        Page<OnboardingJob> jobsPage = jobRepository.findAll(
                businessId,
                status,
                pageable,
                mongoTemplate
        );

        // Map to summary DTOs
        return jobsPage.map(job -> OnboardingJobSummaryDto.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .businessId(job.getBusinessId())
                .progressPercentage(job.getProgressPercentage())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build());
    }

    /**
     * Validates that no templates or event configurations exist for this business.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate (connected to tenant_db_{tenantId})
     * @param businessId Business identifier to check
     * @param providerType Provider type (SMTP or DIGIGOV)
     * @throws OnboardingException if validation fails
     */
    private void validatePrerequisites(MongoTemplate mongoTemplate, String businessId, ProviderType providerType) {
        log.info("Validating prerequisites for businessId: {}, providerType: {}", businessId, providerType);

        // Check 1: No templates should exist
        boolean templatesExist = templateRepository
                .existsByBusinessId(businessId, mongoTemplate);

        if (templatesExist) {
            long count = templateRepository
                    .countByBusinessId(businessId, mongoTemplate);

            throw new OnboardingException(
                    "JDNM1001",
                    String.format(
                            "Templates already exist for business '%s' (%d found). " +
                                    "Cannot proceed with onboarding. Please delete existing templates first.",
                            businessId, count
                    )
            );
        }

        // Check 2: No event configurations should exist
        boolean eventConfigsExist = eventConfigRepository
                .existsByBusinessId(businessId, mongoTemplate);

        if (eventConfigsExist) {
            long count = eventConfigRepository
                    .countByBusinessId(businessId, mongoTemplate);

            throw new OnboardingException(
                    "JDNM1001",
                    String.format(
                            "Event configurations already exist for business '%s' (%d found). " +
                                    "Cannot proceed with onboarding. Please delete existing configurations first.",
                            businessId, count
                    )
            );
        }

        log.info("Prerequisites validation passed - no existing data found for business: {}", businessId);
    }

    /**
     * Generates a unique job ID with timestamp.
     *
     * Format: ONB-YYYYMMdd-HHmmss-XXXXXXXX
     * Example: ONB-20250121-103045-A1B2C3D4
     *
     * @return Unique job ID
     */
    private String generateJobId() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String randomSuffix = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return String.format("ONB-%s-%s", timestamp, randomSuffix);
    }

    /**
     * Calculates the number of completed steps based on current step.
     *
     * @param job Onboarding job
     * @return Number of completed steps (0-4)
     */
    private int calculateCompletedSteps(OnboardingJob job) {
        if (job.getCurrentStep() == null) {
            return 0;
        }

        switch (job.getCurrentStep()) {
            case EXTRACT_LABELS:
                return 1;
            case CREATE_MASTER_LIST:
                return 2;
            case CREATE_TEMPLATES:
                return 3;
            case CREATE_EVENT_CONFIGS:
            case FINALIZE:
                return 4;
            default:
                return 0;
        }
    }

    /**
     * Formats duration in seconds to human-readable format.
     *
     * @param seconds Duration in seconds
     * @return Formatted duration (e.g., "3m37s", "1h5m", "45s")
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m" + (remainingSeconds > 0 ? remainingSeconds + "s" : "");
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            return hours + "h" + (remainingMinutes > 0 ? remainingMinutes + "m" : "");
        }
    }
}
