package com.example.scanner.service;

import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.response.CookieCategorizationResponse;
import com.example.scanner.dto.CookieDto;
import com.example.scanner.entity.CookieEntity;
import com.example.scanner.entity.ScanResultEntity;
import com.example.scanner.enums.SameSite;
import com.example.scanner.enums.ScanStatus;
import com.example.scanner.enums.Source;
import com.example.scanner.exception.ScanExecutionException;
import com.example.scanner.exception.ScannerException;
import com.example.scanner.exception.UrlValidationException;
import com.example.scanner.mapper.ScanResultMapper;
import com.example.scanner.util.CookieDetectionUtil;
import com.example.scanner.util.UrlAndCookieUtil;
import com.example.scanner.util.UrlAndCookieUtil.ValidationResult;
import com.example.scanner.util.SubdomainValidationUtil;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.scanner.config.TenantContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final CookieCategorizationService cookieCategorizationService;
    private final CookieScanMetrics metrics;
    private final MultiTenantMongoConfig mongoConfig;
    private final AuditService auditService;

    @Value("${scanner.browser.launch.timeout.ms:30000}")
    private int browserLaunchTimeout;

    @Value("${scanner.context.default.timeout.ms:15000}")
    private int contextDefaultTimeout;

    @Value("${scanner.context.navigation.timeout.ms:15000}")
    private int contextNavigationTimeout;

    @Value("${scanner.navigation.networkidle.timeout.ms:20000}")
    private int navigationNetworkIdleTimeout;

    @Value("${scanner.navigation.domcontentloaded.timeout.ms:15000}")
    private int navigationDomContentLoadedTimeout;

    @Value("${scanner.navigation.load.timeout.ms:10000}")
    private int navigationLoadTimeout;

    @Value("${scanner.wait.initial.load.ms:500}")
    private int waitInitialLoad;

    @Value("${scanner.wait.embedded.content.ms:1500}")
    private int waitEmbeddedContent;

    @Value("${scanner.wait.external.resources.ms:2000}")
    private int waitExternalResources;

    @Value("${scanner.wait.consent.handled.ms:2500}")
    private int waitConsentHandled;

    @Value("${scanner.wait.scroll.delay.ms:1000}")
    private int waitScrollDelay;

    @Value("${scanner.wait.analytics.trigger.ms:1500}")
    private int waitAnalyticsTrigger;

    @Value("${scanner.wait.cookie.sync.ms:4000}")
    private int waitCookieSync;

    @Value("${scanner.use.proxy:false}")
    private boolean useProxy;

    @Value("${scanner.proxy.url:null}")
    private String proxyUrl;

    @Lazy
    @Autowired
    private ScanService self;

    public String startScan(String tenantId, String url, List<String> subdomains)
            throws UrlValidationException, ScanExecutionException {
        log.info("Received request to scan URL: {} with {} subdomains", url, subdomains != null ? subdomains.size() : 0);

        try {
            ValidationResult validationResult = UrlAndCookieUtil.validateUrlForScanning(url);
            if (!validationResult.isValid()) {
                throw new UrlValidationException(ErrorCodes.VALIDATION_ERROR,
                        "Invalid URL provided",
                        validationResult.getErrorMessage()
                );
            }

            String normalizedUrl = validationResult.getNormalizedUrl();
            log.info("URL validation passed. Normalized URL: {}", normalizedUrl);

            List<String> validatedSubdomains = new ArrayList<>();
            if (subdomains != null && !subdomains.isEmpty()) {
                SubdomainValidationUtil.ValidationResult subdomainValidation =
                        SubdomainValidationUtil.validateSubdomains(normalizedUrl, subdomains);

                if (!subdomainValidation.isValid()) {
                    throw new UrlValidationException(ErrorCodes.VALIDATION_ERROR,
                            "Invalid subdomains provided",
                            "Subdomain validation failed: " + subdomainValidation.getErrorMessage()
                    );
                }

                validatedSubdomains = subdomainValidation.getValidatedSubdomains();
                log.info("Subdomain validation passed. {} valid subdomains", validatedSubdomains.size());
            }

            String transactionId = UUID.randomUUID().toString();

            auditService.logCookieScanInitiated(tenantId, transactionId);

            ScanResultEntity result = new ScanResultEntity();
            result.setId(transactionId);
            result.setTransactionId(transactionId);
            result.setStatus(ScanStatus.PENDING.name());
            result.setUrl(normalizedUrl);

            try {
                saveScanResultToTenant(tenantId, result);
                metrics.recordScanStarted();
            } catch (Exception e) {
                log.error("Failed to save scan result to database");
                throw new ScanExecutionException("Failed to initialize scan: " + e.getMessage());
            }

            log.info("Created new scan with transactionId={} for URL={} and {} subdomains",
                    transactionId, normalizedUrl, validatedSubdomains.size());
            self.runScanAsync(tenantId, transactionId, normalizedUrl, validatedSubdomains);

            return transactionId;

        } catch (UrlValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during scan initialization");
            throw new ScanExecutionException("Failed to start scan: " + e.getMessage());
        }
    }

    @Async
    public void runScanAsync(String tenantId, String transactionId, String url, List<String> subdomains) {
        log.info("Starting MAXIMUM COOKIE DETECTION scan for transactionId={} URL={} with {} subdomains",
                transactionId, url, subdomains != null ? subdomains.size() : 0);

        ScanPerformanceTracker.ScanMetrics scanMetrics = new ScanPerformanceTracker.ScanMetrics();
        long scanStartTime = System.currentTimeMillis();
        ScanResultEntity result = null;

        try {
            result = findScanResultFromTenant(tenantId, transactionId);
            result.setStatus(ScanStatus.RUNNING.name());
            saveScanResultToTenant(tenantId, result);

            auditService.logCookieScanStarted(tenantId, transactionId);

            scanMetrics.setScanPhase("RUNNING");
            performMaximumCookieDetection(url, transactionId, scanMetrics, subdomains, tenantId);

            result = findScanResultFromTenant(tenantId, transactionId);
            result.setStatus(ScanStatus.COMPLETED.name());
            saveScanResultToTenant(tenantId, result);

            scanMetrics.setScanPhase("COMPLETED");
            scanMetrics.markCompleted();

            Duration totalDuration = Duration.ofMillis(System.currentTimeMillis() - scanStartTime);
            metrics.recordScanCompleted(totalDuration);
            scanMetrics.logSummary(transactionId);

            log.info("MAXIMUM DETECTION scan COMPLETED for transactionId={} in {}ms",
                    transactionId, totalDuration.toMillis());

        } catch (Exception e) {
            scanMetrics.markFailed(e.getMessage());
            scanMetrics.setScanPhase("FAILED");

            auditService.logCookieScanFailed(tenantId, transactionId);

            Duration totalDuration = Duration.ofMillis(System.currentTimeMillis() - scanStartTime);

            log.error("Maximum detection scan FAILED for transactionId={} URL={} after {}ms",
                    transactionId, url, totalDuration.toMillis());

            if (result != null) {
                result.setStatus(ScanStatus.FAILED.name());
                result.setErrorMessage(getErrorMessage(e));
                try {
                    saveScanResultToTenant(tenantId, result);
                } catch (Exception saveEx) {
                    log.error("Failed to save error status for transactionId={}", transactionId);
                }
            }

            metrics.recordScanFailed(totalDuration);
            scanMetrics.logSummary(transactionId);
        }
    }

    private void performMaximumCookieDetection(String url, String transactionId,
                                               ScanPerformanceTracker.ScanMetrics scanMetrics,
                                               List<String> subdomains, String tenantId)
            throws ScanExecutionException {

        Playwright playwright = null;
        Browser browser = null;

        Map<String, CookieDto> discoveredCookies = new ConcurrentHashMap<>();
        Set<String> processedUrls = new HashSet<>();

        try {
            scanMetrics.setScanPhase("INITIALIZING_BROWSER");
            playwright = Playwright.create();

            Proxy proxy = null;

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                                .setHeadless(true)
                                .setTimeout(browserLaunchTimeout)
                                .setSlowMo(0);

            if(useProxy){
                proxy = new Proxy(proxyUrl); // Add a check if proxyUrl is null.
                launchOptions.setProxy(proxy);
            }

//            launchOptions.setProxy(proxy);
//            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
//                    .setHeadless(true)
//                    .setTimeout(browserLaunchTimeout)
//                    .setSlowMo(0);

            browser = playwright.chromium().launch(launchOptions);

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setIgnoreHTTPSErrors(false)
                    .setJavaScriptEnabled(true)
                    .setAcceptDownloads(false)
                    .setPermissions(Arrays.asList("geolocation", "notifications"));


            // **CREATE LIST OF ALL URLs TO SCAN COMPREHENSIVELY**
            List<ScanTarget> allTargetsToScan = new ArrayList<>();

            // Add main URL
            String rootDomain = UrlAndCookieUtil.extractRootDomain(url);
            allTargetsToScan.add(new ScanTarget(url, "main"));

            // Add all subdomains
            if (subdomains != null && !subdomains.isEmpty()) {
                for (String subdomain : subdomains) {
                    String subdomainName = SubdomainValidationUtil.extractSubdomainName(subdomain, rootDomain);
                    allTargetsToScan.add(new ScanTarget(subdomain, subdomainName));
                }
            }

            log.info("=== COMPREHENSIVE SCANNING: {} targets total (1 main + {} subdomains) ===",
                    allTargetsToScan.size(), subdomains != null ? subdomains.size() : 0);

            // **LOOP THROUGH ALL TARGETS WITH SAME COMPREHENSIVE PROCESS**
            for (int targetIndex = 0; targetIndex < allTargetsToScan.size(); targetIndex++) {
                ScanTarget target = allTargetsToScan.get(targetIndex);
                String targetUrl = target.url;
                String targetSubdomainName = target.subdomainName;

                BrowserContext context = null;
                Page page = null;

                try {
                    log.info("=== TARGET {}/{}: {} (Subdomain: {}) - CREATING NEW ISOLATED CONTEXT ===",
                            targetIndex + 1, allTargetsToScan.size(), targetUrl, targetSubdomainName);

                    // NAYA CONTEXT BANAO
                    context = browser.newContext(contextOptions);
                    context.setDefaultTimeout(contextDefaultTimeout);
                    context.setDefaultNavigationTimeout(contextNavigationTimeout);

                    // Request listener setup (har context ke liye alag)
                    Set<String> trackingDomains = ConcurrentHashMap.newKeySet();
                    context.onRequest(request -> {
                        String urltemp = request.url();
                        processedUrls.add(urltemp);

                        if (isTrackingRequest(urltemp)) {
                            try {
                                String domain = new java.net.URL(urltemp).getHost();
                                trackingDomains.add(domain);
                                log.debug("Detected tracking request: {}", urltemp);
                            } catch (java.net.MalformedURLException e) {
                                log.info("MalFormed URL ignored");
                            }
                        }
                    });

                    page = context.newPage();

                    // PHASE 1: LOADING PAGE WITH FULL WAIT
                    scanMetrics.setScanPhase("LOADING_PAGE_" + targetSubdomainName.toUpperCase());
                    log.info("=== PHASE 1: Loading {} with extended wait ===", targetSubdomainName);

                    Response response = null;
                    try {
                        response = page.navigate(targetUrl, new Page.NavigateOptions()
                                .setWaitUntil(WaitUntilState.NETWORKIDLE)
                                .setTimeout(navigationNetworkIdleTimeout));
                    } catch (TimeoutError e) {
                        log.warn("Networkidle timeout, trying with domcontentloaded for {}", targetUrl);
                        try {
                            response = page.navigate(targetUrl, new Page.NavigateOptions()
                                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                    .setTimeout(navigationDomContentLoadedTimeout));
                        } catch (TimeoutError e2) {
                            log.warn("Domcontentloaded timeout, trying basic load for {}", targetUrl);
                            response = page.navigate(targetUrl, new Page.NavigateOptions()
                                    .setWaitUntil(WaitUntilState.LOAD)
                                    .setTimeout(navigationLoadTimeout));
                        }
                    }

                    if (response == null || !response.ok()) {
                        log.warn("Failed to load {}: Status {}", targetUrl, response != null ? response.status() : "No response");
                        continue; // Skip this target but continue with others
                    }

                    page.waitForTimeout(waitInitialLoad);

                    // PHASE 3: EMBEDDED CONTENT CHECK
                    if ((Boolean) page.evaluate("document.querySelectorAll('iframe, embed, object').length > 0")) {
                        log.info("Embedded content detected on {} - extending wait time", targetSubdomainName);
                        page.waitForTimeout(waitEmbeddedContent);
                    }

                    // PHASE 4: EXTERNAL RESOURCE DETECTION
                    scanMetrics.setScanPhase("LOADING_EXTERNAL_RESOURCES_" + targetSubdomainName.toUpperCase());
                    log.info("=== PHASE 2: Generic external resource detection and triggering for {} ===", targetSubdomainName);

                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    page.waitForTimeout(waitExternalResources);

                    // PHASE 5: CONSENT BANNER HANDLING
                    scanMetrics.setScanPhase("HANDLING_CONSENT_" + targetSubdomainName.toUpperCase());
                    log.info("=== PHASE 3: Aggressive consent banner handling for {} ===", targetSubdomainName);

                    boolean consentHandled = CookieDetectionUtil.handleConsentBanners(page);

                    if (!consentHandled) {
                        log.info("Trying manual consent detection for {}...", targetSubdomainName);
                        try {
                            Boolean foundButton = (Boolean) page.evaluate(String.format("""
                        (function() {
                            let found = false;
                            let selectors = [
                                'button', 'a', 'div[role="button"]', 'span[role="button"]',
                                '[onclick]', '[data-testid]', '[data-cy]'
                            ];
                            
                            for (let selector of selectors) {
                                let elements = document.querySelectorAll(selector);
                                for (let elem of elements) {
                                    let text = (elem.textContent || elem.innerText || '').toLowerCase();
                                    let attrs = elem.outerHTML.toLowerCase();
                                    
                                    if (text.includes('accept') || text.includes('agree') || 
                                        text.includes('allow') || text.includes('consent') ||
                                        text.includes('continue') || text.includes('ok') ||
                                        attrs.includes('accept') || attrs.includes('consent')) {
                                        
                                        try {
                                            elem.click();
                                            console.log('Clicked consent element on %s:', text || attrs);
                                            found = true;
                                            break;
                                        } catch(e) {
                                            continue;
                                        }
                                    }
                                }
                                if (found) break;
                            }
                            return found;
                        })();
                    """, targetSubdomainName));

                            if (foundButton) {
                                consentHandled = true;
                                log.info("Manual consent handling successful for {}!", targetSubdomainName);
                            }
                        } catch (Exception e) {
                            log.debug("Manual consent detection failed for {}", targetSubdomainName);
                        }
                    }

                    if (consentHandled) {
                        page.waitForTimeout(waitConsentHandled);
                        if ("main".equals(targetSubdomainName)) {
                            captureBrowserCookiesEnhanced(context, targetUrl, discoveredCookies, transactionId, scanMetrics, tenantId);
                        } else {
                            captureBrowserCookiesWithSubdomainName(context, targetUrl, discoveredCookies,
                                    transactionId, scanMetrics, targetSubdomainName, tenantId);
                        }
                        log.info("Captured storage after consent handling for {}", targetSubdomainName);
                    }

                    // PHASE 6: USER INTERACTIONS
                    scanMetrics.setScanPhase("USER_INTERACTIONS_" + targetSubdomainName.toUpperCase());
                    log.info("=== PHASE 4: Aggressive user interaction simulation for {} ===", targetSubdomainName);

                    page.evaluate("""
                    window.scrollTo(0, document.body.scrollHeight * 0.3);
                """);
                    page.waitForTimeout(waitScrollDelay);

                    page.evaluate("""
                    window.scrollTo(0, document.body.scrollHeight * 0.7);
                """);
                    page.waitForTimeout(waitScrollDelay);

                    // Natural events (not artificial analytics calls)
                    page.evaluate("""
                    window.dispatchEvent(new Event('scroll'));
                    window.dispatchEvent(new Event('resize'));
                """);

                    // PHASE 7: ANALYTICS EVENT TRIGGERING
                    scanMetrics.setScanPhase("TRIGGERING_ANALYTICS_" + targetSubdomainName.toUpperCase());
                    log.info("=== PHASE 5: Generic analytics event triggering for {} ===", targetSubdomainName);

                    page.waitForTimeout(waitAnalyticsTrigger);

                    scanMetrics.setScanPhase("COOKIE_SYNC_DETECTION_" + targetSubdomainName.toUpperCase());

                    page.waitForTimeout(waitCookieSync);
                    if ("main".equals(targetSubdomainName)) {
                        captureBrowserCookiesEnhanced(context, targetUrl, discoveredCookies, transactionId, scanMetrics, tenantId);
                    } else {
                        captureBrowserCookiesWithSubdomainName(context, targetUrl, discoveredCookies,
                                transactionId, scanMetrics, targetSubdomainName, tenantId);
                    }

                    // PHASE 9: IFRAME PROCESSING
                    scanMetrics.setScanPhase("IFRAME_DETECTION_" + targetSubdomainName.toUpperCase());
                    log.info("=== PHASE 7: Enhanced iframe/embed detection for {} ===", targetSubdomainName);
                    handleIframes(context, targetUrl, discoveredCookies, transactionId, tenantId, targetSubdomainName);
                    log.info("Completed COMPREHENSIVE scanning of {}: {} (Total cookies: {})",
                            targetSubdomainName, targetUrl, discoveredCookies.size());

                } catch (Exception e) {
                    log.warn("Error during comprehensive scan of {}", targetUrl );
                    // Continue with next target
                } finally {
                    try {
                        if (page != null && !page.isClosed()) {
                            page.close();
                            log.debug("Closed page for {}", targetSubdomainName);
                        }
                    } catch (Exception ex) {
                        log.warn("Error closing page: " );
                    }

                    try {
                        if (context != null) {
                            context.close();
                            log.info("✅ CLOSED ISOLATED CONTEXT for {} - Cookie isolation complete!", targetSubdomainName);
                        }
                    } catch (Exception ex) {
                        log.warn("Error closing context");
                    }
                }
            }

            log.info("=== ENSURING ALL SCANNED SUBDOMAINS ARE SAVED IN DB ===");
            ScanResultEntity finalResult = findScanResultFromTenant(tenantId, transactionId);
            if (finalResult.getCookiesBySubdomain() == null) {
                finalResult.setCookiesBySubdomain(new HashMap<>());
            }

            for (ScanTarget target : allTargetsToScan) {
                String subdomainName = target.subdomainName;
                // Add entry for subdomain if it doesn't exist (even with empty list)
                finalResult.getCookiesBySubdomain().computeIfAbsent(subdomainName, k -> new ArrayList<>());
                log.debug("Ensured subdomain '{}' exists in DB", subdomainName);
            }

            saveScanResultToTenant(tenantId, finalResult);
            log.info("Successfully saved all {} scanned subdomains to DB", allTargetsToScan.size());

            log.info("MAXIMUM DETECTION scan completed. Total unique cookies: {}, Network requests: {}, Targets scanned: {}",
                    discoveredCookies.size(), processedUrls.size(), allTargetsToScan.size());

        } catch (PlaywrightException e) {
            throw new ScanExecutionException("Playwright error during scan: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ScanExecutionException("Unexpected error during scan: " + e.getMessage());
        } finally {
            cleanupResources(null, browser, playwright);
        }
    }

    // Helper class for scan targets
    private static class ScanTarget {
        final String url;
        final String subdomainName;

        ScanTarget(String url, String subdomainName) {
            this.url = url;
            this.subdomainName = subdomainName;
        }
    }

    private void handleIframes(BrowserContext context, String url, Map<String, CookieDto> discoveredCookies,
                               String transactionId, String tenantId, String subdomainName) {
        try {
            List<Cookie> allContextCookies = context.cookies();

            for (Cookie cookie : allContextCookies) {
                // FIX: Use proper subdomain name
                String cookieKey = generateCookieKey(cookie.name, cookie.domain, subdomainName);

                if (!discoveredCookies.containsKey(cookieKey)) {
                    CookieDto cookieDto = mapPlaywrightCookie(cookie, url, UrlAndCookieUtil.extractRootDomain(url));
                    cookieDto.setSubdomainName(subdomainName);

                    discoveredCookies.put(cookieKey, cookieDto);
                    saveIncrementalCookieWithFlush(tenantId, transactionId, cookieDto);
                }
            }

            log.debug("Captured {} iframe cookies for subdomain: {}", allContextCookies.size(), subdomainName);

        } catch (Exception e) {
            log.warn("Error capturing iframe cookies for subdomain {}" ,subdomainName);
        }
    }

    private void captureBrowserCookiesWithSubdomainName(BrowserContext context, String scanUrl,
                                                        Map<String, CookieDto> discoveredCookies,
                                                        String transactionId,
                                                        ScanPerformanceTracker.ScanMetrics scanMetrics,
                                                        String subdomainName, String tenantId) {
        try {
            String siteRoot = UrlAndCookieUtil.extractRootDomain(scanUrl);
            List<Cookie> browserCookies = context.cookies();

            log.info("=== CAPTURING {} browser cookies for subdomain: {} ===", browserCookies.size(), subdomainName);

            List<CookieDto> cookiesToSave = new ArrayList<>();
            for (Cookie playwrightCookie : browserCookies) {
                try {
                    CookieDto cookieDto = mapPlaywrightCookie(playwrightCookie, scanUrl, siteRoot);
                    cookieDto.setSubdomainName(subdomainName);

                    String cookieKey = generateCookieKey(cookieDto.getName(), cookieDto.getDomain(), cookieDto.getSubdomainName());

                    if (!discoveredCookies.containsKey(cookieKey)) {
                        discoveredCookies.put(cookieKey, cookieDto);
                        cookiesToSave.add(cookieDto);  // ADD to save list instead of immediate save
                        scanMetrics.incrementCookiesFound(cookieDto.getSource().name());
                        metrics.recordCookieDiscovered(cookieDto.getSource().name());
                        log.debug("Collected SUBDOMAIN BROWSER COOKIE: {} from domain {} (Source: {}, Subdomain: {})",
                                cookieDto.getName(), cookieDto.getDomain(), cookieDto.getSource(), subdomainName);
                    }
                } catch (Exception e) {
                    log.warn("Error processing browser cookie {} ", playwrightCookie.name);
                }
            }

            categorizeCookiesAndSave(tenantId, cookiesToSave, transactionId);

            log.info("Completed capturing and saving {} subdomain cookies", cookiesToSave.size());

        } catch (Exception e) {
            log.warn("Error capturing browser cookies for subdomain {}", subdomainName);
        }
    }

    private void captureBrowserCookiesEnhanced(BrowserContext context, String scanUrl,
                                               Map<String, CookieDto> discoveredCookies, String transactionId,
                                               ScanPerformanceTracker.ScanMetrics scanMetrics, String tenantId) {
        try {
            String siteRoot = UrlAndCookieUtil.extractRootDomain(scanUrl);
            List<Cookie> browserCookies = context.cookies();

            log.info("=== CAPTURING {} browser cookies ===", browserCookies.size());
            List<CookieDto> cookiesToSave = new ArrayList<>();

            for (Cookie playwrightCookie : browserCookies) {
                try {
                    CookieDto cookieDto = mapPlaywrightCookie(playwrightCookie, scanUrl, siteRoot);
                    cookieDto.setSubdomainName("main");

                    String cookieKey = generateCookieKey(cookieDto.getName(), cookieDto.getDomain(), cookieDto.getSubdomainName());

                    if (!discoveredCookies.containsKey(cookieKey)) {
                        discoveredCookies.put(cookieKey, cookieDto);
                        cookiesToSave.add(cookieDto);  // ADD to save list instead of immediate save
                        scanMetrics.incrementCookiesFound(cookieDto.getSource().name());
                        metrics.recordCookieDiscovered(cookieDto.getSource().name());
                        log.debug("Collected BROWSER COOKIE: {} from domain {} (Source: {})",
                                cookieDto.getName(), cookieDto.getDomain(), cookieDto.getSource());
                    }
                } catch (Exception e) {
                    log.warn("Exception in processing browser cookie {}", playwrightCookie.name);
                }
            }
            categorizeCookiesAndSave(tenantId, cookiesToSave, transactionId);

            log.info("Completed capturing and saving {} browser cookies", cookiesToSave.size());

        } catch (Exception e) {
            log.warn("Error capturing browser cookies");
        }
    }

    private CookieDto mapPlaywrightCookie(Cookie playwrightCookie, String scanUrl, String siteRoot) {
        Instant expiry = playwrightCookie.expires != null ?
                Instant.ofEpochSecond(playwrightCookie.expires.longValue()) : null;

        String cookieDomain = playwrightCookie.domain;
        Source source = determineSourceType(cookieDomain, siteRoot);
        SameSite sameSite = parseSameSiteAttribute(playwrightCookie.sameSite);

        // Extract provider name from domain
        String provider = null;

        return new CookieDto(
                playwrightCookie.name,
                scanUrl,
                cookieDomain,
                playwrightCookie.path != null ? playwrightCookie.path : "/",
                expiry,
                playwrightCookie.secure,
                playwrightCookie.httpOnly,
                sameSite,
                source,
                null, // category - will be set by categorization service
                null, // description - will be set by categorization service
                null, // description_gpt - will be set by categorization service
                "main", // subdomainName - will be updated later
                null, // privacyPolicyUrl - not available during scanning
                provider // NEW: provider name extracted from domain
        );
    }

    private void saveIncrementalCookieWithFlush(String tenantId, String transactionId, CookieDto cookieDto) {
        try {
            ScanResultEntity result = findScanResultFromTenant(tenantId, transactionId);
            if (result != null) {
                if (result.getCookiesBySubdomain() == null) {
                    result.setCookiesBySubdomain(new HashMap<>());
                }

                String subdomainName = cookieDto.getSubdomainName() != null ? cookieDto.getSubdomainName() : "main";

                List<CookieEntity> subdomainCookies = result.getCookiesBySubdomain()
                        .computeIfAbsent(subdomainName, k -> new ArrayList<>());

                boolean exists = subdomainCookies.stream()
                        .anyMatch(c -> c.getName().equals(cookieDto.getName()) &&
                                Objects.equals(c.getDomain(), cookieDto.getDomain()));

                if (!exists) {
                    CookieEntity cookieEntity = ScanResultMapper.cookieDtoToEntity(cookieDto);
                    subdomainCookies.add(cookieEntity);
                    saveScanResultToTenant(tenantId, result);

                    log.debug("✅ Saved cookie: {} to subdomain: {}", cookieDto.getName(), subdomainName);
                } else {
                    log.debug("⚠️ Duplicate cookie skipped: {} in subdomain: {}", cookieDto.getName(), subdomainName);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to save cookie exception occurred. {}", cookieDto.getName());
        }
    }

    private Source determineSourceType(String cookieDomain, String scanDomain) {
        if (cookieDomain == null || scanDomain == null) {
            return Source.UNKNOWN;
        }

        String cleanCookieDomain = cookieDomain.startsWith(".") ?
                cookieDomain.substring(1) : cookieDomain;

        String cookieRoot = UrlAndCookieUtil.extractRootDomain(cleanCookieDomain);
        String scanRoot = UrlAndCookieUtil.extractRootDomain(scanDomain);

        return scanRoot.equalsIgnoreCase(cookieRoot) ? Source.FIRST_PARTY : Source.THIRD_PARTY;
    }

    private SameSite parseSameSiteAttribute(Object sameSiteObj) {
        if (sameSiteObj == null) return null;

        try {
            return SameSite.valueOf(sameSiteObj.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown SameSite value: {}",sameSiteObj );
            return SameSite.LAX;
        }
    }

    private String generateCookieKey(String name, String domain, String subdomainName) {
        return name + "@" + (domain != null ? domain.toLowerCase() : "") +
                "#" + (subdomainName != null ? subdomainName : "main");
    }

    private void cleanupResources(BrowserContext context, Browser browser, Playwright playwright) {
        try {
            if (context != null) {
                List<Page> pages = context.pages();
                for (Page page : pages) {
                    try {
                        if (!page.isClosed()) page.close();
                    } catch (Exception ignored) {}
                }
                context.close();
            }
        } catch (Exception e) {
            log.warn("Error closing context:");
        }

        try {
            if (browser != null && browser.isConnected()) {
                browser.close();
            }
        } catch (Exception e) {
            log.warn("Error closing browser:");
        }

        try {
            if (playwright != null) playwright.close();
        } catch (Exception e) {
            log.warn("Error closing playwright: ");
        }
        System.gc();
    }

    private String getErrorMessage(Exception e) {
        if (e instanceof ScannerException) {
            return ((ScannerException) e).getUserMessage();
        }
        return "An unexpected error occurred during scanning";
    }

    private void categorizeCookiesAndSave(String tenantId, List<CookieDto> cookiesToSave, String transactionId) {
        if (cookiesToSave.isEmpty()) {
            return;
        }

        try {
            Set<String> uniqueCookieNames = cookiesToSave.stream()
                    .map(CookieDto::getName)
                    .collect(Collectors.toSet());

            List<String> cookieNames = new ArrayList<>(uniqueCookieNames);
            log.info("Categorizing {} unique cookie names with ONE API call (from {} cookies to save)",
                    cookieNames.size(), cookiesToSave.size());

            Map<String, CookieCategorizationResponse> results =
                    cookieCategorizationService.categorizeCookies(cookieNames,tenantId);

            for (CookieDto cookie : cookiesToSave) {
                String cookieName = cookie.getName();
                CookieCategorizationResponse response = results.get(cookieName);

                if (response != null) {
                    cookie.setCategory(response.getCategory());
                    cookie.setDescription(response.getDescription());
                    cookie.setDescription_gpt(response.getDescription_gpt());
                    log.debug("Applied categorization to cookie '{}'", cookieName);
                } else {
                    log.warn("No categorization response found for cookie '{}'", cookieName);
                }
            }

            for (CookieDto categorizedCookie : cookiesToSave) {
                saveIncrementalCookieWithFlush(tenantId, transactionId, categorizedCookie);
            }

            log.info("Successfully categorized and saved {} cookies", cookiesToSave.size());

        } catch (Exception e) {
            log.error("Failed to categorize and save cookies: ");

            log.info("Falling back to save cookies without categorization");
            for (CookieDto cookie : cookiesToSave) {
                try {
                    saveIncrementalCookieWithFlush(tenantId, transactionId, cookie);
                } catch (Exception saveEx) {
                    log.warn("Failed to save cookie {}", cookie.getName());
                }
            }
        }
    }

    private void saveScanResultToTenant(String tenantId, ScanResultEntity result) {
        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            tenantMongoTemplate.save(result);
        } finally {
            TenantContext.clear();
        }
    }

    public Optional<ScanResultEntity> getScanResult(String tenantId, String transactionId) {
        return Optional.ofNullable(findScanResultFromTenant(tenantId, transactionId));
    }

    private ScanResultEntity findScanResultFromTenant(String tenantId, String transactionId) {
        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query(Criteria.where("transactionId").is(transactionId));
            return tenantMongoTemplate.findOne(query, ScanResultEntity.class);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isTrackingRequest(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();

        // Known tracking patterns
        return lowerUrl.contains("/collect") ||
                lowerUrl.contains("/analytics") ||
                lowerUrl.contains("/track") ||
                lowerUrl.contains("/pixel") ||
                lowerUrl.contains("/beacon") ||
                lowerUrl.contains("/impression") ||
                lowerUrl.contains("google-analytics.com") ||
                lowerUrl.contains("googletagmanager.com") ||
                lowerUrl.contains("facebook.com/tr") ||
                lowerUrl.contains("doubleclick.net") ||
                lowerUrl.matches(".*\\.(gif|png)\\?.*") || // Pixel images with parameters
                lowerUrl.contains("_ga=") ||
                lowerUrl.contains("_gid=") ||
                lowerUrl.contains("fbclid=");
    }

}