package com.jio.schedular.jobs;

import com.jio.schedular.service.ConsentExpiryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to expire preferences and consents when their end date has passed.
 */
@Component
@Slf4j
public class ConsentExpiryJob {

    private final ConsentExpiryService consentExpiryService;

    @Value("${schedular.job.consent-expiry.batch-size:1000}")
    private int batchSize;

    @Autowired
    public ConsentExpiryJob(ConsentExpiryService consentExpiryService) {
        this.consentExpiryService = consentExpiryService;
    }

    @Scheduled(cron = "${schedular.job.consent-expiry.cron:0 30 2 * * ?}")
    public void expireConsents() {
        log.info("Starting ConsentExpiryJob - batchSize: {}", batchSize);
        try {
            int expiredCount = consentExpiryService.expireConsentsAndPreferences(batchSize);
            log.info("ConsentExpiryJob completed. Expired {} record(s)", expiredCount);
        } catch (Exception e) {
            log.error("Error occurred while executing ConsentExpiryJob", e);
        }
    }
}