package com.jio.digigov.auditmodule.controller;

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

        // Add other common headers
        headers.put("userAgent", request.getHeader("User-Agent"));
        headers.put("contentType", request.getHeader("Content-Type"));
        headers.put("accept", request.getHeader("Accept"));
        log.info("Extracted headers: {}", headers);
        return headers;
    }
}

