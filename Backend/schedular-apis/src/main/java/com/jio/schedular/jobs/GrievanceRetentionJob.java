package com.jio.schedular.jobs;

import com.jio.schedular.service.GrievanceRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers grievance retention processing.
 */
@Component
@Slf4j
public class GrievanceRetentionJob {

    private final GrievanceRetentionService retentionService;

    @Value("${schedular.job.grievance-retention.batch-size:500}")
    private int batchSize;

    @Autowired
    public GrievanceRetentionJob(GrievanceRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${schedular.job.grievance-retention.cron:0 0 3 * * ?}")
    public void run() {
        log.info("Starting GrievanceRetentionJob - batchSize: {}", batchSize);
        try {
            int processed = retentionService.processGrievanceRetention(batchSize);
            log.info("GrievanceRetentionJob completed - processed records: {}", processed);
        } catch (Exception e) {
            log.error("Error executing GrievanceRetentionJob", e);
        }
    }
}