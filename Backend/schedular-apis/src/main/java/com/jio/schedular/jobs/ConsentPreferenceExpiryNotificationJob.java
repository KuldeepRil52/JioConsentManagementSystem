package com.jio.schedular.jobs;

import com.jio.schedular.service.ConsentPreferenceExpiryNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers preference expiry reminder notifications.
 */
@Component
@Slf4j
public class ConsentPreferenceExpiryNotificationJob {

    private final ConsentPreferenceExpiryNotificationService reminderService;

    @Value("${schedular.job.consent-preference-expiry.batch-size:500}")
    private int batchSize;

    @Autowired
    public ConsentPreferenceExpiryNotificationJob(ConsentPreferenceExpiryNotificationService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * Runs on cron configured in application.properties.
     */
    @Scheduled(cron = "${schedular.job.consent-preference-expiry.cron:0 15 2 * * ?}")
    public void run() {
        log.info("Starting ConsentPreferenceExpiryNotificationJob - batchSize: {}", batchSize);
        try {
            int sent = reminderService.sendPreferenceExpiryReminders(batchSize);
            log.info("ConsentPreferenceExpiryNotificationJob completed - notifications sent: {}", sent);
        } catch (Exception e) {
            log.error("Error executing ConsentPreferenceExpiryNotificationJob", e);
        }
    }
}