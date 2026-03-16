package com.jio.schedular.jobs;

import com.jio.schedular.service.LogsRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers logs retention processing.
 */
@Component
@Slf4j
public class LogsRetentionJob {

    private final LogsRetentionService retentionService;

    @Value("${schedular.job.logs-retention.batch-size:500}")
    private int batchSize;

    @Autowired
    public LogsRetentionJob(LogsRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${schedular.job.logs-retention.cron:0 0 3 * * ?}")
    public void run() {
        log.info("Starting LogsRetentionJob - batchSize: {}", batchSize);
        try {
            int processed = retentionService.processLogsRetention(batchSize);
            log.info("LogsRetentionJob completed - processed records: {}", processed);
        } catch (Exception e) {
            log.error("Error executing LogsRetentionJob", e);
        }
    }
}