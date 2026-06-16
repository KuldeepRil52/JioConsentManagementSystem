package com.jio.digigov.notification.service.kafka;

import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.dto.response.kafka.AsyncNotificationResponseDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for asynchronous notification processing via Kafka.
 *
 * This service orchestrates the complete async notification workflow from event
 * validation to Kafka message publishing. It serves as the bridge between the
 * synchronous REST API and the asynchronous Kafka-based processing pipeline,
 * enabling high-throughput notification processing with improved scalability.
 *
 * Key Responsibilities:
 * - Event configuration validation and authorization
 * - Notification record creation and database persistence
 * - Kafka message construction and topic publishing
 * - Multi-channel notification coordination (SMS, Email, Callback)
 * - Error handling and initial retry logic
 * - Performance monitoring and metrics collection
 *
 * Processing Flow:
 * 1. Validate event configuration exists and is active
 * 2. Create notification records in tenant-specific databases
 * 3. Build channel-specific Kafka messages with payloads
 * 4. Publish messages to appropriate Kafka topics
 * 5. Return immediate response with tracking identifiers
 * 6. Consumer services handle actual delivery processing
 *
 * Async Benefits:
 * - 100x faster API response times (< 100ms vs 5-10 seconds)
 * - Horizontal scalability with multiple consumer instances
 * - Better error handling with retry mechanisms and DLQ
 * - Improved system resilience and fault tolerance
 * - Enhanced monitoring and observability
 *
 * Multi-Channel Support:
 * - SMS notifications via DigiGov API integration
 * - Email notifications with template rendering
 * - Webhook callbacks for Data Fiduciaries and Processors
 * - Future channel extensions (Push, In-App, etc.)
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface AsyncNotificationProducerService {

    /**
     * Processes an event asynchronously and publishes notifications to Kafka.
     *
     * This is the main entry point for async notification processing. It validates
     * the event configuration, creates notification records, and publishes messages
     * to appropriate Kafka topics for consumer processing. The method returns
     * immediately with tracking information, allowing clients to monitor progress.
     *
     * Processing Steps:
     * 1. Event Configuration Validation
     *    - Validates event type exists for the business
     *    - Checks notification channels are enabled
     *    - Verifies tenant and business authorization
     *
     * 2. Notification Record Creation
     *    - Creates records in tenant-specific MongoDB
     *    - Generates unique notification IDs
     *    - Initializes status tracking fields
     *
     * 3. Message Construction and Publishing
     *    - Builds channel-specific Kafka messages
     *    - Includes all necessary delivery information
     *    - Publishes to appropriate topics for processing
     *
     * 4. Response Generation
     *    - Returns tracking identifiers immediately
     *    - Provides estimated delivery times
     *    - Includes processing metadata
     *
     * Error Handling:
     * - Configuration errors: Immediate failure response
     * - Database errors: Rollback and failure response
     * - Kafka publishing errors: Retry with exponential backoff
     * - Partial failures: Continue processing, report individual failures
     *
     * @param request Event trigger request containing event details and arguments
     * @param tenantId the tenant identifier for multi-tenant isolation
     * @param businessId the business identifier for tenant-business isolation
     * @param transactionId unique transaction identifier for tracking and correlation
     * @param httpRequest HTTP servlet request for IP extraction and audit logging
     * @return CompletableFuture with AsyncNotificationResponseDto containing tracking info
     * @throws EventConfigurationNotFoundException if event configuration is not found
     * @throws InvalidTenantException if tenant validation fails
     * @throws KafkaPublishingException if message publishing fails
     */
    CompletableFuture<AsyncNotificationResponseDto> processEventAsync(TriggerEventRequestDto request,
                                                String tenantId,
                                                String businessId,
                                                String transactionId,
                                                HttpServletRequest httpRequest);

    // Note: Individual channel publishing methods (publishSmsNotification, publishEmailNotification,
    // publishCallbackNotification) have been removed as part of the async refactoring.
    // All notifications now go through the unified processEventAsync() method which sends
    // EventMessage to all consumers, allowing them to independently validate and process.

    /**
     * Checks if asynchronous processing mode is currently enabled.
     *
     * Allows runtime control of async processing through configuration.
     * When disabled, the system can fall back to synchronous processing
     * for maintenance, debugging, or emergency scenarios.
     *
     * @return true if async mode is enabled, false for synchronous fallback
     */
    boolean isAsyncModeEnabled();

    /**
     * Retrieves current processing metrics and statistics.
     *
     * Provides real-time information about async processing performance,
     * queue lengths, error rates, and throughput metrics for monitoring
     * and capacity planning.
     *
     * @return Map containing processing metrics and statistics
     */
    Map<String, Object> getProcessingMetrics();

    /**
     * Performs a health check of the async processing system.
     *
     * Validates that Kafka connectivity, database access, and external
     * service integrations are functioning correctly. Used by monitoring
     * systems and health check endpoints.
     *
     * @return true if all components are healthy, false if issues detected
     */
    boolean isHealthy();
}