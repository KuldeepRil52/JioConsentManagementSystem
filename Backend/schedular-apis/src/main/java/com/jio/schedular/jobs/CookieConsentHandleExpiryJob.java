package com.jio.schedular.jobs;

import com.jio.schedular.service.CookieConsentHandleExpiryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to mark expired consent handles as EXPIRED
 */
@Component
@Slf4j
public class CookieConsentHandleExpiryJob {

    private final CookieConsentHandleExpiryService cookieConsentHandleExpiryService;

    @Autowired
    public CookieConsentHandleExpiryJob(CookieConsentHandleExpiryService cookieConsentHandleExpiryService) {
        this.cookieConsentHandleExpiryService = cookieConsentHandleExpiryService;
    }

    @Scheduled(cron = "${schedular.job.cookie-consent-handle-expiry.cron:0 * * * * *}")
    public void markExpiredCookieConsentHandles() {
        log.info("Starting CookieConsentHandleExpiryJob");
        try {
            int expiredCount = cookieConsentHandleExpiryService.markExpiredCookieConsentHandles();
            log.info("CookieConsentHandleExpiryJob completed. Expired {} cookie consent handle(s)", expiredCount);
        } catch (Exception e) {
            log.error("Error occurred while executing CookieConsentHandleExpiryJob", e);
        }
    }
}