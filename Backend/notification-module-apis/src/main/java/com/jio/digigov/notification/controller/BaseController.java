package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base controller with common header extraction methods
 */
@Slf4j
public abstract class BaseController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    /**
     * Extract tenant ID from request headers
     * Only accepts X-Tenant-ID header (standardized)
     */
    protected String extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(HeaderConstants.X_TENANT_ID);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Tenant-ID header is required");
        }

        return tenantId;
    }
    
    /**
     * Extract business ID from request headers
     * Only accepts X-Business-ID header (standardized)
     */
    protected String extractBusinessId(HttpServletRequest request) {
        String businessId = request.getHeader(HeaderConstants.X_BUSINESS_ID);

        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Business-ID header is required");
        }

        return businessId;
    }
    
    /**
     * Extract scope level from request headers
     */
    protected ScopeLevel extractScopeLevel(HttpServletRequest request) {
        String scopeLevel = request.getHeader(HeaderConstants.SCOPE_LEVEL);
        if (scopeLevel == null || scopeLevel.trim().isEmpty()) {
            scopeLevel = request.getHeader(HeaderConstants.X_SCOPE_LEVEL);
        }
        
        if (scopeLevel == null || scopeLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("scopeLevel header is required");
        }
        
        try {
            return ScopeLevel.valueOf(scopeLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid scopeLevel: " + scopeLevel + 
                ". Must be TENANT or BUSINESS");
        }
    }
    
    /**
     * Extract notification type from request headers
     */
    protected NotificationType extractNotificationType(HttpServletRequest request) {
        String type = request.getHeader(HeaderConstants.TYPE);
        if (type == null || type.trim().isEmpty()) {
            type = request.getHeader(HeaderConstants.X_TYPE);
        }
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("type header is required");
        }
        
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid type: " + type + 
                ". Must be NOTIFICATION or OTPVALIDATOR");
        }
    }
    
    /**
     * Extract transaction ID from request headers
     * Uses X-Transaction-ID header (standardized)
     * Generates UUID if not provided, logs when missing
     */
    protected String extractTransactionId(HttpServletRequest request) {
        String txn = request.getHeader(HeaderConstants.X_TRANSACTION_ID);

        if (txn == null || txn.trim().isEmpty()) {
            txn = UUID.randomUUID().toString();
            log.warn("X-Transaction-ID header missing, generated new transaction ID: {}", txn);
        }

        return txn;
    }

    /**
     * Extract transaction ID for DigiGov APIs
     * Generates 26-character alphanumeric ID instead of using header
     */
    protected String extractDigiGovTransactionId(HttpServletRequest request) {
        String txn = generateAlphanumericTransactionId(26);
        log.info("Generated DigiGov transaction ID: {}", txn);
        return txn;
    }

    /**
     * Extract correlation ID from request headers
     * Uses X-Correlation-ID header, falls back to X-Transaction-ID, generates UUID if not provided
     */
    protected String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");

        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = request.getHeader(HeaderConstants.X_TRANSACTION_ID);
        }

        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Correlation ID header missing, generated new correlation ID: {}", correlationId);
        }

        return correlationId;
    }

    /**
     * Generate alphanumeric transaction ID of specified length
     */
    private String generateAlphanumericTransactionId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
    }
    
    /**
     * Extract all headers as a Map for audit purposes
     */
    protected Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        try {
            headers.put(HeaderConstants.X_TENANT_ID, extractTenantId(request));
        } catch (Exception e) {
            headers.put(HeaderConstants.X_TENANT_ID, null);
        }

        try {
            headers.put(HeaderConstants.X_BUSINESS_ID, extractBusinessId(request));
        } catch (Exception e) {
            headers.put(HeaderConstants.X_BUSINESS_ID, null);
        }
        
        try {
            headers.put(HeaderConstants.SCOPE_LEVEL, extractScopeLevel(request).name());
        } catch (Exception e) {
            headers.put(HeaderConstants.SCOPE_LEVEL, null);
        }
        
        try {
            headers.put(HeaderConstants.TYPE, extractNotificationType(request).name());
        } catch (Exception e) {
            headers.put(HeaderConstants.TYPE, null);
        }
        
        headers.put(HeaderConstants.X_TRANSACTION_ID, extractTransactionId(request));
        
        // Add other common headers
        headers.put("userAgent", request.getHeader(HeaderConstants.USER_AGENT));
        headers.put("contentType", request.getHeader(HeaderConstants.CONTENT_TYPE));
        headers.put("accept", request.getHeader(HeaderConstants.ACCEPT));
        log.info("Extracted headers: {}", headers);
        return headers;
    }
}