package com.example.scanner.service;

import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.request.AddCookieRequest;
import com.example.scanner.dto.response.AddCookieResponse;
import com.example.scanner.dto.request.CookieUpdateRequest;
import com.example.scanner.dto.response.CookieUpdateResponse;
import com.example.scanner.entity.CookieEntity;
import com.example.scanner.entity.ScanResultEntity;
import com.example.scanner.enums.SameSite;
import com.example.scanner.enums.Source;
import com.example.scanner.exception.CookieNotFoundException;
import com.example.scanner.exception.ScanExecutionException;
import com.example.scanner.exception.TransactionNotFoundException;
import com.example.scanner.exception.UrlValidationException;
import com.example.scanner.util.UrlAndCookieUtil;
import com.example.scanner.util.SubdomainValidationUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.scanner.config.TenantContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CookieService {

    private static final Logger log = LoggerFactory.getLogger(CookieService.class);
    private static final String DEFAULT_SUBDOMAIN = "main";

    private final MultiTenantMongoConfig mongoConfig;
    private final CategoryService categoryService;

    /**
     * Update a specific cookie's category and description within a transaction
     */
    @Transactional
    public CookieUpdateResponse updateCookie(String tenantId, String transactionId, CookieUpdateRequest updateRequest)
            throws TransactionNotFoundException, ScanExecutionException, CookieNotFoundException, UrlValidationException {

        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }

        if (updateRequest == null || updateRequest.getName() == null || updateRequest.getName().trim().isEmpty()) {
            throw new UrlValidationException(ErrorCodes.EMPTY_ERROR,
                    "Cookie name is required for update",
                    "CookieUpdateRequest validation failed: name is null or empty"
            );
        }

        if (updateRequest.getExpires() == null || !updateRequest.getExpires().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Invalid 'expires' — must be a future time");
        }

        // Validate required fields (at least one field must be provided for update)
        if (updateRequest.getCategory() == null && updateRequest.getDescription() == null &&
                updateRequest.getDomain() == null && updateRequest.getPrivacyPolicyUrl() == null &&
                updateRequest.getExpires() == null) {
            throw new UrlValidationException(ErrorCodes.EMPTY_ERROR,
                    "At least one field (category, description, domain, privacyPolicyUrl, or expires) is required for update",
                    "CookieUpdateRequest validation failed: All update fields are null"
            );
        }

        if (updateRequest.getCategory() != null) {
            boolean isValidCategory = categoryService.categoryExists(updateRequest.getCategory(), tenantId);

            if (!isValidCategory) {
                throw new UrlValidationException(ErrorCodes.VALIDATION_ERROR,
                        "Category is not valid",
                        "Category '"+ updateRequest.getCategory()+ "' does not exist in the Category table for this tenant"
                );
            }
        }

        log.info("Updating cookie");

        try {
            // Find the scan result by transaction ID
            Optional<ScanResultEntity> scanResultOpt = findScanResultByTransactionId(tenantId, transactionId);

            if (scanResultOpt.isEmpty()) {
                throw new TransactionNotFoundException(transactionId);
            }

            ScanResultEntity scanResult = scanResultOpt.get();

            // Validate scan completion status
            validateScanStatus(scanResult, transactionId);

            // Find and update the specific cookie
            CookieEntity updatedCookie = findAndUpdateCookie(scanResult, updateRequest, transactionId);

            // Save the updated scan result
            saveScanResult(tenantId, scanResult);

            log.info("Successfully updated cookie");

            // Return updated response with all fields
            return CookieUpdateResponse.success(transactionId, updateRequest.getName(),
                    updatedCookie.getCategory(), updatedCookie.getDescription(), updatedCookie.getDomain(),
                    updatedCookie.getPrivacyPolicyUrl(), updatedCookie.getExpires(), updatedCookie.getProvider());

        } catch (TransactionNotFoundException | UrlValidationException | CookieNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new ScanExecutionException("Database error during cookie update: " + e.getMessage());
        } catch (Exception e) {
            throw new ScanExecutionException("Unexpected error during cookie update: " + e.getMessage());
        }
    }

    private Optional<ScanResultEntity> findScanResultByTransactionId(String tenantId, String transactionId) throws DataAccessException {
        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query(Criteria.where("transactionId").is(transactionId));
            ScanResultEntity result = tenantMongoTemplate.findOne(query, ScanResultEntity.class);
            return Optional.ofNullable(result);
        } finally {
            TenantContext.clear();
        }
    }

    private void validateScanStatus(ScanResultEntity scanResult, String transactionId) throws UrlValidationException {
        if (!"COMPLETED".equals(scanResult.getStatus())) {
            throw new UrlValidationException(ErrorCodes.INVALID_STATE_ERROR,
                    "Scan must be completed before updating cookies. Current status: " + scanResult.getStatus(),
                    "Scan status validation failed for transactionId: " + transactionId + ", current status: " + scanResult.getStatus()
            );
        }
    }

    private CookieEntity findAndUpdateCookie(ScanResultEntity scanResult, CookieUpdateRequest updateRequest, String transactionId) throws CookieNotFoundException, UrlValidationException {
        // Get all cookies from all subdomains for searching
        List<CookieEntity> allCookies = new ArrayList<>();
        if (scanResult.getCookiesBySubdomain() != null) {
            for (List<CookieEntity> subdomainCookies : scanResult.getCookiesBySubdomain().values()) {
                allCookies.addAll(subdomainCookies);
            }
        }

        if (allCookies.isEmpty()) {
            log.warn("No cookies available for update in");
            throw new UrlValidationException(ErrorCodes.NO_COOKIES_FOUND,
                    "No cookies available for this transaction",
                    "Cookie list validation failed: transaction " + transactionId + " has null or empty cookie list"
            );
        }

        // Search by "name" field
        Optional<CookieEntity> cookieToUpdate = allCookies.stream()
                .filter(cookie -> updateRequest.getName().equals(cookie.getName()))
                .findFirst();

        if (cookieToUpdate.isEmpty()) {
            log.warn("Cookie not found in transactionId");
            throw new CookieNotFoundException(updateRequest.getName(), transactionId);
        }

        // Update the cookie with all new fields
        CookieEntity cookie = cookieToUpdate.get();
        String oldCategory = cookie.getCategory();
        String oldDescription = cookie.getDescription();
        String oldDomain = cookie.getDomain();
        String oldPrivacyPolicyUrl = cookie.getPrivacyPolicyUrl();
        String oldProvider = cookie.getProvider();
        Instant oldExpires = cookie.getExpires();

        // Update only if new values are provided and different
        if (updateRequest.getCategory() != null && !updateRequest.getCategory().equals(oldCategory)) {
            cookie.setCategory(updateRequest.getCategory());
        }
        if (updateRequest.getDescription() != null && !updateRequest.getDescription().equals(oldDescription)) {
            cookie.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getDomain() != null && !updateRequest.getDomain().equals(oldDomain)) {
            validateCookieDomainAgainstScanUrl(scanResult.getUrl(), updateRequest.getDomain());

            // Extract subdomain from new domain and validate
            String rootDomain = UrlAndCookieUtil.extractRootDomain(scanResult.getUrl());
            String newSubdomainName = SubdomainValidationUtil.extractSubdomainName(updateRequest.getDomain(), rootDomain);
            validateSubdomainExistsInScan(scanResult, newSubdomainName, updateRequest.getDomain());

            cookie.setDomain(updateRequest.getDomain());
            cookie.setSubdomainName(newSubdomainName);
        }
        if (updateRequest.getPrivacyPolicyUrl() != null && !updateRequest.getPrivacyPolicyUrl().equals(oldPrivacyPolicyUrl)) {
            cookie.setPrivacyPolicyUrl(updateRequest.getPrivacyPolicyUrl());
        }
        if (updateRequest.getExpires() != null && !updateRequest.getExpires().equals(oldExpires)) {
            cookie.setExpires(updateRequest.getExpires());
        }
        if (updateRequest.getProvider() != null && !updateRequest.getProvider().equals(oldProvider)) {
            cookie.setProvider(updateRequest.getProvider());
        }

        return cookie;
    }

    private void saveScanResult(String tenantId, ScanResultEntity scanResult) throws DataAccessException {
        TenantContext.setCurrentTenant(tenantId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            tenantMongoTemplate.save(scanResult);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Add a new cookie to a transaction
     */
    @Transactional
    public AddCookieResponse addCookie(String tenantId, String transactionId, AddCookieRequest addRequest)
            throws TransactionNotFoundException, ScanExecutionException, UrlValidationException {

        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }

        if (addRequest == null) {
            throw new UrlValidationException(ErrorCodes.EMPTY_ERROR,
                    "Cookie information is required",
                    "AddCookieRequest is null"
            );
        }

        if (addRequest.getExpires() == null || !addRequest.getExpires().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Invalid 'expires' — must be a future time");
        }

        log.info("Adding cookie");

        try {
            // Find the scan result by transaction ID
            Optional<ScanResultEntity> scanResultOpt = findScanResultByTransactionId(tenantId, transactionId);

            if (scanResultOpt.isEmpty()) {
                throw new TransactionNotFoundException(transactionId);
            }

            ScanResultEntity scanResult = scanResultOpt.get();

            // Validate that we can add cookies (scan doesn't need to be completed for manual additions)
            if ("FAILED".equals(scanResult.getStatus())) {
                throw new UrlValidationException(ErrorCodes.INVALID_STATE_ERROR,
                        "Cannot add cookies to a failed scan",
                        "Scan status validation failed for transactionId: " + transactionId + ", status: FAILED"
                );
            }

            // Validate cookie domain against scan URL
            validateCookieDomainAgainstScanUrl(scanResult.getUrl(), addRequest.getDomain());

            // Extract subdomain from cookie domain
            String rootDomain = UrlAndCookieUtil.extractRootDomain(scanResult.getUrl());
            String subdomainName = SubdomainValidationUtil.extractSubdomainName(addRequest.getDomain(), rootDomain);

            // Validate subdomain exists in scan result
            validateSubdomainExistsInScan(scanResult, subdomainName, addRequest.getDomain());

            // Check for duplicate cookie in the extracted subdomain
            if (cookieExistsInSubdomain(scanResult, addRequest, subdomainName)) {
                throw new UrlValidationException(ErrorCodes.DUPLICATE_ERROR,
                        "Cookie already exists",
                        "Duplicate cookie: " + addRequest.getName() + " in subdomain: " + subdomainName
                );
            }

            if (addRequest.getCategory() != null) {
                boolean isValidCategory = categoryService.categoryExists(addRequest.getCategory(), tenantId);

                if (!isValidCategory) {
                    throw new UrlValidationException(ErrorCodes.VALIDATION_ERROR,
                            "Category is not valid",
                            "Category '"+ addRequest.getCategory()+ "' does not exist in the Category table for this tenant"
                    );
                }
            }

            // Create the new cookie entity
            CookieEntity newCookie = createCookieEntityFromRequest(addRequest, scanResult.getUrl(), subdomainName);

            // Add cookie to the extracted subdomain
            addCookieToSubdomain(scanResult, newCookie, subdomainName);

            // Save the updated scan result
            saveScanResult(tenantId, scanResult);

            log.info("Successfully added cookie to transactionId");

            return AddCookieResponse.success(transactionId, addRequest.getName(),
                    addRequest.getDomain(), subdomainName, addRequest.getProvider());

        } catch (TransactionNotFoundException | UrlValidationException e) {
            log.warn("Validation failed for adding cookie");
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error during cookie addition");
            throw new ScanExecutionException("Database error during cookie addition: " + e.getMessage());
        } catch (Exception e) {
            throw new ScanExecutionException("Unexpected error during cookie addition: " + e.getMessage());
        }
    }

    private void validateCookieDomainAgainstScanUrl(String scanUrl, String cookieDomain)
            throws UrlValidationException {
        try {
            String scanRootDomain = UrlAndCookieUtil.extractRootDomain(scanUrl);

            // Clean cookie domain (remove leading dot if present)
            String cleanCookieDomain = cookieDomain.startsWith(".") ?
                    cookieDomain.substring(1) : cookieDomain;

            String cookieRootDomain = UrlAndCookieUtil.extractRootDomain(cleanCookieDomain);

            // Cookie domain must belong to the same root domain as the scan URL
            if (!scanRootDomain.equalsIgnoreCase(cookieRootDomain)) {
                throw new UrlValidationException(ErrorCodes.INVALID_FORMAT_ERROR,
                        "Cookie domain must belong to the same root domain as the scanned URL",
                        String.format("Cookie domain '%s' does not belong to scan domain '%s'",
                                cookieDomain, scanRootDomain)
                );
            }

            log.debug("Cookie domain validation passed");

        } catch (Exception e) {
            throw new UrlValidationException(ErrorCodes.INVALID_FORMAT_ERROR,
                    "Invalid cookie domain format",
                    "Cookie domain validation failed: " + e.getMessage()
            );
        }
    }

    /**
     * Validates that the extracted subdomain exists in the scan result
     */
    private void validateSubdomainExistsInScan(ScanResultEntity scanResult, String subdomainName, String cookieDomain)
            throws UrlValidationException {

        if (scanResult.getCookiesBySubdomain() == null ||
                !scanResult.getCookiesBySubdomain().containsKey(subdomainName)) {

            throw new UrlValidationException(
                    ErrorCodes.INVALID_FORMAT_ERROR,
                    "Subdomain was not part of the original scan",
                    String.format("Cookie domain '%s' (subdomain: '%s') was not scanned. " +
                                    "Only cookies for scanned domains can be added/updated.",
                            cookieDomain, subdomainName)
            );
        }

        log.debug("Subdomain validation passed");
    }

    private boolean cookieExistsInSubdomain(ScanResultEntity scanResult, AddCookieRequest addRequest, String subdomainName) {
        if (scanResult.getCookiesBySubdomain() == null) {
            return false;
        }

        List<CookieEntity> subdomainCookies = scanResult.getCookiesBySubdomain().get(subdomainName);

        if (subdomainCookies == null) {
            return false;
        }

        return subdomainCookies.stream()
                .anyMatch(cookie ->
                        cookie.getName().equals(addRequest.getName()) &&
                                Objects.equals(cookie.getDomain(), addRequest.getDomain())
                );
    }

    private CookieEntity createCookieEntityFromRequest(AddCookieRequest request, String scanUrl, String subdomainName) {
        // Parse enums
        SameSite sameSite = null;
        if (request.getSameSite() != null) {
            try {
                sameSite = SameSite.valueOf(request.getSameSite().toUpperCase());
            } catch (IllegalArgumentException e) {
                sameSite = SameSite.LAX;
            }
        }

        Source source = null;
        if (request.getSource() != null) {
            try {
                source = Source.valueOf(request.getSource().toUpperCase());
            } catch (IllegalArgumentException e) {
                source = Source.FIRST_PARTY;
            }
        }

        return new CookieEntity(
                request.getName(),
                scanUrl,
                request.getDomain(),
                request.getPath() != null ? request.getPath() : "/",
                request.getExpires(),
                request.isSecure(),
                request.isHttpOnly(),
                sameSite,
                source,
                request.getCategory(),
                request.getDescription(),
                request.getDescription_gpt(),
                subdomainName,
                request.getPrivacyPolicyUrl(),
                request.getProvider()
        );
    }

    private void addCookieToSubdomain(ScanResultEntity scanResult, CookieEntity cookie, String subdomainName) {
        // Initialize the map if it doesn't exist
        if (scanResult.getCookiesBySubdomain() == null) {
            scanResult.setCookiesBySubdomain(new HashMap<>());
        }

        // Get or create the subdomain cookie list
        List<CookieEntity> subdomainCookies = scanResult.getCookiesBySubdomain()
                .computeIfAbsent(subdomainName, k -> new ArrayList<>());

        // Add the cookie
        subdomainCookies.add(cookie);

        log.debug("Added cookie to subdomain.");
    }
}