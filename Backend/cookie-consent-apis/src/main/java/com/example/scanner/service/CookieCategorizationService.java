package com.example.scanner.service;

import com.example.scanner.dto.request.CookieCategorizationRequest;
import com.example.scanner.dto.response.CookieCategorizationResponse;
import com.example.scanner.exception.CookieCategorizationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CookieCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(CookieCategorizationService.class);

    @Value("${cookie.categorization.api.url}")
    private String categorizationApiUrl;

    @Value("${cookie.categorization.cache.enabled}")
    private boolean cacheEnabled;

    @Value("${cookie.categorization.cache.ttl.minutes:60}")
    private long cacheTtlMinutes;

    @Value("${cookie.categorization.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    @Value("${cookie.categorization.use-external-api:false}")
    private boolean useExternalApi;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CategoryService categoryService;
    private final CookieCsvLoaderService cookieCsvLoaderService;
    private final Map<String, CacheEntry> categorizationCache = new ConcurrentHashMap<>();


    /**
     * Categorize cookies with retry mechanism
     */
    public Map<String, CookieCategorizationResponse> categorizeCookies(List<String> cookieNames, String tenantId) throws CookieCategorizationException {
        if (cookieNames == null || cookieNames.isEmpty()) {
            log.debug("No cookie names provided for categorization");
            return Collections.emptyMap();
        }

        try {
            log.info("Starting categorization");

            // Check cache first
            Map<String, CookieCategorizationResponse> cachedResults = getCachedResults(cookieNames);
            List<String> uncachedCookies = cookieNames.stream()
                    .filter(name -> !cachedResults.containsKey(name))
                    .collect(Collectors.toList());

            Map<String, CookieCategorizationResponse> apiResults = Collections.emptyMap();

            if (!uncachedCookies.isEmpty()) {
                log.debug("Fetching categorization uncached cookies");

                if (useExternalApi) {
                    apiResults = callCategorizationApiWithRetry(uncachedCookies);
                } else {
                    apiResults = getCategoriesFromCsv(uncachedCookies);
                }

                if (!apiResults.isEmpty()) {
                    log.info("Validating predicted categories against Category table");

                    Map<String, CookieCategorizationResponse> validatedResults = new ConcurrentHashMap<>();

                    for (Map.Entry<String, CookieCategorizationResponse> entry : apiResults.entrySet()) {
                        String cookieName = entry.getKey();
                        CookieCategorizationResponse response = entry.getValue();

                        // Validate and map the predicted category
                        String predictedCategory = response.getCategory();
                        String validatedCategory = validateAndMapCategory(predictedCategory, tenantId);

                        // Update response with validated category
                        response.setCategory(validatedCategory);

                        if (!predictedCategory.equals(validatedCategory)) {
                            log.info("Predicted category is not equals to validated category");
                        }

                        validatedResults.put(cookieName, response);
                    }

                    apiResults = validatedResults;

                    // Cache the validated results
                    if (cacheEnabled) {
                        cacheResults(apiResults);
                    }
                }
            }

            // Combine cached and API results
            Map<String, CookieCategorizationResponse> allResults = new ConcurrentHashMap<>(cachedResults);
            allResults.putAll(apiResults);

            log.info("Successfully categorized cookies from API)");

            return allResults;

        } catch (Exception e) {
            log.error("Cookie categorization service failed");
            throw new CookieCategorizationException("Cookie categorization service is unavailable: " + e.getMessage());
        }
    }

    /**
     * Call categorization API with automatic retry
     */
    @Retryable(
            value = {ResourceAccessException.class, HttpServerErrorException.class, RestClientException.class},
            maxAttemptsExpression = "#{${cookie.categorization.retry.maxAttempts:3}}",
            backoff = @Backoff(
                    delayExpression = "#{${cookie.categorization.retry.delay:1000}}",
                    multiplierExpression = "#{${cookie.categorization.retry.multiplier:2.0}}",
                    maxDelayExpression = "#{${cookie.categorization.retry.maxDelay:10000}}"
            )
    )
    public Map<String, CookieCategorizationResponse> callCategorizationApiWithRetry(List<String> cookieNames) throws CookieCategorizationException {
        log.debug("Calling categorization API for cookies");

        try {
            // Prepare request
            CookieCategorizationRequest request = new CookieCategorizationRequest(cookieNames);
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "CookieScanner/1.0");
            headers.set("Accept", "application/json");

            HttpEntity<CookieCategorizationRequest> requestEntity = new HttpEntity<>(request, headers);

            // Make API call with injected RestTemplate (has timeout configured)
            ResponseEntity<String> response = restTemplate.exchange(
                    categorizationApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.info("API call completed successfully");

            return parseApiResponse(response);

        } catch (ResourceAccessException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                log.error("CONFIRMED: Read timeout occurred. External API is taking too long to respond.");
            }
            throw e;

        } catch (Exception e) {
           throw e;
        }
    }

    private Map<String, CookieCategorizationResponse> getCachedResults(List<String> cookieNames) {
        if (!cacheEnabled) {
            return Collections.emptyMap();
        }

        cleanExpiredEntries();

        Map<String, CookieCategorizationResponse> cachedResults = new ConcurrentHashMap<>();
        Instant now = Instant.now();

        for (String cookieName : cookieNames) {
            CacheEntry entry = categorizationCache.get(cookieName);
            if (entry != null && entry.isValid(now)) {
                cachedResults.put(cookieName, entry.response);
            }
        }

        return cachedResults;
    }

    private Map<String, CookieCategorizationResponse> parseApiResponse(ResponseEntity<String> response) throws CookieCategorizationException {
        try {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new CookieCategorizationException("External categorization API returned error status: " + response.getStatusCode());
            }

            String responseBody = response.getBody();

            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new CookieCategorizationException("External categorization API returned empty response");
            }

            List<CookieCategorizationResponse> responses = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {
                    }
            );

            log.debug("Successfully parsed categorization responses from API");

            Map<String, CookieCategorizationResponse> resultMap = responses.stream()
                    .filter(resp -> resp != null && resp.getName() != null)
                    .collect(Collectors.toMap(
                            CookieCategorizationResponse::getName,
                            resp -> resp,
                            (existing, replacement) -> existing
                    ));

            log.debug("Mapped valid responses to cookie names");
            return resultMap;

        } catch (Exception e) {
            log.error("Error parsing API response");
            throw new CookieCategorizationException("Failed to parse categorization API response: " + e.getMessage(), e);
        }
    }

    private void cacheResults(Map<String, CookieCategorizationResponse> results) {
        if (!cacheEnabled || results.isEmpty()) return;

        Instant expiryTime = Instant.now().plus(Duration.ofMinutes(cacheTtlMinutes));

        for (Map.Entry<String, CookieCategorizationResponse> entry : results.entrySet()) {
            CacheEntry cacheEntry = new CacheEntry(entry.getValue(), expiryTime);
            categorizationCache.put(entry.getKey(), cacheEntry);
        }

        log.debug("Cached categorization results");
    }

    private void cleanExpiredEntries() {
        if (!cacheEnabled) return;
        Instant now = Instant.now();
        categorizationCache.entrySet().removeIf(entry -> !entry.getValue().isValid(now));


    }

    private static class CacheEntry {
        final CookieCategorizationResponse response;
        final Instant expiryTime;

        CacheEntry(CookieCategorizationResponse response, Instant expiryTime) {
            this.response = response;
            this.expiryTime = expiryTime;
        }

        boolean isValid(Instant now) {
            return now.isBefore(expiryTime);
        }
    }

    /**
     * Validate predicted category against tenant's category table
     * If not found, map to closest or use default "Others"
     */
    private String validateAndMapCategory(String predictedCategory, String tenantId) {
        if (predictedCategory == null || predictedCategory.trim().isEmpty()) {
            log.warn("Empty category predicted, using default 'Others'");
            return "Others";
        }

        // Check if predicted category exists in database
        if (categoryService.categoryExists(predictedCategory, tenantId)) {
            return predictedCategory;
        }

        // Category doesn't exist - try to find closest match
        List<String> existingCategories = categoryService.getAllCategoryNames(tenantId);

        // Case-insensitive match
        Optional<String> match = existingCategories.stream()
                .filter(cat -> cat.equalsIgnoreCase(predictedCategory))
                .findFirst();

        if (match.isPresent()) {
            log.info("Mapped predicted to existing category");
            return match.get();
        }

        // No match found - use default
        log.warn("Predicted category not found in database for tenant Using 'Others'");
        return "Others";
    }

    /**
     * Get categories from CSV data
     */
    private Map<String, CookieCategorizationResponse> getCategoriesFromCsv(List<String> cookieNames) {
        Map<String, CookieCategorizationResponse> results = new ConcurrentHashMap<>();

        for (String cookieName : cookieNames) {
            String category = cookieCsvLoaderService.getCategoryForCookie(cookieName);

            if (category == null) {
                category = "Others";
            }
            CookieCategorizationResponse response = new CookieCategorizationResponse();
            response.setName(cookieName);
            response.setCategory(category);
            response.setDescription(null);
            response.setDescription_gpt(null);
            results.put(cookieName, response);
        }

        log.debug("CSV lookup: found out of cookies");
        return results;
    }

}