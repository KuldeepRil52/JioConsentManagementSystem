package com.example.scanner.util;

import java.util.Base64;
import java.util.regex.Pattern;

public class CommonUtil {

    private static final Pattern VALID_TRANSACTION_ID = Pattern.compile("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$");

    public static  boolean isValidTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }

        if (transactionId.contains("..") ||
                transactionId.contains("/") ||
                transactionId.contains("\\") ||
                transactionId.contains("%2e") ||
                transactionId.contains("%2f") ||
                transactionId.contains("%5c")) {
            return false;
        }

        // Validate UUID format - your app generates UUIDs like: 550e8400-e29b-41d4-a716-446655440000
        // This regex ensures ONLY valid UUIDs are accepted (36 characters, specific pattern)
        return VALID_TRANSACTION_ID.matcher(transactionId).matches();
    }

    /**
     * Validate tenant ID
     */
    public static void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
    }

    /**
     * Validate template ID
     */
    public static void validateTemplateId(String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Template ID is mandatory");
        }
    }

    public static boolean isValidBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        String trimmed = str.trim();

        if (trimmed.length() % 4 != 0) {
            return false;
        }

        if (!trimmed.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            return false;
        }

        try {
            Base64.getDecoder().decode(trimmed);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
