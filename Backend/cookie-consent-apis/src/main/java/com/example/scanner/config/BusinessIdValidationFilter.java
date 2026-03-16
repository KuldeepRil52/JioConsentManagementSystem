package com.example.scanner.config;

import com.example.scanner.constants.Constants;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
@Order(3) // After TenantDatabaseValidationFilter (Order 2)
@Slf4j
public class BusinessIdValidationFilter implements Filter {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;

    @Value("${multi-tenant.tenant-database-prefix}")
    private String tenantDatabasePrefix;

    // Skip validation for these paths
    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/health", "/metrics", "/error", "/swagger-ui", "/api-docs", "/dashboard"
    );

    // ONLY validate business-id for these specific endpoints
    private static final List<String> VALIDATE_BUSINESS_ID_ENDPOINTS = Arrays.asList(
            "/consent-handle/create",
            "/consent-handle/get",
            "/consent/get",
            "/cookie-templates"
    );

    public BusinessIdValidationFilter(MultiTenantMongoConfig mongoConfig, ObjectMapper objectMapper) {
        this.mongoConfig = mongoConfig;
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
            String method = httpRequest.getMethod();

            // Skip validation for health/metrics endpoints
            if (shouldSkipValidation(requestURI)) {
                chain.doFilter(request, response);
                return;
            }

            // Check if this endpoint requires business-id validation
            if (shouldValidateBusinessId(requestURI, method)) {

                String tenantId = httpRequest.getHeader(Constants.X_TENANT_ID);
                String businessId = httpRequest.getHeader(Constants.BUSINESS_ID_HEADER);

                // Business ID is REQUIRED for these endpoints
                if (businessId == null || businessId.trim().isEmpty()) {
                    log.warn("BUSINESS VALIDATION ERROR: Business ID is required but missing");
                    handleMissingBusinessId(httpRequest, httpResponse);
                    return;
                }

                // Validate that tenantId is also present
                if (tenantId == null || tenantId.trim().isEmpty()) {
                    log.warn("BUSINESS VALIDATION ERROR: Business ID provided but Tenant ID");
                    handleMissingTenantId(httpRequest, httpResponse);
                    return;
                }

                // Validate business ID exists in tenant database
                if (!businessIdExistsInTenant(businessId.trim(), tenantId.trim())) {
                    handleInvalidBusinessId(httpRequest, httpResponse, businessId, tenantId);
                    return;
                }

                log.debug("Business ID validation passed");
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Check if the endpoint requires business-id validation
     */
    private boolean shouldValidateBusinessId(String requestURI, String method) {
        // Check consent-handle endpoints
        if (requestURI.contains("/consent-handle/create") ||
                requestURI.contains("/consent-handle/get") ||
                requestURI.contains("/consent-handle/handle-code")) {
            return true;
        }

        // Check consent history ONLY
        if (requestURI.contains("/consent/") && requestURI.endsWith("/history")) {
            return true;
        }

        if (requestURI.contains("/consent/validate-token")) {
            return true;
        }

        // Check cookie-templates (only POST and PUT)
        if (requestURI.contains("/cookie-templates")) {
            return method.equals("POST") || method.equals("PUT");
        }

        return false;
    }

    /**
     * Check if business ID exists in tenant's business_applications collection
     * Uses dynamic query without entity/repository
     */
    private boolean businessIdExistsInTenant(String businessId, String tenantId) {
        try {
            // Set tenant context
            TenantContext.setCurrentTenant(tenantId);

            // Get MongoTemplate for this tenant
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Create dynamic query to check if businessId exists
            Query query = new Query(Criteria.where("businessId").is(businessId));

            // Check existence in business_applications collection
            return tenantMongoTemplate.exists(query, "business_applications");
        } catch (Exception e) {
            log.error("Error checking business ID existence");
            return false;
        } finally {
            // Clear tenant context
            TenantContext.clear();
        }
    }

    private boolean shouldSkipValidation(String requestURI) {
        return SKIP_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    private void handleMissingBusinessId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Business ID is required",
                "business-id header is missing or empty. Please provide a valid business ID in the request header.",
                Instant.now(),
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private void handleMissingTenantId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Tenant ID is required when Business ID is provided",
                "X-Tenant-ID header is missing or empty but business-id header is present. Both are required together.",
                Instant.now(),
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private void handleInvalidBusinessId(HttpServletRequest request, HttpServletResponse response,
                                         String businessId, String tenantId) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Invalid Business ID",
                String.format("Business ID '%s' does not exist in tenant '%s'. Please verify the business-id header.",
                        businessId, tenantId),
                Instant.now(),
                request.getRequestURI()
        );

        // Return 400 Bad Request
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}