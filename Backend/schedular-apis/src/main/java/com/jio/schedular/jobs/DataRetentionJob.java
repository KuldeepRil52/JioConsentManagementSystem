package com.jio.schedular.jobs;

import com.jio.schedular.service.DataRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers general data retention processing.
 */
@Component
@Slf4j
public class DataRetentionJob {

    private final DataRetentionService retentionService;

    @Value("${schedular.job.data-retention.batch-size:500}")
    private int batchSize;

    @Autowired
    public DataRetentionJob(DataRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${schedular.job.data-retention.cron:0 0 3 * * ?}")
    public void run() {
        log.info("Starting DataRetentionJob - batchSize: {}", batchSize);
        try {
            int processed = retentionService.processDataRetention(batchSize);
            log.info("DataRetentionJob completed - processed records: {}", processed);
        } catch (Exception e) {
            log.error("Error executing DataRetentionJob", e);
        }
    }
}