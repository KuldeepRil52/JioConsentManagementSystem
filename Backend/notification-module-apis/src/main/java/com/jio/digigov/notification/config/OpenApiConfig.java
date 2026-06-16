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
            .info(createApiInfo())
            .servers(createServerList())
            .addSecurityItem(new SecurityRequirement().addList("TenantAuth"))
            .addSecurityItem(new SecurityRequirement().addList("BusinessAuth"))
            .addSecurityItem(new SecurityRequirement().addList("TransactionAuth"))
            .components(createSecurityComponents())
            .tags(createApiTags());
    }

    private Info createApiInfo() {
        return new Info()
            .title("DPDP Notification Service API")
            .version("2.0.0")
            .description(buildApiDescription())
            .contact(createContactInfo())
            .license(createLicenseInfo());
    }

    private String buildApiDescription() {
        return """
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
            """;
    }

    private Contact createContactInfo() {
        return new Contact()
            .name("DPDP Notification Team")
            .email("dpdp-notification@jio.com")
            .url("https://internal.jio.com/dpdp/support");
    }

    private License createLicenseInfo() {
        return new License()
            .name("Jio Internal License")
            .url("https://internal.jio.com/licenses/platform");
    }

    private List<Server> createServerList() {
        return List.of(
            new Server().url("http://localhost:9003/notification")
                .description("Local Development Environment"),
            new Server().url("http://localhost:30005/notification")
                .description("Development Environment (DEV)"),
            new Server().url("http://localhost:9003/notification")
                .description("Local / ST Environment"),
            new Server().url("http://notification-module-apis:9005/notification")
                .description("Kubernetes (service name)")
        );
    }

    private Components createSecurityComponents() {
        return new Components()
            .addSecuritySchemes("TenantAuth", createTenantAuthScheme())
            .addSecuritySchemes("BusinessAuth", createBusinessAuthScheme())
            .addSecuritySchemes("TransactionAuth", createTransactionAuthScheme());
    }

    private SecurityScheme createTenantAuthScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-Tenant-Id")
            .description("Tenant identifier for multi-tenant isolation");
    }

    private SecurityScheme createBusinessAuthScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-Business-Id")
            .description("Business unit identifier for business-specific operations");
    }

    private SecurityScheme createTransactionAuthScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-Transaction-Id")
            .description("Transaction correlation ID for request tracing (optional)");
    }

    private List<Tag> createApiTags() {
        return List.of(
            createEventConfigurationTag(),
            createMasterListManagementTag(),
            createTemplateManagementTag(),
            createEventTriggersTag(),
            createMissedNotificationManagementTag(),
            createNotificationEventsTag()
        );
    }

    private Tag createEventConfigurationTag() {
        return new Tag().name("Event Configuration")
            .description("Event configuration management for notification settings. "
                + "Configure recipient notification preferences "
                + "and delivery channels for specific event types.");
    }

    private Tag createTemplateManagementTag() {
        return new Tag().name("Template Management")
            .description("Unified SMS and Email template management operations. "
                + "Create, retrieve, update and delete notification templates "
                + "with validation and approval workflows.");
    }

    private Tag createEventTriggersTag() {
        return new Tag().name("Event Triggers")
            .description("Event triggering and notification delivery APIs. "
                + "Initiate notification events with multi-channel delivery support "
                + "and comprehensive status tracking.");
    }

    private Tag createMissedNotificationManagementTag() {
        return new Tag().name("Missed Notification Management")
            .description("APIs for retrieving failed and scheduled callback notifications "
                + "for Data Processors and Data Fiduciaries.");
    }

    private Tag createMasterListManagementTag() {
        return new Tag().name("Master List Configuration")
            .description("APIs for managing tenant-specific master list configurations "
                + "with versioning and fallback support.");
    }

    private Tag createNotificationEventsTag() {
        return new Tag().name("Event Notifications")
            .description("Comprehensive event notification management endpoints. "
                + "Query event notification delivery status, history, and analytics "
                + "across SMS, Email, and Callback channels.");
    }
}