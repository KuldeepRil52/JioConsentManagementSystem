package com.jio.schedular.jobs;

import com.jio.schedular.service.CookieConsentRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers cookie consent artifact retention processing.
 */
@Component
@Slf4j
public class CookieConsentRetentionJob {

    private final CookieConsentRetentionService retentionService;

    @Value("${schedular.job.cookie-consent-retention.batch-size:500}")
    private int batchSize;

    @Autowired
    public CookieConsentRetentionJob(CookieConsentRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${schedular.job.cookie-consent-retention.cron:0 0 3 * * ?}")
    public void run() {
        log.info("Starting CookieConsentRetentionJob - batchSize: {}", batchSize);
        try {
            int processed = retentionService.processCookieConsentRetention(batchSize);
            log.info("CookieConsentRetentionJob completed - processed records: {}", processed);
        } catch (Exception e) {
            log.error("Error executing CookieConsentRetentionJob", e);
        }
    }
}