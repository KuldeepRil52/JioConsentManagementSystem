package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.dto.response.kafka.AsyncNotificationResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.entity.EmailTemplate;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.SMSTemplate;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.service.system.SystemConfigurationService;
import com.jio.digigov.notification.service.system.SystemNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for System-Wide Notifications.
 *
 * Provides endpoints for triggering and managing system-wide notifications
 * that are not specific to any tenant. These endpoints bypass TenantFilter
 * and do not require tenant/business headers.
 *
 * Key Features:
 * - No authentication required (internal system use only)
 * - No tenant/business headers required
 * - Uses shared database (tenant_db_shared)
 * - Reuses 100% of existing Kafka infrastructure
 *
 * Base Path: /v1/system
 *
 * @author Notification Service Team
 * @since 2025-01-21
 */
@RestController
@RequestMapping("/v1/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Notifications", description = "System-wide notification APIs (no authentication required)")
public class SystemNotificationController extends BaseController {

    private final SystemNotificationService systemNotificationService;
    private final SystemConfigurationService systemConfigurationService;

    /**
     * Trigger a system-wide notification event.
     *
     * This endpoint triggers a notification that is not specific to any tenant.
     * The notification is processed through the same Kafka infrastructure as
     * tenant notifications, but stored in the shared database.
     *
     * No headers required - this endpoint bypasses TenantFilter.
     *
     * Request Body Example:
     * <pre>
     * {
     *   "eventType": "SYSTEM_ALERT",
     *   "customerIdentifiers": {
     *     "type": "EMAIL",
     *     "value": "admin@example.com"
     *   },
     *   "eventPayload": {
     *     "title": "Critical Alert",
     *     "details": "System maintenance required",
     *     "timestamp": "2025-01-21T10:00:00"
     *   }
     * }
     * </pre>
     *
     * @param request The event trigger request
     * @return Async notification response with 202 ACCEPTED status
     */
    @PostMapping("/events/trigger")
    @Operation(
            summary = "Trigger system-wide notification event",
            description = "Triggers a system-wide notification without requiring tenant/business headers"
    )
    public ResponseEntity<StandardApiResponseDto<AsyncNotificationResponseDto>> triggerSystemEvent(
            HttpServletRequest httpRequest,
            @RequestBody @Valid TriggerEventRequestDto request) {

        log.info("Received system event trigger request: eventType={}", request.getEventType());

        String correlationId = extractCorrelationId(httpRequest);

        try {
            CompletableFuture<AsyncNotificationResponseDto> future =
                    systemNotificationService.triggerSystemEvent(request);

            // Wait for async processing (with timeout)
            AsyncNotificationResponseDto asyncResponse = future.get(5, TimeUnit.SECONDS);

            if ("ACCEPTED".equals(asyncResponse.getStatus())) {
                log.info("System event accepted: eventType={}, eventId={}",
                        request.getEventType(), asyncResponse.getEventId());

                StandardApiResponseDto<AsyncNotificationResponseDto> response = StandardApiResponseDto.success(
                        asyncResponse,
                        "Event accepted for asynchronous processing"
                ).withTransactionId(correlationId)
                        .withPath(httpRequest.getRequestURI());

                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            } else {
                log.warn("System event rejected: eventType={}, reason={}",
                        request.getEventType(), asyncResponse.getMessage());

                StandardApiResponseDto<AsyncNotificationResponseDto> response = StandardApiResponseDto.success(
                        asyncResponse,
                        asyncResponse.getMessage()
                ).withTransactionId(correlationId)
                        .withPath(httpRequest.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("Failed to trigger system event: eventType={}, error={}",
                    request.getEventType(), e.getMessage(), e);

            AsyncNotificationResponseDto failureResponse = AsyncNotificationResponseDto.failure(
                    null,
                    null,
                    "Internal error: " + e.getMessage()
            );

            StandardApiResponseDto<AsyncNotificationResponseDto> response = StandardApiResponseDto.success(
                    failureResponse,
                    "Event processing failed"
            ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get system notification configuration.
     *
     * Returns the notification provider configuration (DigiGov/SMTP) for
     * system-wide notifications.
     *
     * @return System notification configuration or 404 if not found
     */
    @GetMapping("/configuration")
    @Operation(
            summary = "Get system notification configuration",
            description = "Retrieves the notification provider configuration for system-wide notifications"
    )
    public ResponseEntity<NotificationConfig> getConfiguration() {
        log.debug("Fetching system notification configuration");

        return systemConfigurationService.getConfiguration()
                .map(config -> {
                    log.debug("System configuration found: configId={}", config.getConfigId());
                    return ResponseEntity.ok(config);
                })
                .orElseGet(() -> {
                    log.warn("System notification configuration not found");
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * List all system event configurations.
     *
     * Returns all active event configurations for system-wide notifications.
     *
     * @return List of event configurations
     */
    @GetMapping("/event-configurations")
    @Operation(
            summary = "List all system event configurations",
            description = "Retrieves all active event configurations for system-wide notifications"
    )
    public ResponseEntity<List<EventConfiguration>> listEventConfigurations() {
        log.debug("Fetching system event configurations");

        List<EventConfiguration> configs = systemConfigurationService.getAllEventConfigurations();

        log.debug("Found {} system event configurations", configs.size());
        return ResponseEntity.ok(configs);
    }

    /**
     * Get a specific event configuration by event type.
     *
     * @param eventType The event type
     * @return Event configuration or 404 if not found
     */
    @GetMapping("/event-configurations/{eventType}")
    @Operation(
            summary = "Get event configuration by type",
            description = "Retrieves a specific event configuration for the given event type"
    )
    public ResponseEntity<EventConfiguration> getEventConfiguration(
            @PathVariable String eventType) {

        log.debug("Fetching event configuration for eventType: {}", eventType);

        return systemConfigurationService.getEventConfiguration(eventType)
                .map(config -> {
                    log.debug("Event configuration found: eventType={}, configId={}",
                            eventType, config.getConfigId());
                    return ResponseEntity.ok(config);
                })
                .orElseGet(() -> {
                    log.warn("Event configuration not found for eventType: {}", eventType);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * List all system email templates.
     *
     * @return List of email templates
     */
    @GetMapping("/templates/email")
    @Operation(
            summary = "List system email templates",
            description = "Retrieves all email templates for system-wide notifications"
    )
    public ResponseEntity<List<EmailTemplate>> listEmailTemplates() {
        log.debug("Fetching system email templates");

        List<EmailTemplate> templates = systemConfigurationService.getAllEmailTemplates();

        log.debug("Found {} system email templates", templates.size());
        return ResponseEntity.ok(templates);
    }

    /**
     * List all system SMS templates.
     *
     * @return List of SMS templates
     */
    @GetMapping("/templates/sms")
    @Operation(
            summary = "List system SMS templates",
            description = "Retrieves all SMS templates for system-wide notifications"
    )
    public ResponseEntity<List<SMSTemplate>> listSmsTemplates() {
        log.debug("Fetching system SMS templates");

        List<SMSTemplate> templates = systemConfigurationService.getAllSmsTemplates();

        log.debug("Found {} system SMS templates", templates.size());
        return ResponseEntity.ok(templates);
    }

    /**
     * Get email template by event type.
     *
     * @param eventType The event type
     * @return Email template or 404 if not found
     */
    @GetMapping("/templates/email/{eventType}")
    @Operation(
            summary = "Get email template by event type",
            description = "Retrieves the email template for a specific event type"
    )
    public ResponseEntity<EmailTemplate> getEmailTemplateByEventType(
            @PathVariable String eventType) {

        log.debug("Fetching email template for eventType: {}", eventType);

        return systemConfigurationService.getEmailTemplateByEventType(eventType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Email template not found for eventType: {}", eventType);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get SMS template by event type.
     *
     * @param eventType The event type
     * @return SMS template or 404 if not found
     */
    @GetMapping("/templates/sms/{eventType}")
    @Operation(
            summary = "Get SMS template by event type",
            description = "Retrieves the SMS template for a specific event type"
    )
    public ResponseEntity<SMSTemplate> getSmsTemplateByEventType(
            @PathVariable String eventType) {

        log.debug("Fetching SMS template for eventType: {}", eventType);

        return systemConfigurationService.getSmsTemplateByEventType(eventType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("SMS template not found for eventType: {}", eventType);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Health check endpoint for system notifications.
     *
     * Checks:
     * - System configuration exists in shared database
     * - Kafka producer is healthy
     * - Template counts
     *
     * @return Health status with details
     */
    @GetMapping("/health")
    @Operation(
            summary = "System notification health check",
            description = "Checks health status of system notification infrastructure"
    )
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Performing system notification health check");

        try {
            boolean configExists = systemConfigurationService.configurationExists();
            boolean isHealthy = systemNotificationService.isHealthy();
            long[] templateCounts = systemConfigurationService.getTemplateCounts();

            Map<String, Object> healthStatus = Map.of(
                    "status", isHealthy ? "UP" : "DOWN",
                    "configurationExists", configExists,
                    "emailTemplateCount", templateCounts[0],
                    "smsTemplateCount", templateCounts[1],
                    "kafkaProducerHealthy", isHealthy,
                    "timestamp", LocalDateTime.now(),
                    "database", "tenant_db_shared"
            );

            HttpStatus status = isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

            log.debug("Health check result: status={}, configExists={}, templates={}/{}",
                    isHealthy ? "UP" : "DOWN", configExists, templateCounts[0], templateCounts[1]);

            return ResponseEntity.status(status).body(healthStatus);

        } catch (Exception e) {
            log.error("Health check failed");

            Map<String, Object> errorStatus = Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorStatus);
        }
    }
}
