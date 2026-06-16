package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Security validation utility for input sanitization and validation.
 *
 * This utility provides comprehensive security validation methods to prevent
 * common security vulnerabilities such as injection attacks, XSS, and data
 * tampering. All input validation follows OWASP security guidelines.
 *
 * Security Features:
 * - SQL injection prevention
 * - XSS attack prevention
 * - Path traversal protection
 * - Input sanitization and normalization
 * - Business ID and tenant ID validation
 * - Email and phone number validation
 * - Template content validation
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class SecurityValidationUtil {

    // Regex patterns for security validation
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern BUSINESS_ID_PATTERN = Pattern.compile("^[A-Z0-9_]{3,50}$");
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*$");

    // Dangerous keywords for injection detection
    private static final Set<String> SQL_INJECTION_KEYWORDS = new HashSet<>(Arrays.asList(
            "select", "insert", "update", "delete", "drop", "create", "alter", "truncate",
            "union", "exec", "execute", "script", "javascript", "vbscript", "onload",
            "onerror", "onclick", "onmouseover", "eval", "expression", "alert", "confirm"
    ));

    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
            ".*(<script[^>]*>.*?</script>|javascript:|vbscript:|onload=|onerror=|onclick=|" +
            "onmouseover=|eval\\(|expression\\().*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            ".*(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c).*", Pattern.CASE_INSENSITIVE);

    /**
     * Validates and sanitizes business ID
     */
    public String validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("Business ID cannot be null or empty");
        }

        String sanitized = sanitizeInput(businessId.trim().toUpperCase());

        if (!BUSINESS_ID_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid business ID format detected: {}", businessId);
            throw new IllegalArgumentException("Business ID must contain only alphanumeric characters, " +
                    "underscores, and hyphens (3-50 characters)");
        }

        return sanitized;
    }

    /**
     * Validates and sanitizes tenant ID
     */
    public String validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }

        String sanitized = sanitizeInput(tenantId.trim().toLowerCase());

        if (!TENANT_ID_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid tenant ID format detected: {}", tenantId);
            throw new IllegalArgumentException("Tenant ID must contain only alphanumeric characters, " +
                    "underscores, and hyphens (3-50 characters)");
        }

        return sanitized;
    }

    /**
     * Validates email address
     */
    public String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String sanitized = sanitizeInput(email.trim().toLowerCase());

        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid email format detected: {}", email);
            throw new IllegalArgumentException("Invalid email format");
        }

        if (sanitized.length() > 254) {
            throw new IllegalArgumentException("Email address too long (max 254 characters)");
        }

        return sanitized;
    }

    /**
     * Validates phone number
     */
    public String validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        // Remove spaces and special characters except + and digits
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");

        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            log.warn("Invalid phone number format detected: {}", phoneNumber);
            throw new IllegalArgumentException("Invalid phone number format");
        }

        return cleaned;
    }

    /**
     * Validates and sanitizes template content
     */
    public String validateTemplateContent(String content) {
        if (content == null) {
            return null;
        }

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(content).matches()) {
            log.warn("Potential XSS attack detected in template content");
            throw new IllegalArgumentException("Template content contains potentially malicious scripts");
        }

        // Sanitize HTML while preserving template variables
        return sanitizeHtmlContent(content);
    }

    /**
     * Validates URL for webhook/callback endpoints
     */
    public String validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String sanitized = sanitizeInput(url.trim());

        if (!URL_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid URL format detected: {}", url);
            throw new IllegalArgumentException("Invalid URL format - must be HTTPS");
        }

        // Ensure HTTPS for security
        if (!sanitized.startsWith("https://")) {
            throw new IllegalArgumentException("URLs must use HTTPS protocol");
        }

        return sanitized;
    }

    /**
     * Generic input sanitization
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        // Check for SQL injection patterns
        for (String keyword : SQL_INJECTION_KEYWORDS) {
            if (sanitized.toLowerCase().contains(keyword.toLowerCase())) {
                log.warn("Potential SQL injection keyword detected: {}", keyword);
                throw new IllegalArgumentException("Input contains potentially malicious content");
            }
        }

        // Check for path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(sanitized).matches()) {
            log.warn("Path traversal pattern detected in input");
            throw new IllegalArgumentException("Input contains path traversal patterns");
        }

        // Remove null bytes and control characters
        sanitized = sanitized.replaceAll("\\u0000", "")
                            .replaceAll("[\u0001-\u0008\u000B\u000C\u000E-\u001F\u007F]", "");

        return sanitized;
    }

    /**
     * Sanitizes HTML content while preserving template variables
     */
    private String sanitizeHtmlContent(String content) {
        if (content == null) {
            return null;
        }

        // Remove dangerous HTML tags and attributes
        String sanitized = content
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<iframe[^>]*>.*?</iframe>", "")
                .replaceAll("<object[^>]*>.*?</object>", "")
                .replaceAll("<embed[^>]*>.*?</embed>", "")
                .replaceAll("javascript:", "")
                .replaceAll("vbscript:", "")
                .replaceAll("on\\w+\\s*=", "");

        return sanitized;
    }

    /**
     * Validates transaction ID format
     */
    public String validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return null; // Transaction ID is optional
        }

        String sanitized = sanitizeInput(transactionId.trim());

        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Transaction ID too long (max 100 characters)");
        }

        if (!ALPHANUMERIC_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid transaction ID format detected: {}", transactionId);
            throw new IllegalArgumentException("Transaction ID must contain only alphanumeric " +
                    "characters, underscores, and hyphens");
        }

        return sanitized;
    }

    /**
     * Validates event type
     */
    public String validateEventType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }

        String sanitized = sanitizeInput(eventType.trim().toUpperCase());

        if (sanitized.length() > 50) {
            throw new IllegalArgumentException("Event type too long (max 50 characters)");
        }

        if (!ALPHANUMERIC_PATTERN.matcher(sanitized).matches()) {
            log.warn("Invalid event type format detected: {}", eventType);
            throw new IllegalArgumentException("Event type must contain only alphanumeric " +
                    "characters, underscores, and hyphens");
        }

        return sanitized;
    }

    /**
     * Validates JSON content for potential security issues
     */
    public void validateJsonContent(String jsonContent) {
        if (jsonContent == null) {
            return;
        }

        // Check for potential script injection in JSON
        if (XSS_PATTERN.matcher(jsonContent).matches()) {
            log.warn("Potential script injection detected in JSON content");
            throw new IllegalArgumentException("JSON content contains potentially malicious scripts");
        }

        // Check for extremely large JSON (DoS prevention)
        if (jsonContent.length() > 1_000_000) { // 1MB limit
            throw new IllegalArgumentException("JSON content too large (max 1MB)");
        }
    }
}