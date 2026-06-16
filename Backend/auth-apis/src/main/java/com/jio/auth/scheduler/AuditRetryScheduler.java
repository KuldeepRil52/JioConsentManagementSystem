package com.jio.auth.scheduler;

import com.jio.auth.model.FailedAudit;
import com.jio.auth.dto.AuditDto;
import com.jio.auth.repository.FailedAuditRepository;
import com.jio.auth.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRetryScheduler {

    private final FailedAuditRepository failedRepo;
    private final AuditService auditService;

    private static final int BATCH_SIZE = 500;

    @Scheduled(fixedDelay = 5000)
    public void retryFailedAudits() {

        log.info("==== AUDIT RETRY SCHEDULER TRIGGERED ====");

        int pageIndex = 0;
        int totalProcessed = 0;

        while (true) {
            Page<FailedAudit> batch = failedRepo.findAll(PageRequest.of(pageIndex, BATCH_SIZE));

            if (batch.isEmpty()) {
                log.info("No failed audits found. Scheduler cycle complete.");
                break;
            }

            log.info("Fetched batch #{} with {} failed audits", pageIndex, batch.getContent().size());

            for (FailedAudit fa : batch) {

                log.info("Retrying auditId={} tenantId={} businessId={}",
                        fa.getId(), fa.getTenantId(), fa.getBusinessId());

                try {
                    AuditDto dto = toDto(fa);

                    auditService.sendAudit(dto);

                    log.info("Audit retry success for id={} — deleting from DB", fa.getId());
                    failedRepo.deleteById(fa.getId());

                } catch (Exception ex) {
                    log.error("Retry failed for auditId={} — WILL RETRY LATER", fa.getId());
                }

                totalProcessed++;
            }

            pageIndex++;
        }

        log.info("==== AUDIT RETRY SCHEDULER COMPLETED — Processed {} audits ====", totalProcessed);
    }

    private AuditDto toDto(FailedAudit fa) {
        AuditDto dto = new AuditDto();
        dto.setTxnId(fa.getTxnId());
        dto.setTenantId(fa.getTenantId());
        dto.setBusinessId(fa.getBusinessId());
        dto.setTransactionId(fa.getTransactionId());
        dto.setIp(fa.getIp());
        dto.setRequestUri(fa.getRequestUri());
        dto.setMethod(fa.getMethod());
        dto.setRequestPayload(fa.getRequestPayload());
        dto.setResponsePayload(fa.getResponsePayload());
        dto.setActor(fa.getActor());
        return dto;
    }
}
