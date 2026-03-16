package com.example.scanner.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CookieScanMetrics {

    private static final Logger log = LoggerFactory.getLogger(CookieScanMetrics.class);

    private final Counter scansStarted;
    private final Counter scansCompleted;
    private final Counter scansFailed;
    private final Counter cookiesDiscovered;
    private final Counter consentBannersHandled;
    private final Counter thirdPartyCookies;
    private final Counter firstPartyCookies;

    public final Timer scanDuration;
    public final Timer consentHandlingDuration;
    public final Timer cookieCategorizationDuration;

    private final AtomicInteger activeScanCount = new AtomicInteger(0);

    public CookieScanMetrics(MeterRegistry meterRegistry) {
        this.scansStarted = Counter.builder("cookie_scans_started_total")
                .description("Total number of cookie scans started")
                .register(meterRegistry);

        this.scansCompleted = Counter.builder("cookie_scans_completed_total")
                .description("Total number of cookie scans completed successfully")
                .register(meterRegistry);

        this.scansFailed = Counter.builder("cookie_scans_failed_total")
                .description("Total number of cookie scans failed")
                .register(meterRegistry);

        this.cookiesDiscovered = Counter.builder("cookies_discovered_total")
                .description("Total number of cookies discovered")
                .tag("type", "all")
                .register(meterRegistry);

        this.consentBannersHandled = Counter.builder("consent_banners_handled_total")
                .description("Total number of consent banners successfully handled")
                .register(meterRegistry);

        this.thirdPartyCookies = Counter.builder("cookies_discovered_total")
                .description("Total number of third-party cookies discovered")
                .tag("type", "third_party")
                .register(meterRegistry);

        this.firstPartyCookies = Counter.builder("cookies_discovered_total")
                .description("Total number of first-party cookies discovered")
                .tag("type", "first_party")
                .register(meterRegistry);

        this.scanDuration = Timer.builder("cookie_scan_duration_seconds")
                .description("Duration of cookie scans")
                .register(meterRegistry);

        this.consentHandlingDuration = Timer.builder("consent_handling_duration_seconds")
                .description("Duration of consent banner handling")
                .register(meterRegistry);

        this.cookieCategorizationDuration = Timer.builder("cookie_categorization_duration_seconds")
                .description("Duration of cookie categorization")
                .register(meterRegistry);

        // Gauge for active scans
        meterRegistry.gauge("cookie_scans_active", activeScanCount);
    }

    public void recordScanStarted() {
        scansStarted.increment();
        activeScanCount.incrementAndGet();
        log.debug("Scan started. Active scans: {}", activeScanCount.get());
    }

    public void recordScanCompleted(Duration duration) {
        scansCompleted.increment();
        activeScanCount.decrementAndGet();
        scanDuration.record(duration);
        log.debug("Scan completed in {}ms. Active scans: {}", duration.toMillis(), activeScanCount.get());
    }

    public void recordScanFailed(Duration duration) {
        scansFailed.increment();
        activeScanCount.decrementAndGet();
        scanDuration.record(duration);
        log.debug("Scan failed after {}ms. Active scans: {}", duration.toMillis(), activeScanCount.get());
    }

    public void recordCookieDiscovered(String cookieType) {
        cookiesDiscovered.increment();

        if ("FIRST_PARTY".equals(cookieType)) {
            firstPartyCookies.increment();
        } else if ("THIRD_PARTY".equals(cookieType)) {
            thirdPartyCookies.increment();
        }
    }

    public void recordConsentBannerHandled(Duration duration) {
        consentBannersHandled.increment();
        consentHandlingDuration.record(duration);
        log.debug("Consent banner handled in {}ms", duration.toMillis());
    }

    public void recordCookieCategorizationTime(Duration duration) {
        cookieCategorizationDuration.record(duration);
    }

    public Timer.Sample startScanTimer() {
        return Timer.start();
    }

    public Timer.Sample startConsentTimer() {
        return Timer.start();
    }

    public Timer.Sample startCategorizationTimer() {
        return Timer.start();
    }

    // ADDED HELPER METHODS - YE BHI FIX HAI
    public void stopTimer(Timer.Sample sample, Timer timer) {
        sample.stop(timer);
    }
}

/**
 * Scan performance tracker for detailed monitoring
 */
@Component
class ScanPerformanceTracker {

    private static final Logger log = LoggerFactory.getLogger(ScanPerformanceTracker.class);

    public static class ScanMetrics {
        private final Instant startTime;
        private Instant endTime;
        private int cookiesFound = 0;
        private int thirdPartyCookies = 0;
        private int firstPartyCookies = 0;
        private boolean consentHandled = false;
        private Duration consentHandlingTime = Duration.ZERO;
        private int networkRequests = 0;
        private int iframesProcessed = 0;
        private String scanPhase = "INITIALIZING";
        private String errorMessage;
        private int interactions = 0;

        public void incrementInteractions() {
            this.interactions++;
        }

        public ScanMetrics() {
            this.startTime = Instant.now();
        }

        public void markCompleted() {
            this.endTime = Instant.now();
        }

        private final Map<String, Object> customMetrics = new ConcurrentHashMap<>();

        public void addCustomMetric(String key, Object value) {
            customMetrics.put(key, value);
        }

        public Object getCustomMetric(String key) {
            return customMetrics.get(key);
        }

        public void markFailed(String error) {
            this.endTime = Instant.now();
            this.errorMessage = error;
        }

        public Duration getTotalDuration() {
            Instant end = endTime != null ? endTime : Instant.now();
            return Duration.between(startTime, end);
        }

        public void incrementCookiesFound(String source) {
            cookiesFound++;
            if ("FIRST_PARTY".equals(source)) {
                firstPartyCookies++;
            } else if ("THIRD_PARTY".equals(source)) {
                thirdPartyCookies++;
            }
        }

        public void setConsentHandled(boolean handled, Duration duration) {
            this.consentHandled = handled;
            this.consentHandlingTime = duration;
        }

        public void incrementNetworkRequests() {
            networkRequests++;
        }

        public void incrementIframesProcessed() {
            iframesProcessed++;
        }

        public void setScanPhase(String phase) {
            this.scanPhase = phase;
            log.debug("Scan phase changed to: {}", phase);
        }

        // Getters
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public int getCookiesFound() { return cookiesFound; }
        public int getThirdPartyCookies() { return thirdPartyCookies; }
        public int getFirstPartyCookies() { return firstPartyCookies; }
        public boolean isConsentHandled() { return consentHandled; }
        public Duration getConsentHandlingTime() { return consentHandlingTime; }
        public int getNetworkRequests() { return networkRequests; }
        public int getIframesProcessed() { return iframesProcessed; }
        public String getScanPhase() { return scanPhase; }
        public String getErrorMessage() { return errorMessage; }

        public void logSummary(String transactionId) {
            log.info("""
                Scan Summary for Transaction: {}
                =====================================
                Duration: {}ms
                Phase: {}
                Cookies Found: {} (1st: {}, 3rd: {})
                Consent Handled: {} ({}ms)
                Network Requests: {}
                Iframes Processed: {}
                Status: {}
                """,
                    transactionId,
                    getTotalDuration().toMillis(),
                    scanPhase,
                    cookiesFound, firstPartyCookies, thirdPartyCookies,
                    consentHandled, consentHandlingTime.toMillis(),
                    networkRequests,
                    iframesProcessed,
                    errorMessage != null ? "FAILED - " + errorMessage : "SUCCESS"
            );
        }
    }
}