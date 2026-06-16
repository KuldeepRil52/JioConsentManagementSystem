package com.jio.schedular.jobs;

import com.jio.schedular.service.ConsentHandleExpiryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to expire consent handles that have been pending for the configured duration
 */
@Component
@Slf4j
public class ConsentHandleExpiryJob {

    private final ConsentHandleExpiryService consentHandleExpiryService;

    @Value("${schedular.job.consent-handle-expiry.batch-size:100}")
    private int batchSize;

    @Value("${schedular.job.consent-handle-expiry.period-type:MONTHS}")
    private String periodType;

    @Value("${schedular.job.consent-handle-expiry.period-value:1}")
    private int periodValue;

    @Autowired
    public ConsentHandleExpiryJob(ConsentHandleExpiryService consentHandleExpiryService) {
        this.consentHandleExpiryService = consentHandleExpiryService;
    }

    /**
     * Scheduled method to expire consent handles
     * Cron expression is configured in application.properties
     */
    @Scheduled(cron = "${schedular.job.consent-handle-expiry.cron:0 0 2 * * ?}")
    public void expireConsentHandles() {
        log.info("Starting ConsentHandleExpiryJob - Batch size: {}, Period: {} {}", 
                batchSize, periodValue, periodType);
        
        try {
            int expiredCount = consentHandleExpiryService.expirePendingConsentHandles(
                    batchSize, periodType, periodValue);
            
            log.info("ConsentHandleExpiryJob completed successfully. Expired {} consent handles", expiredCount);
            
        } catch (Exception e) {
            log.error("Error occurred while executing ConsentHandleExpiryJob", e);
        }
    }
}
