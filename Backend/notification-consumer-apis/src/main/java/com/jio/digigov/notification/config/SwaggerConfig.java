package com.jio.digigov.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
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
//@Configuration
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
     * Complete DPDP API group configuration.
     * Combines all DPDP-related endpoints for comprehensive documentation.
     */
    @Bean
    public GroupedOpenApi dpdpCompleteApi() {
        return GroupedOpenApi.builder()
                .group("dpdp-complete")
                .displayName("DPDP Notification APIs")
                .pathsToMatch("/v1/templates/**", "/v1/events/**", "/v1/event-configurations/**")
                .pathsToExclude("/health/**", "/actuator/**", "/legacy/**", "/otp/**",
                               "/notifications/**", "/configuration/**", "/data-processors/**")
                .addOperationCustomizer(globalHeadersCustomizer())
                .addOpenApiCustomizer(apiDocumentationCustomizer())
                .build();
    }

    /**
     * Operation customizer to add global headers to all operations.
     * Ensures consistent header documentation across all endpoints.
     */
    private OperationCustomizer globalHeadersCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            // Add global parameters that are common across all operations
            if (operation.getParameters() == null) {
                operation.setParameters(List.of());
            }

            // Check if X-Tenant-Id parameter is already present
            boolean hasTenantId = operation.getParameters().stream()
                    .anyMatch(param -> "X-Tenant-Id".equals(param.getName()));

            // Check if X-Business-Id parameter is already present
            boolean hasBusinessId = operation.getParameters().stream()
                    .anyMatch(param -> "X-Business-Id".equals(param.getName()));

            // Add global tenant parameter if not present
            if (!hasTenantId) {
                Parameter tenantParam = new Parameter()
                        .name("X-Tenant-Id")
                        .in("header")
                        .required(true)
                        .description("Tenant identifier for multi-tenant isolation")
                        .example("tenant_001")
                        .schema(new Schema<String>().type("string"));
                operation.addParametersItem(tenantParam);
            }

            // Add global business parameter if not present
            if (!hasBusinessId) {
                Parameter businessParam = new Parameter()
                        .name("X-Business-Id")
                        .in("header")
                        .required(true)
                        .description("Business unit identifier")
                        .example("business_001")
                        .schema(new Schema<String>().type("string"));
                operation.addParametersItem(businessParam);
            }

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
}