package com.jio.digigov.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for DPDP Notification Module
 *
 * Configured to expose only specific controllers:
 * - TemplateController (v1) - Template management operations
 * - EventConfigurationController - Event configuration management
 * - EventController - Event trigger and management APIs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DPDP Notification Service API")
                .version("2.0.0")
                .description("""
                    Data Protection and Digital Privacy notification service for the DigiGov platform.

                    This service provides comprehensive notification capabilities including template management,
                    event configuration, and multi-channel notification delivery for compliance with data
                    protection regulations.

                    ## Core Features
                    - Template Management: Create and manage SMS/Email notification templates
                    - Event Configuration: Configure notification settings for different recipient types
                    - Event Triggering: Initiate notifications with multi-channel delivery support
                    - Async Processing: High-performance asynchronous notification processing
                    - Multi-Recipient Support: Notifications for Data Principal, Data Fiduciary, and Data Processor
                    - Analytics and Tracking: Event monitoring and delivery status reporting

                    ## Required Headers
                    All API endpoints require the following headers:
                    - X-Tenant-Id: Tenant identifier (required)
                    - X-Business-Id: Business unit identifier (required)
                    - X-Transaction-Id: Transaction correlation ID (optional)
                    - X-Scope-Level: Scope level for template operations (default: BUSINESS)
                    - X-Type: Notification type (default: NOTIFICATION)

                    ## Architecture
                    - Multi-tenant isolation for data security
                    - Business-specific configuration management
                    - Role-based access controls

                    ## Supported Channels
                    - SMS: Mobile notifications with DLT compliance
                    - EMAIL: HTML and text email notifications
                    - CALLBACK: Webhook notifications for data processors

                    ## Rate Limits
                    - Template operations: 50 requests per minute per business
                    - Event triggering: 1000 requests per minute per business
                    - Event queries: 200 requests per minute per business
                    """)
                .contact(new Contact()
                    .name("DPDP Notification Team")
                    .email("dpdp-notification@jio.com")
                    .url("https://internal.jio.com/dpdp/support"))
                .license(new License()
                    .name("Jio Internal License")
                    .url("https://internal.jio.com/licenses/platform")))
            .servers(List.of(
                new Server().url("http://localhost:9003/notification").description("Local Development"),
                new Server().url("http://localhost:30005/notification").description("Development Environment"),
                new Server().url("http://notification-module-apis:9005/notification").description("Kubernetes (service name)")
            ))
            .addSecurityItem(new SecurityRequirement().addList("TenantAuth"))
            .addSecurityItem(new SecurityRequirement().addList("BusinessAuth"))
            .addSecurityItem(new SecurityRequirement().addList("TransactionAuth"))
            .components(new Components()
                .addSecuritySchemes("TenantAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Tenant-Id")
                    .description("Tenant identifier for multi-tenant isolation"))
                .addSecuritySchemes("BusinessAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Business-Id")
                    .description("Business unit identifier for business-specific operations"))
                .addSecuritySchemes("TransactionAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Transaction-Id")
                    .description("Transaction correlation ID for request tracing (optional)"))
            )
            .tags(List.of(
                new Tag().name("Event Configuration")
                    .description("Event configuration management for notification settings. "
                        + "Configure recipient notification preferences "
                        + "and delivery channels for specific event types."),
                new Tag().name("Master List Management")
                    .description("APIs for managing tenant-specific master list configurations "
                        + "with versioning and fallback support."),
                new Tag().name("Template Management")
                    .description("Unified SMS and Email template management operations. "
                        + "Create, retrieve, update and delete notification templates "
                        + "with validation and approval workflows."),
                new Tag().name("Event Triggers")
                    .description("Event triggering and notification delivery APIs. "
                        + "Initiate notification events with multi-channel delivery support "
                        + "and comprehensive status tracking."),
                new Tag().name("Notification Events")
                    .description("Comprehensive notification event management endpoints "
                        + "for tracking and monitoring notification delivery across all channels."),
                new Tag().name("Missed Notification Management")
                    .description("APIs for retrieving failed and scheduled callback notifications "
                        + "for Data Processors and Data Fiduciaries.")
            ));
    }
}