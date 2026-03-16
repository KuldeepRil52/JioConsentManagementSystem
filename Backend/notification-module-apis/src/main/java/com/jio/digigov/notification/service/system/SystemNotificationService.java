package com.jio.digigov.notification.service.system;

import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.dto.response.kafka.AsyncNotificationResponseDto;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.repository.event.EventConfigurationRepository;
import com.jio.digigov.notification.service.kafka.AsyncNotificationProducerService;
import com.jio.digigov.notification.service.ratelimit.RateLimitService;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for triggering system-wide notifications.
 *
 * This service wraps the existing AsyncNotificationProducerService and routes
 * system notifications through the same Kafka infrastructure as tenant notifications.
 * The only difference is that it uses "SYSTEM" as tenantId and businessId, which
 * routes the data to the shared database (tenant_db_shared) instead of tenant-specific
 * databases.
 *
 * Key Features:
 * - 100% reuses existing Kafka producer infrastructure
 * - 100% reuses existing Kafka consumers (SMS, Email, Callback)
 * - No authentication required (internal system use)
 * - Validates event configuration exists before triggering
 * - Uses shared database for all data storage
 *
 * @author Notification Service Team
 * @since 2025-01-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationService {

    private final AsyncNotificationProducerService asyncNotificationProducerService;
    private final EventConfigurationRepository eventConfigurationRepository;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final RateLimitService rateLimitService;

    @Value("${system.notification.tenant-id:SYSTEM}")
    private String systemTenantId;

    @Value("${system.notification.business-id:SYSTEM}")
    private String systemBusinessId;

    /**
     * Triggers a system-wide notification event.
     *
     * This method:
     * 1. Validates the event configuration exists in shared database
     * 2. Checks if the event configuration is active
     * 3. Calls the existing AsyncNotificationProducerService with SYSTEM identifiers
     * 4. Returns the same async response structure as tenant notifications
     *
     * The existing producer will:
     * - Save event record to shared database (via MongoTemplateProvider routing)
     * - Publish message to appropriate Kafka topics (SMS/EMAIL/CALLBACK)
     * - Consumers will process and deliver notifications
     * - All notification records stored in shared database
     *
     * @param request The event trigger request (same structure as tenant notifications)
     * @return CompletableFuture with async notification response
     */
    public CompletableFuture<AsyncNotificationResponseDto> triggerSystemEvent(
            TriggerEventRequestDto request) {

        log.info("Triggering system-wide notification event: {}", request.getEventType());

        try {
            // Get shared database template
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            // Validate event configuration exists in shared database
            Optional<EventConfiguration> eventConfigOpt =
                    eventConfigurationRepository.findByBusinessIdAndEventType(
                            systemBusinessId,
                            request.getEventType(),
                            sharedTemplate
                    );

            if (eventConfigOpt.isEmpty()) {
                log.error("System event configuration not found for eventType: {}", request.getEventType());
                return CompletableFuture.completedFuture(
                        AsyncNotificationResponseDto.failure(
                                null,
                                generateSystemTransactionId(),
                                "Event configuration not found: " + request.getEventType()
                        )
                );
            }

            EventConfiguration eventConfig = eventConfigOpt.get();

            // Check if event configuration is active
            if (!Boolean.TRUE.equals(eventConfig.getIsActive())) {
                log.warn("System event configuration is inactive: {}", request.getEventType());
                return CompletableFuture.completedFuture(
                        AsyncNotificationResponseDto.failure(
                                null,
                                generateSystemTransactionId(),
                                "Event configuration is inactive: " + request.getEventType()
                        )
                );
            }

            log.debug("Event configuration validated successfully: {}", eventConfig.getConfigId());

            // Check rate limit before processing the request
            // Extract recipient value from customer identifiers
            if (request.getCustomerIdentifiers() != null &&
                    request.getCustomerIdentifiers().getValue() != null) {

                String recipientValue = request.getCustomerIdentifiers().getValue();

                log.debug("Checking rate limit for recipient: {}, eventType: {}",
                        recipientValue, request.getEventType());

                // This will throw RateLimitExceededException if limit is exceeded
                // The exception is caught by GlobalExceptionHandler and returns 429
                rateLimitService.checkRateLimit(
                        systemTenantId,
                        systemBusinessId,
                        recipientValue,
                        request.getEventType()
                );

                log.debug("Rate limit check passed for recipient: {}", recipientValue);
            }

            // Call existing producer with SYSTEM identifiers
            // This routes all database operations to shared database (tenant_db_shared)
            // and publishes to the same Kafka topics used by tenant notifications
            String transactionId = generateSystemTransactionId();

            log.debug("Calling AsyncNotificationProducerService with tenantId={}, businessId={}",
                    systemTenantId, systemBusinessId);

            CompletableFuture<AsyncNotificationResponseDto> response =
                    asyncNotificationProducerService.processEventAsync(
                            request,
                            systemTenantId,    // "SYSTEM"
                            systemBusinessId,  // "SYSTEM"
                            transactionId,
                            null              // No HTTP context for system notifications
                    );

            // Evict rate limit cache after event is created to ensure next request gets accurate count
            if (request.getCustomerIdentifiers() != null &&
                    request.getCustomerIdentifiers().getValue() != null) {
                String recipientValue = request.getCustomerIdentifiers().getValue();
                response.thenAccept(result -> {
                    if (result != null && !"FAILED".equals(result.getStatus())) {
                        rateLimitService.evictRateLimitCache(
                                systemTenantId,
                                systemBusinessId,
                                recipientValue,
                                request.getEventType()
                        );
                        log.debug("Evicted rate limit cache for recipient: {} after successful event creation",
                                recipientValue);
                    }
                });
            }

            log.info("System event triggered successfully: eventType={}, transactionId={}",
                    request.getEventType(), transactionId);

            return response;

        } catch (Exception e) {
            log.error("Failed to trigger system event: eventType={}, error={}",
                    request.getEventType(), e.getMessage(), e);

            return CompletableFuture.completedFuture(
                    AsyncNotificationResponseDto.failure(
                            null,
                            generateSystemTransactionId(),
                            "Failed to trigger system event: " + e.getMessage()
                    )
            );
        }
    }

    /**
     * Generates a unique transaction ID for system notifications.
     * Uses SYS-TXN prefix to distinguish from tenant transactions.
     *
     * @return Transaction ID with format: SYS-TXN-{uuid}
     */
    private String generateSystemTransactionId() {
        return "SYS-TXN-" + UUID.randomUUID().toString();
    }

    /**
     * Checks if system notification infrastructure is healthy.
     *
     * @return true if configuration exists and producer is healthy
     */
    public boolean isHealthy() {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);
            boolean configExists = sharedTemplate.exists(
                    org.springframework.data.mongodb.core.query.Query.query(
                            org.springframework.data.mongodb.core.query.Criteria
                                    .where("businessId").is(systemBusinessId)
                    ),
                    com.jio.digigov.notification.entity.NotificationConfig.class
            );

            boolean producerHealthy = asyncNotificationProducerService.isHealthy();

            return configExists && producerHealthy;

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
