package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID filter for distributed tracing and request tracking.
 *
 * This filter ensures that every request has a unique correlation ID that can be
 * used to trace the request across multiple services and log entries. It supports
 * both incoming correlation IDs and generates new ones when needed.
 *
 * Features:
 * - Automatic correlation ID generation for new requests
 * - Propagation of existing correlation IDs across service boundaries
 * - MDC integration for automatic logging context
 * - Request/response header management
 * - Thread-safe correlation ID handling
 *
 * Headers Managed:
 * - X-Correlation-ID: Primary correlation identifier
 * - X-Request-ID: Alternative request identifier
 * - X-Trace-ID: Distributed tracing identifier
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract or generate correlation ID
            String correlationId = extractOrGenerateCorrelationId(httpRequest);
            String requestId = extractOrGenerateRequestId(httpRequest);
            String traceId = extractOrGenerateTraceId(httpRequest);

            // Set in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // Add to response headers for downstream services
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            // Log request start
            logRequestStart(httpRequest, correlationId, requestId);

            // Continue with the request
            chain.doFilter(request, response);

            // Log request completion
            logRequestEnd(httpRequest, httpResponse, correlationId);

        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * Extract correlation ID from request headers or generate a new one
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }

        return correlationId;
    }

    /**
     * Extract request ID from request headers or generate a new one
     */
    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = generateRequestId();
            log.debug("Generated new request ID: {}", requestId);
        } else {
            log.debug("Using existing request ID: {}", requestId);
        }

        return requestId;
    }

    /**
     * Extract trace ID from request headers or generate a new one
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = generateTraceId();
            log.debug("Generated new trace ID: {}", traceId);
        } else {
            log.debug("Using existing trace ID: {}", traceId);
        }

        return traceId;
    }

    /**
     * Generate a new correlation ID
     */
    private String generateCorrelationId() {
        return "CORR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Generate a new request ID
     */
    private String generateRequestId() {
        return "REQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Generate a new trace ID
     */
    private String generateTraceId() {
        return "TRACE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Log request start with correlation context
     */
    private void logRequestStart(HttpServletRequest request, String correlationId, String requestId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);

        String fullUrl = queryString != null ? uri + "?" + queryString : uri;

        log.info("Request started - {} {} from IP: {} | UA: {} | Correlation: {} | Request: {}",
                method, fullUrl, clientIp, userAgent, correlationId, requestId);
    }

    /**
     * Log request completion with correlation context
     */
    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response, String correlationId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        log.info("Request completed - {} {} | Status: {} | Correlation: {}",
                method, uri, status, correlationId);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Utility method to get current correlation ID from MDC
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }

    /**
     * Utility method to get current request ID from MDC
     */
    public static String getCurrentRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }

    /**
     * Utility method to get current trace ID from MDC
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }

    /**
     * Utility method to set correlation ID programmatically
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }

    /**
     * Utility method to clear all correlation context
     */
    public static void clearCorrelationContext() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(REQUEST_ID_MDC_KEY);
        MDC.remove(TRACE_ID_MDC_KEY);
    }
}