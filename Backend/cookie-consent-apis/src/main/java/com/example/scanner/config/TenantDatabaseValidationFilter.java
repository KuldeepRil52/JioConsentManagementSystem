package com.example.scanner.config;

import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoIterable;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
@Order(2)
@Slf4j
public class TenantDatabaseValidationFilter implements Filter {

    private final MongoClient mongoClient;
    private final ObjectMapper objectMapper;

    @Value("${multi-tenant.tenant-database-prefix}")
    private String tenantDatabasePrefix;

    // Skip validation for these paths
    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/health", "/metrics", "/error", "/swagger-ui", "/api-docs", "/dashboard"
    );

    public TenantDatabaseValidationFilter(MongoClient mongoClient, ObjectMapper objectMapper) {
        this.mongoClient = mongoClient;
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

            // Skip validation for health/metrics endpoints
            if (shouldSkipValidation(requestURI)) {
                chain.doFilter(request, response);
                return;
            }

            String tenantId = httpRequest.getHeader("X-Tenant-ID");

            if (tenantId == null || tenantId.trim().isEmpty()) {
                log.warn("TENANT VALIDATION ERROR: Missing X-Tenant-ID header for URI");
                handleMissingTenantId(httpRequest, httpResponse);
                return;
            }

            // Validate tenant database exists
            String databaseName = tenantDatabasePrefix + tenantId.trim();

            if (!databaseExists(databaseName)) {
                log.warn("TENANT DATABASE ERROR");
                handleDatabaseNotExists(httpRequest, httpResponse, tenantId, databaseName);
                return;
            }

            log.debug("Tenant database validation passed");
        }

        chain.doFilter(request, response);
    }

    private void handleMissingTenantId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Tenant ID is required",
                "X-Tenant-ID header is missing or empty. Please provide a valid tenant ID in the request header.",
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

    private boolean shouldSkipValidation(String requestURI) {
        return SKIP_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    private boolean databaseExists(String databaseName) {
        try {
            MongoIterable<String> databaseNames = mongoClient.listDatabaseNames();
            for (String name : databaseNames) {
                if (name.equals(databaseName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking database existence for: {}", databaseName);
            return false;
        }
    }

    private void handleDatabaseNotExists(HttpServletRequest request, HttpServletResponse response,
                                         String tenantId, String databaseName) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                "Invalid tenant ID - database does not exist",
                String.format("Database '%s' does not exist for tenant '%s'. Please verify the tenant ID in X-Tenant-ID header.",
                        databaseName, tenantId),
                Instant.now(),
                request.getRequestURI()
        );

        // Force JSON response
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Add security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}