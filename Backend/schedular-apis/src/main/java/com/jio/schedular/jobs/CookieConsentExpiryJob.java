package com.jio.schedular.jobs;

import com.jio.schedular.service.CookieConsentExpiryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to mark expired consents as EXPIRED
 */
@Component
@Slf4j
public class CookieConsentExpiryJob {

    private final CookieConsentExpiryService cookieConsentExpiryService;

    @Autowired
    public CookieConsentExpiryJob(CookieConsentExpiryService cookieConsentExpiryService) {
        this.cookieConsentExpiryService = cookieConsentExpiryService;
    }

    @Scheduled(cron = "${schedular.job.cookie-consent-expiry.cron:0 0 0 * * *}")
    public void markExpiredCookieConsents() {
        log.info("Starting CookieConsentExpiryJob");
        try {
            int expiredCount = cookieConsentExpiryService.markExpiredCookieConsents();
            log.info("CookieConsentExpiryJob completed. Expired {} Cookie Consent(s)", expiredCount);
        } catch (Exception e) {
            log.error("Error occurred while executing CookieConsentExpiryJob", e);
        }
    }
}