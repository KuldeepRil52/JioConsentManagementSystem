package com.jio.digigov.grievance.controller;

import com.jio.digigov.grievance.enumeration.ScopeLevel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Base controller with common header extraction methods
 */
@Slf4j
public abstract class BaseController {

    /**
     * Extract tenant ID from request headers
     */
    protected String extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader("tenantId");
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = request.getHeader("X-Tenant-Id");
        }

        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("tenantId header is required");
        }

        return tenantId;
    }

    /**
     * Extract business ID from request headers
     */
    protected String extractBusinessId(HttpServletRequest request) {
        String businessId = request.getHeader("businessId");
        if (businessId == null || businessId.trim().isEmpty()) {
            businessId = request.getHeader("X-Business-Id");
        }

        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("businessId header is required");
        }

        return businessId;
    }

    /**
     * Extract scope level from request headers
     */
    protected ScopeLevel extractScopeLevel(HttpServletRequest request) {
        String scopeLevel = request.getHeader("scopeLevel");
        if (scopeLevel == null || scopeLevel.trim().isEmpty()) {
            scopeLevel = request.getHeader("X-Scope-Level");
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

//    /**
//     * Extract notification type from request headers
//     */
//    protected NotificationType extractNotificationType(HttpServletRequest request) {
//        String type = request.getHeader("type");
//        if (type == null || type.trim().isEmpty()) {
//            type = request.getHeader("X-Type");
//        }
//
//        if (type == null || type.trim().isEmpty()) {
//            throw new IllegalArgumentException("type header is required");
//        }
//
//        try {
//            return NotificationType.valueOf(type.toUpperCase());
//        } catch (IllegalArgumentException e) {
//            throw new IllegalArgumentException("Invalid type: " + type +
//                    ". Must be NOTIFICATION or OTPVALIDATOR");
//        }
//    }

    /**
     * Extract transaction ID from request headers
     */
    protected String extractTransactionId(HttpServletRequest request) {
        String txn = request.getHeader("txn");
        if (txn == null || txn.trim().isEmpty()) {
            txn = request.getHeader("X-Txn");
        }

        if (txn == null || txn.trim().isEmpty()) {
            // Generate default transaction ID if not provided
            txn = "TXN_" + System.currentTimeMillis();
        }

        return txn;
    }

    /**
     * Extract all headers as a Map for audit purposes
     */
    protected Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();

        try {
            headers.put("tenantId", extractTenantId(request));
        } catch (Exception e) {
            headers.put("tenantId", null);
        }

        try {
            headers.put("businessId", extractBusinessId(request));
        } catch (Exception e) {
            headers.put("businessId", null);
        }

        try {
            headers.put("scopeLevel", extractScopeLevel(request).name());
        } catch (Exception e) {
            headers.put("scopeLevel", null);
        }

//        try {
//            headers.put("type", extractNotificationType(request).name());
//        } catch (Exception e) {
//            headers.put("type", null);
//        }

//        headers.put("txn", extractTransactionId(request));

        // Add other common headers
        headers.put("userAgent", request.getHeader("User-Agent"));
        headers.put("contentType", request.getHeader("Content-Type"));
        headers.put("accept", request.getHeader("Accept"));
        log.info("Extracted headers: {}", headers);
        return headers;
    }
}
