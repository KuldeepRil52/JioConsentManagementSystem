package com.jio.digigov.notification.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced Swagger configuration for selective API documentation.
 *
 * This configuration provides fine-grained control over which APIs are exposed
 * in the Swagger documentation and adds common parameters to all operations.
 *
 * Key Features:
 * - Selective API exposure for specific controllers only
 * - Grouped APIs for better organization
 * - Global header parameters for multi-tenant operations
 * - Custom operation and schema processing
 * - Enhanced documentation with examples and constraints
 */
@Configuration
public class SwaggerConfig {

    /**
     * Template Management API group configuration.
     * Exposes only template-related endpoints with comprehensive documentation.
     */
    @Bean
    public GroupedOpenApi templateManagementApi() {
        return GroupedOpenApi.builder()
                .group("template-management")
                .displayName("Template Management APIs")
                .pathsToMatch("/v1/templates/**")
                .addOperationCustomizer(globalHeadersCustomizer())
                .build();
    }

    /**
     * Event Management API group configuration.
     * Includes both event configuration and event trigger endpoints.
     */
    @Bean
    public GroupedOpenApi eventManagementApi() {
        return GroupedOpenApi.builder()
                .group("event-management")
                .displayName("Event Management APIs")
                .pathsToMatch("/v1/events/**", "/v1/event-configurations/**")
                .addOperationCustomizer(globalHeadersCustomizer())
                .build();
    }

    /**
     * Notification Management API group configuration.
     * Exposes notification query and reporting endpoints.
     */
    @Bean
    public GroupedOpenApi notificationManagementApi() {
        return GroupedOpenApi.builder()
                .group("notification-management")
                .displayName("Notification Management APIs")
                .pathsToMatch("/v1/notifications/**")
                .addOperationCustomizer(globalHeadersCustomizer())
                .build();
    }

    /**
     * Complete DPDP API group configuration.
     * Combines all DPDP-related endpoints for comprehensive documentation.
     */
    @Bean
    public GroupedOpenApi dpdpCompleteApi() {
        return GroupedOpenApi.builder()
                .group("dpdp-complete")
                .displayName("DPDP Notification APIs")
                .pathsToMatch("/v1/templates/**", "/v1/events/**", "/v1/event-configurations/**", "/v1/notifications/**")
                .pathsToExclude("/health/**", "/actuator/**", "/legacy/**", "/otp/**",
                               "/configuration/**", "/data-processors/**")
                .addOperationCustomizer(globalHeadersCustomizer())
                .addOpenApiCustomizer(apiDocumentationCustomizer())
                .build();
    }

    /**
     * Operation customizer for global headers.
     * Note: X-Tenant-Id, X-Business-Id, and X-Transaction-Id are handled globally
     * via SecuritySchemes in OpenApiConfig and will appear in the "Authorize" button.
     * This customizer is kept for any future operation-specific customizations.
     */
    private OperationCustomizer globalHeadersCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            // Global security headers (X-Tenant-Id, X-Business-Id, X-Transaction-Id)
            // are handled via SecuritySchemes in OpenApiConfig.java
            // Users can set them once in the "Authorize" button at the top of Swagger UI

            // Any additional operation-specific customizations can be added here

            return operation;
        };
    }

    /**
     * OpenAPI customizer for enhanced documentation.
     * Adds global documentation improvements and examples.
     */
    private OpenApiCustomizer apiDocumentationCustomizer() {
        return openApi -> {
            // Add global examples and descriptions
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    addPathDocumentation(path, pathItem);
                });
            }
        };
    }

    /**
     * Adds enhanced documentation to specific paths.
     */
    private void addPathDocumentation(String path, PathItem pathItem) {
        // Add path-specific documentation enhancements
        if (path.contains("/templates")) {
            enhanceTemplatePathDocumentation(pathItem);
        } else if (path.contains("/events")) {
            enhanceEventPathDocumentation(pathItem);
        } else if (path.contains("/event-configurations")) {
            enhanceEventConfigPathDocumentation(pathItem);
        } else if (path.contains("/notifications")) {
            enhanceNotificationPathDocumentation(pathItem);
        }
    }

    /**
     * Enhances template-related path documentation.
     */
    private void enhanceTemplatePathDocumentation(PathItem pathItem) {
        // Add template-specific documentation
        if (pathItem.getPost() != null && pathItem.getPost().getSummary() == null) {
            pathItem.getPost().summary("Template operation")
                    .description("Template management operation with validation and approval workflow");
        }
    }

    /**
     * Enhances event-related path documentation.
     */
    private void enhanceEventPathDocumentation(PathItem pathItem) {
        // Add event-specific documentation
        if (pathItem.getPost() != null && pathItem.getPost().getSummary() == null) {
            pathItem.getPost().summary("Event operation")
                    .description("Event processing operation with multi-channel notification delivery");
        }
    }

    /**
     * Enhances event configuration path documentation.
     */
    private void enhanceEventConfigPathDocumentation(PathItem pathItem) {
        // Add event configuration specific documentation
        if (pathItem.getPost() != null && pathItem.getPost().getSummary() == null) {
            pathItem.getPost().summary("Event configuration operation")
                    .description("Event configuration management for notification settings");
        }
    }

    /**
     * Enhances notification-related path documentation.
     */
    private void enhanceNotificationPathDocumentation(PathItem pathItem) {
        // Add notification-specific documentation
        if (pathItem.getGet() != null && pathItem.getGet().getSummary() == null) {
            pathItem.getGet().summary("Notification query operation")
                    .description("Query and retrieve notification delivery status and history");
        }
    }
}