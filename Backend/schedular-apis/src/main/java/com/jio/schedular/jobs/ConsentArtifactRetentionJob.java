package com.jio.schedular.jobs;

import com.jio.schedular.service.ConsentArtifactRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers consent artifact retention processing.
 */
@Component
@Slf4j
public class ConsentArtifactRetentionJob {

    private final ConsentArtifactRetentionService retentionService;

    @Value("${schedular.job.consent-artifact-retention.batch-size:500}")
    private int batchSize;

    @Autowired
    public ConsentArtifactRetentionJob(ConsentArtifactRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${schedular.job.consent-artifact-retention.cron:0 0 3 * * ?}")
    public void run() {
        log.info("Starting ConsentArtifactRetentionJob - batchSize: {}", batchSize);
        try {
            int processed = retentionService.processConsentArtifactRetention(batchSize);
            log.info("ConsentArtifactRetentionJob completed - processed records: {}", processed);
        } catch (Exception e) {
            log.error("Error executing ConsentArtifactRetentionJob", e);
        }
    }
}