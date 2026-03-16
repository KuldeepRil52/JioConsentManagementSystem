package com.jio.digigov.notification.filter;

import com.jio.digigov.notification.constant.NotificationConstants;
import com.jio.digigov.notification.repository.NotificationConfigRepository;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.util.TransactionIdRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to extract tenant and business information from request headers
 * and set it in TenantContextHolder for database routing.
 * Validates that NotificationConfig exists for the provided business ID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final NotificationConfigRepository notificationConfigRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract or generate transaction ID
            String transactionId = extractOrGenerateTransactionId(request);

            // Wrap request to ensure transaction ID is always available
            HttpServletRequest wrappedRequest = new TransactionIdRequestWrapper(request, transactionId);

            String tenantId = extractTenantId(wrappedRequest);
            String businessId = extractBusinessId(wrappedRequest);

            // Set tenant context first (required for multi-tenant database routing)
            if (tenantId != null && !tenantId.trim().isEmpty()) {
                log.debug("Setting tenant context: {}", tenantId);
                TenantContextHolder.setTenantId(tenantId);
            } else {
                log.debug("No tenant ID found in request headers");
            }

            // Set business context first (required for database routing)
            if (businessId != null && !businessId.trim().isEmpty()) {
                log.debug("Setting business context: {}", businessId);
                TenantContextHolder.setBusinessId(businessId);
            } else {
                log.debug("No business ID found in request headers");
            }

            // Validate NotificationConfig after context is set
            // Skip validation for POST /v1/configurations and POST /v1/onboarding/setup
            // (both endpoints create the configuration)
            if (businessId != null && !businessId.trim().isEmpty() && !isConfigurationCreationRequest(wrappedRequest)) {
                log.debug("Validating NotificationConfig for businessId: {}, tenantId: {}", businessId, tenantId);

                boolean configExists = notificationConfigRepository.existsByBusinessIdCustom(businessId);

                log.debug("NotificationConfig exists check result: {} for businessId: {}, tenantId: {}",
                         configExists, businessId, tenantId);

                if (!configExists) {
                    log.warn("NotificationConfig not found for businessId: {}, tenantId: {}. Request rejected.",
                            businessId, tenantId);
                    sendErrorResponse(response, wrappedRequest, businessId);
                    return;
                }

                log.debug("NotificationConfig validated successfully for businessId: {}", businessId);
            }

            filterChain.doFilter(wrappedRequest, response);

        } finally {
            // Always clear context after request
            TenantContextHolder.clear();
        }
    }
    
    /**
     * Check if the request is a POST request to create new NGConfiguration or onboard a tenant.
     * This allows onboarding new businesses without the deadlock issue.
     */
    private boolean isConfigurationCreationRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String requestUri = request.getRequestURI();

        // Allow POST requests to /v1/configurations endpoint (exact match or with trailing slash)
        boolean isPostMethod = "POST".equalsIgnoreCase(method);
        boolean isConfigEndpoint = requestUri != null &&
                                   (requestUri.endsWith("/v1/configurations") ||
                                    requestUri.endsWith("/v1/configurations/"));

        // Allow POST requests to /v1/onboarding/setup endpoint
        // (which also creates notification configuration)
        boolean isOnboardingSetupEndpoint = requestUri != null &&
                                           (requestUri.endsWith("/v1/onboarding/setup") ||
                                            requestUri.endsWith("/v1/onboarding/setup/"));

        boolean isCreationRequest = isPostMethod && (isConfigEndpoint || isOnboardingSetupEndpoint);

        if (isCreationRequest) {
            log.debug("Skipping NGConfiguration validation for configuration creation request: {} {}",
                     method, requestUri);
        }

        return isCreationRequest;
    }

    /**
     * Extract or generate transaction ID from request headers.
     * If the transaction ID header is missing or empty, generates a new UUID.
     *
     * @param request the HTTP request
     * @return transaction ID (never null)
     */
    private String extractOrGenerateTransactionId(HttpServletRequest request) {
        String transactionId = request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID);

        if (transactionId == null || transactionId.trim().isEmpty()) {
            transactionId = UUID.randomUUID().toString();
            log.debug("Transaction ID header missing, generated new transaction ID: {}", transactionId);
        }

        return transactionId;
    }

    /**
     * Extract tenant ID from request headers
     * Supports both 'tenantId' and 'X-Tenant-Id' headers
     */
    private String extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader("tenantId");

        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = request.getHeader(NotificationConstants.HEADER_TENANT_ID);
        }

        return tenantId;
    }

    /**
     * Extract business ID from request headers
     * Supports both 'businessId' and 'X-Business-Id' headers
     */
    private String extractBusinessId(HttpServletRequest request) {
        String businessId = request.getHeader("businessId");

        if (businessId == null || businessId.trim().isEmpty()) {
            businessId = request.getHeader(NotificationConstants.HEADER_BUSINESS_ID);
        }

        return businessId;
    }

    /**
     * Send standardized error response when NGConfiguration is not found
     */
    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, String businessId) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String errorJson = String.format(
                "{\"success\":false,\"errorCode\":\"JDNM2001\",\"message\":\"NGConfiguration not found for business ID: %s. Please verify the business ID is valid and properly configured.\"}",
                businessId
        );

        response.getWriter().write(errorJson);
        response.getWriter().flush();
    }
}