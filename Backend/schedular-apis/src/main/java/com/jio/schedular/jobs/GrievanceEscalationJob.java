package com.jio.schedular.jobs;

import com.jio.schedular.service.GrievanceEscalationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers grievance escalation processing.
 */
@Component
@Slf4j
public class GrievanceEscalationJob {

    private final GrievanceEscalationService escalationService;

    @Value("${schedular.job.grievance-escalation.batch-size:500}")
    private int batchSize;

    @Autowired
    public GrievanceEscalationJob(GrievanceEscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(cron = "${schedular.job.grievance-escalation.cron:0 0 3 * * ?}")
    public void run() {
        log.info("Starting GrievanceEscalationJob - batchSize: {}", batchSize);
        try {
            int updated = escalationService.escalateGrievances(batchSize);
            log.info("GrievanceEscalationJob completed - escalated: {}", updated);
        } catch (Exception e) {
            log.error("Error executing GrievanceEscalationJob", e);
        }
    }
}