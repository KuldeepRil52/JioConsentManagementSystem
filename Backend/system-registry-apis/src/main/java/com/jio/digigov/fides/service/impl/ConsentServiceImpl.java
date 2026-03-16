package com.jio.digigov.fides.service.impl;

import com.jio.digigov.fides.client.audit.AuditManager;
import com.jio.digigov.fides.client.audit.request.Actor;
import com.jio.digigov.fides.client.audit.request.AuditRequest;
import com.jio.digigov.fides.client.audit.request.Context;
import com.jio.digigov.fides.client.audit.request.Resource;
import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.constant.Constants;
import com.jio.digigov.fides.constant.HeaderConstants;
import com.jio.digigov.fides.dto.request.WithdrawlRequest;
import com.jio.digigov.fides.entity.ConsentWithdrawalJob;
import com.jio.digigov.fides.enumeration.ActionType;
import com.jio.digigov.fides.enumeration.AuditComponent;
import com.jio.digigov.fides.enumeration.Group;
import com.jio.digigov.fides.service.ConsentAsyncProcessor;
import com.jio.digigov.fides.service.ConsentService;
import com.jio.digigov.fides.util.HeaderValidationService;
import com.jio.digigov.fides.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentServiceImpl implements ConsentService {

    private final HeaderValidationService headerValidationService;
    private final ConsentAsyncProcessor asyncProcessor;
    private final MultiTenantMongoConfig mongoConfig;
    private final AuditManager auditManager;

    public ConsentWithdrawalJob withdrawConsent(
            String consentId,
            String tenantId,
            String businessId,
            WithdrawlRequest request) {

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            com.jio.digigov.fides.entity.NotificationEvent event = mongoTemplate.findOne(
                        new Query(Criteria.where("eventId").is(request.getEventId())),
                        com.jio.digigov.fides.entity.NotificationEvent.class
                );
        
            if (event == null) {
                throw new IllegalStateException("NotificationEvent not found for eventId: " + request.getEventId());
            }

            ConsentWithdrawalJob job = new ConsentWithdrawalJob();
            job.setConsentId(consentId);
            job.setTenantId(tenantId);
            job.setBusinessId(businessId);
            job.setStatus("PENDING");
            job.setEventId(request.getEventId());
            job.setCreatedAt(Instant.now());
            job.setUpdatedAt(Instant.now());

            // Save job
            ConsentWithdrawalJob savedJob = mongoTemplate.save(job);

            asyncProcessor.process(job);
            logJobInitiatedAudit(job.getId(), consentId, tenantId, businessId, ActionType.INITIATED);

            return savedJob;
        } finally {
            TenantContextHolder.clear();
        }
    }

    public void logJobInitiatedAudit(String jobId, String consentId, String tenantId, String businessId, ActionType actionType) {
        try {

            Actor actor = Actor.builder()
                    .id(jobId)
                    .role(Constants.SYSTEM)
                    .type(Constants.JOB_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_ID)
                    .id(consentId)
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : UUID.randomUUID().toString())
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : Constants.IP_ADDRESS)
                    .build();

            Map<String, Object> extra = new HashMap<>();

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(businessId)
                    .group(String.valueOf(Group.DATA))
                    .component(AuditComponent.DATA_ERASURE_JOB)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SYSTEM)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for job id: {}, consent id: {}, action: {}, error: {}", 
                    jobId, consentId, actionType, e.getMessage(), e);
        }
    }
}