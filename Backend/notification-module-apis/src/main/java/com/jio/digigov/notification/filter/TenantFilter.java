package com.jio.digigov.notification.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.constant.NotificationConstants;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.enums.JdnmErrorCode;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.repository.NotificationConfigRepository;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.util.TransactionIdRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Bypass filter for system notification endpoints (no tenant/business required)
        if (isSystemNotificationEndpoint(request)) {
            log.debug("Bypassing TenantFilter for system notification endpoint: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

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

                boolean configExists = checkConfigExistsWithFallback(businessId, tenantId);

                log.debug("NotificationConfig exists check result: {} for businessId: {}, tenantId: {}",
                         configExists, businessId, tenantId);

                if (!configExists) {
                    log.warn("NotificationConfig not found after all 3 fallback steps for businessId: {}, tenantId: {}. Request rejected.",
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
     * Check if NotificationConfig exists with 3-step fallback mechanism.
     *
     * Step 1: Check by businessId
     * Step 2: Check by scopeLevel=TENANT
     * Step 3: Check by tenantId in businessId field
     *
     * @param businessId The business ID from request header
     * @param tenantId The tenant ID from request header
     * @return true if config exists in any of the 3 steps, false otherwise
     */
    private boolean checkConfigExistsWithFallback(String businessId, String tenantId) {
        try {
            // Step 1: Check by businessId
            Query step1Query = new Query(Criteria.where("businessId").is(businessId));
            boolean existsStep1 = mongoTemplate.exists(step1Query, "notification_configurations");

            if (existsStep1) {
                log.debug("NotificationConfig found (Step 1: businessId match): businessId={}", businessId);
                return true;
            }

            // Step 2: Check by scopeLevel=TENANT
            log.debug("NotificationConfig not found for businessId: {}, trying scopeLevel=TENANT fallback (Step 2)", businessId);
            Query step2Query = new Query(Criteria.where("scopeLevel").is(ScopeLevel.TENANT.name()));
            boolean existsStep2 = mongoTemplate.exists(step2Query, "notification_configurations");

            if (existsStep2) {
                log.info("NotificationConfig found using TENANT-level fallback (Step 2: scopeLevel=TENANT match)");
                return true;
            }

            // Step 3: Check by tenantId in businessId field
            if (tenantId != null && !tenantId.trim().isEmpty()) {
                log.debug("NotificationConfig not found for scopeLevel=TENANT, trying tenantId as businessId fallback (Step 3)");
                Query step3Query = new Query(Criteria.where("businessId").is(tenantId));
                boolean existsStep3 = mongoTemplate.exists(step3Query, "notification_configurations");

                if (existsStep3) {
                    log.info("NotificationConfig found using tenantId as businessId fallback (Step 3: tenantId match): tenantId={}", tenantId);
                    return true;
                }
            }

            log.debug("NotificationConfig not found after all 3 fallback steps for businessId: {}, tenantId: {}", businessId, tenantId);
            return false;

        } catch (Exception e) {
            log.error("Error checking NotificationConfig existence for businessId: {}, tenantId: {}", businessId, tenantId);
            return false;
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
     * Check if the request is for a system notification endpoint.
     * System notification endpoints bypass tenant/business validation.
     *
     * @param request The HTTP request
     * @return true if request is for system notification endpoint
     */
    private boolean isSystemNotificationEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }

        // Check for /v1/system/ endpoints (with or without context path)
        return uri.contains("/v1/system/") || uri.contains("/api/v1/system/");
    }

    /**
     * Send standardized error response when NGConfiguration is not found
     */
    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, String businessId) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String transactionId = request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID);

        StandardApiResponseDto<Object> errorResponse = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM2001,
                String.format("NGConfiguration not found for business ID: %s. Please verify the business ID is valid and properly configured.", businessId),
                null
        ).withTransactionId(transactionId);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}