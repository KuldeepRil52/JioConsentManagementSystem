package com.example.scanner.config;

import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.regex.Pattern;

@Component
@Order(1) // High priority - runs before path traversal filter
public class InvalidCharacterFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(InvalidCharacterFilter.class);

    // ENHANCED: Pattern for ALL invalid characters that should be blocked
    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile(
            ".*[\\$%\\^&\\*'\"<>\\\\\\[\\]\\{\\}\\|`~#@!+=:;,?\\s].*"
    );

    // Pattern specifically for transaction ID validation (should only contain alphanumeric, hyphens)
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile(
            "^[a-fA-F0-9-]{36}$" // UUID format: 8-4-4-4-12 characters
    );

    private final ObjectMapper objectMapper;

    public InvalidCharacterFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                chain.doFilter(request, response);
                return;
            }

            String requestURI = httpRequest.getRequestURI();

            // ENHANCED: Check for transaction ID endpoints specifically
            if (isStatusEndpoint(requestURI)) {
                String transactionId = extractTransactionId(requestURI);
                if (transactionId != null && !isValidTransactionId(transactionId)) {
                    log.warn("SECURITY: Invalid transaction ID format blocked");
                    handleInvalidTransactionId(httpRequest, httpResponse, requestURI, transactionId);
                    return;
                }
            }

            // Check if URI contains other invalid characters
            if (INVALID_CHAR_PATTERN.matcher(requestURI).find()) {
                log.warn("SECURITY: Request with invalid characters blocked");
                handleInvalidCharacters(httpRequest, httpResponse, requestURI);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Check if this is a status endpoint
     */
    private boolean isStatusEndpoint(String requestURI) {
        return requestURI != null &&
                (requestURI.startsWith("/api/v2/status/") ||
                        requestURI.startsWith("/status/") ||
                        requestURI.matches(".*/(status|transaction)/[^/]+.*"));
    }

    /**
     * Extract transaction ID from URI
     */
    private String extractTransactionId(String requestURI) {
        try {
            // Match patterns like /api/v2/status/{id} or /status/{id}
            String[] parts = requestURI.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("status".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
                if ("transaction".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting transaction ID");
        }
        return null;
    }

    /**
     * ENHANCED: Validate transaction ID format (UUID)
     */
    private boolean isValidTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }

        // Check for dangerous patterns first
        if (INVALID_CHAR_PATTERN.matcher(transactionId).find()) {
            return false;
        }

        // Check UUID format
        return TRANSACTION_ID_PATTERN.matcher(transactionId).matches();
    }

    private void handleInvalidTransactionId(HttpServletRequest request, HttpServletResponse response,
                                            String uri, String invalidId) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Invalid transaction ID format. Transaction ID must be a valid UUID.",
                String.format("SECURITY: Invalid transaction ID '%s' in URI: %s from IP: %s",
                        invalidId, uri, getClientIpAddress(request)),
                Instant.now(),
                uri
        );

        sendJsonErrorResponse(response, HttpStatus.BAD_REQUEST, errorResponse);
    }

    private void handleInvalidCharacters(HttpServletRequest request, HttpServletResponse response,
                                         String uri) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Invalid characters in URL path. Only alphanumeric characters, hyphens, and underscores are allowed.",
                String.format("SECURITY: Request URI '%s' contains invalid characters from IP: %s",
                        uri, getClientIpAddress(request)),
                Instant.now(),
                uri
        );

        sendJsonErrorResponse(response, HttpStatus.BAD_REQUEST, errorResponse);
    }

    /**
     * CRITICAL: Ensure JSON response with proper headers
     */
    private void sendJsonErrorResponse(HttpServletResponse response, HttpStatus status,
                                       ErrorResponse errorResponse) throws IOException {

        // FORCE JSON response
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Add security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "X-Client-IP",
                "CF-Connecting-IP",
                "True-Client-IP"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}