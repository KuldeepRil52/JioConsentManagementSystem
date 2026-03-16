package com.jio.digigov.notification.service.kafka.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.response.CallbackAcknowledgementResponseDto;
import com.jio.digigov.notification.entity.notification.StatusHistoryEntry;
import com.jio.digigov.notification.dto.kafka.EventMessage;
import com.jio.digigov.notification.dto.kafka.CallbackPayload;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.entity.event.DataProcessor;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.enums.StatusHistorySource;
import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.enums.CustomerIdentifierType;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.exception.CallbackUrlNotFoundException;
import com.jio.digigov.notification.repository.event.EventConfigurationRepository;
import com.jio.digigov.notification.repository.event.DataProcessorRepository;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.service.audit.AuditEventService;
import com.jio.digigov.notification.service.callback.CallbackUrlResolutionService;
import com.jio.digigov.notification.service.kafka.RetryPolicyService;
import com.jio.digigov.notification.service.registry.ConsentWithdrawService;
import com.jio.digigov.notification.service.template.TemplateService;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Kafka consumer for processing Callback notification messages.
 *
 * This service listens to the callback notification topic and processes messages by:
 * 1. Resolving callback payload templates with dynamic arguments
 * 2. Making HTTP API calls to client-specified callback URLs
 * 3. Handling API responses and acknowledgments
 * 4. Implementing retry logic with exponential backoff
 * 5. Supporting JSON response format
 *
 * Key Features:
 * - Simple HTTP API call delivery
 * - Configurable API timeout and retry policies
 * - Template-based payload generation
 * - Multi-tenant callback URL resolution
 *
 * Performance Features:
 * - Parallel processing with configurable concurrency
 * - Template caching for improved rendering performance
 * - Connection pooling for API delivery
 * - Manual acknowledgment for reliable message processing
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackNotificationConsumer {

    private final NotificationCallbackRepository callbackRepository;
    private final EventConfigurationRepository eventConfigurationRepository;
    private final DataProcessorRepository dataProcessorRepository;
    private final TemplateService templateService;
    private final RetryPolicyService retryPolicyService;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CallbackUrlResolutionService callbackUrlResolutionService;
    private final AuditEventService auditEventService;
    private final ConsentWithdrawService consentWithdrawService;

    /**
     * Event types that require consent withdraw API call before DF callback.
     * For these events, the withdraw API is called to save withdrawal_data to notification_events.
     */
    private static final Set<String> CONSENT_WITHDRAW_EVENT_TYPES = Set.of(
            "CONSENT_EXPIRED",
            "CONSENT_WITHDRAWN"
    );

    @Value("${notification.callback.send-on-missing-config:true}")
    private boolean sendOnMissingConfig;

    @Value("${api.callback.timeout:30000}")
    private int apiTimeout;

    @Value("${api.callback.max-retries:5}")
    private int maxApiRetries;

    /**
     * SecureRandom instance for cryptographically secure notification ID generation
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Main entry point for callback event processing.
     * Processes event messages with validation, DP ID validation, and callback creation.
     */
    @KafkaListener(
        topics = "${kafka.topics.callback}",
        groupId = "${kafka.consumer.group-id}",
        containerFactory = "callbackKafkaListenerContainerFactory"
    )
    public void processCallbackNotification(
            @Payload EventMessage eventMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header("kafka_receivedMessageKey") String messageKey,
            Acknowledgment acknowledgment) {

        log.info("Processing callback event: eventId={}, messageId={}, topic={}, partition={}, offset={}, messageKey={}",
                eventMessage.getEventId(), eventMessage.getMessageId(), topic, partition, offset, messageKey);

        try {
            // Get tenant-specific MongoTemplate
            var mongoTemplate = mongoTemplateProvider.getTemplate(eventMessage.getTenantId());

            // Validate event configuration
            boolean shouldSend = validateEventConfigurationAndCallbackEnabled(eventMessage, mongoTemplate);
            if (!shouldSend) {
                log.info("Callback not enabled for event or missing config - eventId: {}", eventMessage.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Process callback notifications based on message key
            processCallbackNotifications(eventMessage, messageKey, mongoTemplate);

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed callback event: eventId={}", eventMessage.getEventId());

        } catch (Exception e) {
            log.error("Error processing callback event: eventId={}, error={}",
                     eventMessage.getEventId(), e.getMessage(), e);

            handleProcessingError(null, eventMessage.getTenantId(), e, acknowledgment);
        }
    }

    /**
     * Core callback processing logic with API delivery.
     */
    private void processCallback(NotificationCallback notification, CallbackPayload callbackPayload, EventMessage message,
                                MongoTemplate mongoTemplate) {

        try {
            // Resolve callback URL based on recipient type
            String resolvedCallbackUrl;
            try {
                resolvedCallbackUrl = callbackUrlResolutionService.resolveCallbackUrl(
                    notification.getRecipientType(),
                    notification.getRecipientId(),
                    notification.getBusinessId(),
                    message.getTenantId()
                );

                // Override the callback URL from payload with resolved URL
                callbackPayload = CallbackPayload.builder()
                    .callbackUrl(resolvedCallbackUrl)
                    .payloadTemplateId(callbackPayload.getPayloadTemplateId())
                    .arguments(callbackPayload.getArguments())
                    .httpMethod(callbackPayload.getHttpMethod())
                    .recipientType(callbackPayload.getRecipientType())
                    .recipientId(callbackPayload.getRecipientId())
                    .eventData(callbackPayload.getEventData())
                    .webhookHeaders(callbackPayload.getWebhookHeaders())
                    .timeoutSeconds(callbackPayload.getTimeoutSeconds())
                    .eventType(callbackPayload.getEventType())
                    .correlationId(callbackPayload.getCorrelationId())
                    .expectedResponseFormat(callbackPayload.getExpectedResponseFormat())
                    .maxRetryAttempts(callbackPayload.getMaxRetryAttempts())
                    .deliveryConfig(callbackPayload.getDeliveryConfig())
                    .build();

                log.info("Resolved callback URL for recipientType={}, recipientId={}, businessId={}: {}",
                    notification.getRecipientType(), notification.getRecipientId(),
                    notification.getBusinessId(), resolvedCallbackUrl);

            } catch (CallbackUrlNotFoundException e) {
                log.error("Failed to resolve callback URL for notification: notificationId={}, recipientType={}, recipientId={}, businessId={}: {}",
                    notification.getNotificationId(), notification.getRecipientType(),
                    notification.getRecipientId(), notification.getBusinessId(), e.getMessage());

                // Audit failed delivery
                auditEventService.auditNotificationDelivery(
                        notification.getNotificationId(),
                        "DELIVERY_FAILED",
                        "CALLBACK_CONSUMER",
                        message.getTenantId(),
                        message.getBusinessId(),
                        message.getTransactionId(),
                        message.getSourceIp(),
                        message.getEventId()
                );

                handleDeliveryFailure(notification, "Callback URL resolution failed: " + e.getMessage(), mongoTemplate);
                return;
            } catch (Exception e) {
                log.error("Unexpected error resolving callback URL for notification: notificationId={}: {}",
                    notification.getNotificationId(), e.getMessage(), e);

                // Audit failed delivery
                auditEventService.auditNotificationDelivery(
                        notification.getNotificationId(),
                        "DELIVERY_FAILED",
                        "CALLBACK_CONSUMER",
                        message.getTenantId(),
                        message.getBusinessId(),
                        message.getTransactionId(),
                        message.getSourceIp(),
                        message.getEventId()
                );

                handleDeliveryFailure(notification, "Callback URL resolution error: " + e.getMessage(), mongoTemplate);
                return;
            }
            // Resolve callback payload template
            String resolvedPayload;
            if (callbackPayload.getPayloadTemplateId() != null) {
                try {
                    String templateContent = templateService.resolveTemplate(
                            callbackPayload.getPayloadTemplateId(),
                            callbackPayload.getArguments(),
                            message.getTenantId()
                    );

                    // Create callback payload with resolved template content
                    resolvedPayload = createCallbackPayloadWithTemplate(notification, callbackPayload, templateContent, message.getTenantId());
                } catch (Exception templateException) {
                    log.warn("Template resolution failed for templateId: {}, using default payload",
                            callbackPayload.getPayloadTemplateId(), templateException);
                    // Fallback to default callback payload
                    resolvedPayload = createDefaultCallbackPayload(notification, callbackPayload, message.getTenantId());
                }
            } else {
                log.debug("No template ID specified for callback, using default payload");
                // Create default payload when no template is specified
                resolvedPayload = createDefaultCallbackPayload(notification, callbackPayload, message.getTenantId());
            }

            // Create API request
            ApiRequest apiRequest = createApiRequest(
                    notification,
                    callbackPayload,
                    resolvedPayload
            );

            // Deliver API callback
            ApiResponse response = deliverApiCallback(apiRequest, notification.getTenantId());

            // Update notification with delivery status
            if (response.isSuccess()) {
                // Parse response body to validate acknowledgement structure
                CallbackAcknowledgementResponseDto ackResponse = parseAcknowledgementResponse(response.getResponseBody());

                if (ackResponse != null && "ACKNOWLEDGED".equalsIgnoreCase(ackResponse.getStatus())) {
                    // Valid acknowledgement response
                    StatusHistoryEntry historyEntry = StatusHistoryEntry.builder()
                            .timestamp(LocalDateTime.now())
                            .status("ACKNOWLEDGED")
                            .acknowledgementId(ackResponse.getAcknowledgementId())
                            .remark(ackResponse.getRemark())
                            .source(StatusHistorySource.CALLBACK_RESPONSE)
                            .updatedBy(notification.getRecipientId())
                            .build();

                    callbackRepository.updateStatusWithHistory(
                            notification.getNotificationId(),
                            NotificationStatus.ACKNOWLEDGED.name(),
                            historyEntry,
                            mongoTemplate
                    );

                    // Record success metrics
                    retryPolicyService.recordRetryMetrics(NotificationChannel.CALLBACK.name(), notification.getAttemptCount(), true, null);

                    // Audit successful delivery
                    auditEventService.auditNotificationDelivery(
                            notification.getNotificationId(),
                            "DELIVERY_SUCCESS",
                            "CALLBACK_CONSUMER",
                            message.getTenantId(),
                            message.getBusinessId(),
                            message.getTransactionId(),
                            message.getSourceIp(),
                            message.getEventId()
                    );

                    log.info("Callback acknowledged: notificationId={}, url={}, status={}, ackId={}, remark={}, attempts={}",
                            notification.getNotificationId(), callbackPayload.getWebhookUrl(), response.getHttpStatusCode(),
                            ackResponse.getAcknowledgementId(), ackResponse.getRemark(), notification.getAttemptCount());
                } else {
                    // Invalid acknowledgement structure - mark as FAILED
                    String errorMsg = "Invalid acknowledgement response format. Expected {status: 'ACKNOWLEDGED', acknowledgementId: '...', remark: '...'}";
                    log.warn("Invalid callback response for notificationId={}: {}",
                            notification.getNotificationId(), response.getResponseBody());

                    // Audit failed delivery
                    auditEventService.auditNotificationDelivery(
                            notification.getNotificationId(),
                            "DELIVERY_FAILED",
                            "CALLBACK_CONSUMER",
                            message.getTenantId(),
                            message.getBusinessId(),
                            message.getTransactionId(),
                            message.getSourceIp(),
                            message.getEventId()
                    );

                    handleDeliveryFailure(notification, errorMsg, mongoTemplate);
                }
            } else {
                // Audit failed delivery
                auditEventService.auditNotificationDelivery(
                        notification.getNotificationId(),
                        "DELIVERY_FAILED",
                        "CALLBACK_CONSUMER",
                        message.getTenantId(),
                        message.getBusinessId(),
                        message.getTransactionId(),
                        message.getSourceIp(),
                        message.getEventId()
                );

                handleDeliveryFailure(notification, response.getErrorMessage(), mongoTemplate);
            }

        } catch (Exception e) {
            log.error("Error processing callback for notificationId={}: {}",
                     notification.getNotificationId(), e.getMessage(), e);

            // Audit failed delivery
            auditEventService.auditNotificationDelivery(
                    notification.getNotificationId(),
                    "DELIVERY_FAILED",
                    "CALLBACK_CONSUMER",
                    message.getTenantId(),
                    message.getBusinessId(),
                    message.getTransactionId(),
                    message.getSourceIp(),
                    message.getEventId()
            );

            handleDeliveryFailure(notification, e.getMessage(), mongoTemplate);
        }
    }

    /**
     * Creates an API request with essential headers for callback delivery.
     */
    private ApiRequest createApiRequest(NotificationCallback notification,
                                       CallbackPayload callbackPayload,
                                       String resolvedPayload) {

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Event-Id", notification.getEventId());
        headers.set("X-Correlation-Id", notification.getCorrelationId());
        headers.set("X-Notification-Id", notification.getNotificationId());
        headers.set(HeaderConstants.X_TIMESTAMP, Instant.now().toString());

        // Add custom headers if specified
        if (callbackPayload.getHeaders() != null) {
            callbackPayload.getHeaders().forEach(headers::set);
        }

        return ApiRequest.builder()
                .url(callbackPayload.getWebhookUrl())
                .method(callbackPayload.getHttpMethod())
                .headers(headers)
                .payload(resolvedPayload)
                .timeout(apiTimeout)
                .build();
    }



    /**
     * Delivers API callback to the specified URL with proper error handling.
     */
    private ApiResponse deliverApiCallback(ApiRequest request, String tenantId) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(request.getPayload(), request.getHeaders());

            ResponseEntity<String> response = restTemplate.exchange(
                    request.getUrl(),
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    String.class
            );

            return ApiResponse.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .httpStatusCode(response.getStatusCode().value())
                    .responseBody(response.getBody())
                    .build();

        } catch (Exception e) {
            log.error("API callback delivery failed: url={}, error={}", request.getUrl(), e.getMessage());
            return ApiResponse.builder()
                    .success(false)
                    .errorMessage("API callback delivery failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Handles API callback delivery failures with retry logic.
     */
    private void handleDeliveryFailure(NotificationCallback notification, String errorMessage,
                                     MongoTemplate mongoTemplate) {
        try {
            int currentAttempt = notification.getAttemptCount();
            String channel = NotificationChannel.CALLBACK.name();

            // Check if error is retryable
            if (!retryPolicyService.isRetryableError(errorMessage, channel)) {
                log.warn("API callback delivery failed with non-retryable error: notificationId={}, error={}",
                        notification.getNotificationId(), errorMessage);

                // Mark as permanently failed for non-retryable errors
                callbackRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);
                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "NON_RETRYABLE");
                return;
            }

            // Check if we should retry based on attempt count
            if (retryPolicyService.shouldRetry(currentAttempt, channel)) {
                // Calculate next retry time with exponential backoff
                LocalDateTime nextRetryAt = retryPolicyService.calculateNextRetry(currentAttempt, channel);

                // Update retry information
                callbackRepository.updateRetryInfo(
                        notification.getNotificationId(),
                        currentAttempt + 1,
                        nextRetryAt,
                        errorMessage,
                        mongoTemplate
                );

                // Update status to RETRY_SCHEDULED
                callbackRepository.updateStatus(notification.getNotificationId(), NotificationStatus.RETRY_SCHEDULED.name(), mongoTemplate);

                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "RETRYABLE");

                log.warn("API callback delivery failed, scheduling retry: notificationId={}, attempt={}, nextRetry={}, error={}",
                        notification.getNotificationId(), currentAttempt + 1, nextRetryAt, errorMessage);
            } else {
                // Mark as permanently failed after max attempts
                callbackRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);

                retryPolicyService.recordRetryMetrics(channel, currentAttempt, false, "MAX_ATTEMPTS_EXCEEDED");

                log.error("API callback delivery permanently failed after {} attempts: notificationId={}, finalError={}",
                         retryPolicyService.getMaxRetryAttempts(channel), notification.getNotificationId(), errorMessage);
            }

        } catch (Exception e) {
            log.error("Error handling API callback delivery failure: notificationId={}, error={}",
                     notification.getNotificationId(), e.getMessage(), e);

            // Fallback to FAILED status on error handling failure
            try {
                callbackRepository.updateStatus(notification.getNotificationId(), NotificationStatus.FAILED.name(), mongoTemplate);
            } catch (Exception fallbackException) {
                log.error("Failed to update status even in fallback: notificationId={}",
                         notification.getNotificationId(), fallbackException);
            }
        }
    }

    /**
     * Handles processing errors with appropriate acknowledgment strategy.
     */
    private void handleProcessingError(String notificationId, String tenantId, Exception error,
                                     Acknowledgment acknowledgment) {
        try {
            if (notificationId != null) {
                var mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
                callbackRepository.updateStatus(notificationId, NotificationStatus.ERROR.name(), mongoTemplate);
            }

            // Acknowledge to prevent infinite reprocessing of corrupted messages
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error in error handling for notificationId={}: {}",
                     notificationId, e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * API request model.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ApiRequest {
        private String url;
        private String method;
        private HttpHeaders headers;
        private String payload;
        private int timeout;
    }

    /**
     * Creates a callback payload with resolved template content.
     *
     * @param notification The callback notification record
     * @param callbackPayload The callback payload from Kafka message
     * @param templateContent The resolved template content
     * @param tenantId The tenant ID
     * @return JSON string representing the callback payload with template content
     */
    private String createCallbackPayloadWithTemplate(NotificationCallback notification, CallbackPayload callbackPayload, String templateContent, String tenantId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", notification.getNotificationId());
            payload.put("eventId", notification.getEventId());
            payload.put("businessId", notification.getBusinessId());
            payload.put("tenantId", tenantId);
            payload.put("recipientType", notification.getRecipientType());
            payload.put("recipientId", notification.getRecipientId());
            payload.put("eventType", notification.getEventType());
            payload.put("correlationId", notification.getCorrelationId());
            payload.put("timestamp", Instant.now().toString());
            payload.put("message", templateContent); // Include the resolved template content

            // Add event data if available
            if (callbackPayload.getEventData() != null) {
                payload.put("eventData", callbackPayload.getEventData());
            }

            // Add template arguments if available
            if (callbackPayload.getArguments() != null) {
                payload.put("arguments", callbackPayload.getArguments());
            }

            return objectMapper.writeValueAsString(payload);

        } catch (Exception e) {
            log.error("Failed to create callback payload with template content: {}", e.getMessage());
            // Fallback to default payload
            return createDefaultCallbackPayload(notification, callbackPayload, tenantId);
        }
    }

    /**
     * Creates a default callback payload when template resolution fails or no template is specified.
     *
     * @param notification The callback notification record
     * @param callbackPayload The callback payload from Kafka message
     * @param tenantId The tenant ID
     * @return JSON string representing the default callback payload
     */
    private String createDefaultCallbackPayload(NotificationCallback notification, CallbackPayload callbackPayload, String tenantId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", notification.getNotificationId());
            payload.put("eventId", notification.getEventId());
            payload.put("businessId", notification.getBusinessId());
            payload.put("tenantId", tenantId);
            payload.put("recipientType", notification.getRecipientType());
            payload.put("recipientId", notification.getRecipientId());
            payload.put("eventType", notification.getEventType());
            payload.put("correlationId", notification.getCorrelationId());
            payload.put("timestamp", Instant.now().toString());

            // Add event data if available
            if (callbackPayload.getEventData() != null) {
                payload.put("eventData", callbackPayload.getEventData());
            }

            // Add template arguments if available
            if (callbackPayload.getArguments() != null) {
                payload.put("arguments", callbackPayload.getArguments());
            }

            return objectMapper.writeValueAsString(payload);

        } catch (Exception e) {
            log.error("Failed to create default callback payload: {}", e.getMessage());
            // Return minimal JSON payload as last resort
            return String.format(
                "{\"notificationId\":\"%s\",\"eventId\":\"%s\",\"businessId\":\"%s\",\"tenantId\":\"%s\",\"timestamp\":\"%s\"}",
                notification.getNotificationId(),
                notification.getEventId(),
                notification.getBusinessId(),
                tenantId,
                Instant.now().toString()
            );
        }
    }

    /**
     * API response model.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ApiResponse {
        private boolean success;
        private int httpStatusCode;
        private String responseBody;
        private String errorMessage;
    }

    /**
     * Checks if a notification has already been successfully processed.
     * Prevents duplicate processing of messages that may be redelivered by Kafka.
     *
     * @param notification The notification to check
     * @return true if the notification has been successfully processed, false otherwise
     */
    private boolean isSuccessfullyProcessed(NotificationCallback notification) {
        String status = notification.getStatus();
        return NotificationStatus.SENT.name().equals(status) ||
               NotificationStatus.DELIVERED.name().equals(status) ||
               NotificationStatus.ACKNOWLEDGED.name().equals(status) ||
               NotificationStatus.RETRIEVED.name().equals(status);
    }

    /**
     * Validates event configuration and checks if callback notifications are enabled.
     * If config is missing, uses application property fallback.
     */
    private boolean validateEventConfigurationAndCallbackEnabled(EventMessage eventMessage,
                                                                MongoTemplate mongoTemplate) {
        try {
            Optional<EventConfiguration> configOpt = eventConfigurationRepository.findByBusinessIdAndEventType(
                eventMessage.getBusinessId(), eventMessage.getTenantId(), eventMessage.getEventType(), mongoTemplate);

            if (configOpt.isEmpty()) {
                log.warn("Event configuration not found for business: {} and event type: {}, using fallback: {}",
                        eventMessage.getBusinessId(), eventMessage.getEventType(), sendOnMissingConfig);
                return sendOnMissingConfig;
            }

            EventConfiguration eventConfig = configOpt.get();
            boolean callbackEnabled = eventConfig.getNotifications() != null &&
                    ((eventConfig.getNotifications().getDataFiduciary() != null &&
                      Boolean.TRUE.equals(eventConfig.getNotifications().getDataFiduciary().getEnabled())) ||
                     (eventConfig.getNotifications().getDataProcessor() != null &&
                      Boolean.TRUE.equals(eventConfig.getNotifications().getDataProcessor().getEnabled())));

            log.debug("Callback notifications enabled: {} for eventType: {}, business: {}",
                     callbackEnabled, eventMessage.getEventType(), eventMessage.getBusinessId());
            return callbackEnabled;

        } catch (Exception e) {
            log.error("Error validating event configuration for Callback: {}", e.getMessage());
            return sendOnMissingConfig; // Fallback on error
        }
    }

    /**
     * Processes callback notifications for Data Fiduciary and Data Processors.
     */
    private void processCallbackNotifications(EventMessage eventMessage, String messageKey,
                                             MongoTemplate mongoTemplate) {

        // Determine which recipient to process based on message key
        if (messageKey.endsWith("_DF")) {
            // This message is specifically for Data Fiduciary
            log.info("Processing Data Fiduciary callback for messageKey: {}", messageKey);

            // Call consent withdraw API before DF callback for consent-related events
            callConsentWithdrawApiIfApplicable(eventMessage);

            createAndProcessCallback(eventMessage, RecipientType.DATA_FIDUCIARY.name(), eventMessage.getBusinessId(), mongoTemplate);

            // Handle own-use DP callback from DF consumer path to avoid race condition with System Registry
            handleOwnUseDpCallbackIfApplicable(eventMessage, mongoTemplate);
        }
        else if (messageKey.contains("_DP_")) {
            // This message is specifically for a Data Processor
            // Extract DP ID from the message key: EVT_20250929_82239_DP_d9d3e75c-4cf4-4419-831d-81a68bd2fbba
            String dpId = messageKey.substring(messageKey.lastIndexOf("_DP_") + 4);
            log.info("Processing Data Processor callback for messageKey: {}, dpId: {}", messageKey, dpId);

            // Skip own-use DP for consent withdrawal/expiration events - handled by DF consumer path
            if (isConsentWithdrawEvent(eventMessage.getEventType()) && isOwnUseDataProcessor(dpId, eventMessage.getBusinessId(), mongoTemplate)) {
                log.info("Skipping own-use DP callback for eventId={}, dpId={} - handled by DF consumer path",
                        eventMessage.getEventId(), dpId);
                return;
            }

            // Validate the specific DP ID
            List<String> dataProcessorIds = eventMessage.getDataProcessorIds();
            if (dataProcessorIds != null && dataProcessorIds.contains(dpId)) {
                List<String> validDataProcessorIds = validateDataProcessorIds(
                    List.of(dpId), eventMessage.getBusinessId(), mongoTemplate);

                if (validDataProcessorIds.contains(dpId)) {
                    createAndProcessCallback(eventMessage, RecipientType.DATA_PROCESSOR.name(), dpId, mongoTemplate);
                } else {
                    log.warn("Invalid Data Processor ID for eventId: {}, dpId: {}", eventMessage.getEventId(), dpId);
                }
            } else {
                log.warn("Data Processor ID not found in event data: eventId={}, dpId={}", eventMessage.getEventId(), dpId);
            }
        } else {
            log.warn("Unknown message key format for callback processing: messageKey={}, eventId={}",
                    messageKey, eventMessage.getEventId());
        }
    }

    /**
     * Creates or finds existing callback notification and processes it.
     */
    private void createAndProcessCallback(EventMessage eventMessage, String recipientType, String recipientId,
                                         MongoTemplate mongoTemplate) {
        try {
            // First check if notification already exists for this event + recipient combination
            NotificationCallback existingNotification = findExistingCallbackNotification(
                eventMessage.getEventId(), recipientType, recipientId, mongoTemplate);

            NotificationCallback callbackNotification;
            String notificationId;

            if (existingNotification != null) {
                log.debug("Found existing callback notification for retry: notificationId={}, eventId={}, recipientType={}, recipientId={}, attempt={}",
                        existingNotification.getNotificationId(), eventMessage.getEventId(), recipientType, recipientId, existingNotification.getAttemptCount());
                callbackNotification = existingNotification;
                notificationId = existingNotification.getNotificationId();
            } else {
                // Create new notification record only if none exists
                notificationId = generateNotificationId("CALLBACK_" + recipientType);
                callbackNotification = createCallbackNotificationRecord(
                    eventMessage, notificationId, recipientType, recipientId, mongoTemplate);

                if (callbackNotification == null) {
                    log.error("Failed to create callback notification record - eventId: {}, recipientType: {}, recipientId: {}",
                             eventMessage.getEventId(), recipientType, recipientId);
                    return;
                }
                log.info("Created new callback notification record: notificationId={}, eventId={}, recipientType={}, recipientId={}",
                        callbackNotification.getNotificationId(), eventMessage.getEventId(), recipientType, recipientId);
            }

            // Update status to PROCESSING
            callbackRepository.updateStatus(notificationId, NotificationStatus.PROCESSING.name(), mongoTemplate);

            // Process the callback
            processCallback(callbackNotification, eventMessage, mongoTemplate);

            log.info("Successfully processed callback: eventId={}, notificationId={}, recipientType={}, recipientId={}",
                    eventMessage.getEventId(), notificationId, recipientType, recipientId);

        } catch (Exception e) {
            log.error("Error processing callback for eventId: {}, recipientType: {}, recipientId: {}, error: {}",
                     eventMessage.getEventId(), recipientType, recipientId, e.getMessage(), e);
        }
    }

    /**
     * Finds existing callback notification for the same event and recipient combination.
     * This prevents creating duplicate notifications on retry.
     */
    private NotificationCallback findExistingCallbackNotification(String eventId, String recipientType, String recipientId,
                                                                MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("eventId").is(eventId)
                    .and("recipientType").is(recipientType)
                    .and("recipientId").is(recipientId));

            List<NotificationCallback> existingNotifications = mongoTemplate.find(query, NotificationCallback.class);

            if (!existingNotifications.isEmpty()) {
                // Return the most recent notification (in case there are duplicates from previous versions)
                return existingNotifications.stream()
                        .max((n1, n2) -> n1.getCreatedAt().compareTo(n2.getCreatedAt()))
                        .orElse(null);
            }

            return null;
        } catch (Exception e) {
            log.error("Error finding existing callback notification for eventId={}, recipientType={}, recipientId={}: {}",
                     eventId, recipientType, recipientId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates callback notification record from event message.
     */
    private NotificationCallback createCallbackNotificationRecord(EventMessage eventMessage, String notificationId,
                                                                 String recipientType, String recipientId,
                                                                 MongoTemplate mongoTemplate) {
        try {
            // Determine template ID (use the same logic as producer)
            String templateId = determineCallbackTemplateId(eventMessage);

            String resolvedCallbackUrl = callbackUrlResolutionService.resolveCallbackUrl(
                    recipientType,
                    recipientId,
                    eventMessage.getBusinessId(),
                    eventMessage.getTenantId()
            );

            NotificationCallback callbackNotification = NotificationCallback.builder()
                    .notificationId(notificationId)
                    .eventId(eventMessage.getEventId())
                    .eventType(eventMessage.getEventType())
                    .correlationId(eventMessage.getCorrelationId())
                    .businessId(eventMessage.getBusinessId())
                    .recipientType(recipientType)
                    .recipientId(recipientId)
                    .callbackUrl(resolvedCallbackUrl)
                    .eventType(eventMessage.getEventType())
                    .eventData(eventMessage.getEventPayload())
                    .status(NotificationStatus.PENDING.name())
                    .priority(EventPriority.HIGH.name())
                    .attemptCount(0)
                    .maxAttempts(retryPolicyService.getMaxRetryAttempts(NotificationChannel.CALLBACK.name()))
                    .isAsync(true)
                    .build();

            callbackRepository.save(callbackNotification, mongoTemplate);
            log.info("Created Callback notification record: notificationId={}, eventId={}, recipientType={}, recipientId={}",
                    notificationId, eventMessage.getEventId(), recipientType, recipientId);
            return callbackNotification;

        } catch (Exception e) {
            log.error("Failed to create Callback notification record: {}", e.getMessage());

            // Mark as failed in database for audit
            try {
                NotificationCallback failedNotification = NotificationCallback.builder()
                        .notificationId(notificationId)
                        .eventId(eventMessage.getEventId())
                        .eventType(eventMessage.getEventType())
                        .correlationId(eventMessage.getCorrelationId())
                        .businessId(eventMessage.getBusinessId())
                        .recipientType(recipientType)
                        .recipientId(recipientId)
                        .eventType(eventMessage.getEventType())
                        .eventData(eventMessage.getEventPayload())
                        .status(NotificationStatus.FAILED.name())
                        .isAsync(true)
                        .build();
                callbackRepository.save(failedNotification, mongoTemplate);
            } catch (Exception saveException) {
                log.error("Failed to save failed Callback notification record: {}", saveException.getMessage());
            }

            return null;
        }
    }

    /**
     * Processes a single callback with the updated logic.
     */
    private void processCallback(NotificationCallback notification, EventMessage eventMessage,
                                MongoTemplate mongoTemplate) {
        try {
            // Resolve callback URL
            String resolvedCallbackUrl = callbackUrlResolutionService.resolveCallbackUrl(
                notification.getRecipientType(),
                notification.getRecipientId(),
                notification.getBusinessId(),
                eventMessage.getTenantId()
            );

            // Resolve template if specified
            String templateId = determineCallbackTemplateId(eventMessage);
            String resolvedPayload = createCallbackPayloadWithEventData(notification, eventMessage, templateId);

            // Create API request
            ApiRequest apiRequest = createApiRequest(notification, resolvedCallbackUrl, resolvedPayload);

            // Deliver API callback
            ApiResponse response = deliverApiCallback(apiRequest, eventMessage.getTenantId());

            // Update notification with delivery status
            if (response.isSuccess()) {
                // Parse response body to validate acknowledgement structure
                CallbackAcknowledgementResponseDto ackResponse = parseAcknowledgementResponse(response.getResponseBody());

                if (ackResponse != null && "ACKNOWLEDGED".equalsIgnoreCase(ackResponse.getStatus())) {
                    // Valid acknowledgement response
                    StatusHistoryEntry historyEntry = StatusHistoryEntry.builder()
                            .timestamp(LocalDateTime.now())
                            .status("ACKNOWLEDGED")
                            .acknowledgementId(ackResponse.getAcknowledgementId())
                            .remark(ackResponse.getRemark())
                            .source(StatusHistorySource.CALLBACK_RESPONSE)
                            .updatedBy(notification.getRecipientId())
                            .build();

                    callbackRepository.updateStatusWithHistory(
                            notification.getNotificationId(),
                            NotificationStatus.ACKNOWLEDGED.name(),
                            historyEntry,
                            mongoTemplate
                    );

                    retryPolicyService.recordRetryMetrics(NotificationChannel.CALLBACK.name(), notification.getAttemptCount(), true, null);

                    log.info("Callback acknowledged: notificationId={}, url={}, status={}, ackId={}, remark={}",
                            notification.getNotificationId(), resolvedCallbackUrl, response.getHttpStatusCode(),
                            ackResponse.getAcknowledgementId(), ackResponse.getRemark());
                } else {
                    // Invalid acknowledgement structure - mark as FAILED
                    String errorMsg = "Invalid acknowledgement response format. Expected {status: 'ACKNOWLEDGED', acknowledgementId: '...', remark: '...'}";
                    log.warn("Invalid callback response for notificationId={}: {}",
                            notification.getNotificationId(), response.getResponseBody());
                    handleDeliveryFailure(notification, errorMsg, mongoTemplate);
                }
            } else {
                handleDeliveryFailure(notification, response.getErrorMessage(), mongoTemplate);
            }

        } catch (CallbackUrlNotFoundException e) {
            log.error("Failed to resolve callback URL for notification: notificationId={}, recipientType={}, recipientId={}: {}",
                notification.getNotificationId(), notification.getRecipientType(),
                notification.getRecipientId(), e.getMessage());
            handleDeliveryFailure(notification, "Callback URL resolution failed: " + e.getMessage(), mongoTemplate);
        } catch (Exception e) {
            log.error("Error processing callback for notificationId={}: {}",
                     notification.getNotificationId(), e.getMessage(), e);
            handleDeliveryFailure(notification, e.getMessage(), mongoTemplate);
        }
    }

    /**
     * Validates Data Processor IDs against the database.
     */
    private List<String> validateDataProcessorIds(List<String> dataProcessorIds, String businessId,
                                                  MongoTemplate mongoTemplate) {
        if (dataProcessorIds == null || dataProcessorIds.isEmpty()) {
            log.debug("No data processor IDs specified for validation");
            return new ArrayList<>();
        }

        log.info("Validating {} data processor IDs for businessId: {}", dataProcessorIds.size(), businessId);

        try {
            var foundProcessors = dataProcessorRepository.findActiveByDataProcessorIdsAndBusinessId(
                dataProcessorIds, businessId, mongoTemplate);

            List<String> validIds = foundProcessors.stream()
                .map(DataProcessor::getDataProcessorId)
                .toList();

            log.info("Validated {} out of {} data processor IDs: {}",
                    validIds.size(), dataProcessorIds.size(), validIds);
            return validIds;

        } catch (Exception e) {
            log.error("Error validating data processor IDs: {}", e.getMessage());
            return new ArrayList<>(); // Return empty list on error, don't create callbacks for invalid IDs
        }
    }

    /**
     * Generates a unique notification ID using SecureRandom for cryptographic security.
     */
    private String generateNotificationId(String type) {
        return "NOTIF_" + type + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "_" + String.format("%05d", secureRandom.nextInt(100000));
    }

    /**
     * Determines callback template ID based on customer identifier type.
     */
    private String determineCallbackTemplateId(EventMessage eventMessage) {
        return eventMessage.getEventType();
    }

    /**
     * Creates callback payload with event data and optional template resolution.
     */
    private String createCallbackPayloadWithEventData(NotificationCallback notification, EventMessage eventMessage, String templateId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", notification.getNotificationId());
            payload.put("eventId", notification.getEventId());
            payload.put("businessId", notification.getBusinessId());
            payload.put("tenantId", eventMessage.getTenantId());
            payload.put("recipientType", notification.getRecipientType());
            payload.put("recipientId", notification.getRecipientId());
            payload.put("eventType", notification.getEventType());
            payload.put("correlationId", notification.getCorrelationId());
            payload.put("timestamp", Instant.now().toString());

            // Add event data
            if (eventMessage.getEventPayload() != null) {
                payload.put("eventData", eventMessage.getEventPayload());
            }

            // Add customer identifiers
            payload.put("customerIdentifiers", Map.of(
                "type", eventMessage.getCustomerIdentifiers().getType(),
                "value", eventMessage.getCustomerIdentifiers().getValue()
            ));

            return objectMapper.writeValueAsString(payload);

        } catch (Exception e) {
            log.error("Failed to create callback payload: {}", e.getMessage());
            // Return minimal JSON payload as fallback
            return String.format(
                "{\"notificationId\":\"%s\",\"eventId\":\"%s\",\"businessId\":\"%s\",\"tenantId\":\"%s\",\"timestamp\":\"%s\"}",
                notification.getNotificationId(),
                notification.getEventId(),
                notification.getBusinessId(),
                eventMessage.getTenantId(),
                Instant.now().toString()
            );
        }
    }

    /**
     * Parses and validates the callback acknowledgement response from DF/DP.
     *
     * @param responseBody The HTTP response body from the callback
     * @return CallbackAcknowledgementResponseDto if valid, null otherwise
     */
    private CallbackAcknowledgementResponseDto parseAcknowledgementResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Empty response body received from callback");
                return null;
            }

            CallbackAcknowledgementResponseDto response = objectMapper.readValue(
                    responseBody, CallbackAcknowledgementResponseDto.class);

            // Case-insensitive validation
            if (response.getStatus() != null &&
                    "ACKNOWLEDGED".equalsIgnoreCase(response.getStatus())) {
                return response;
            }

            // Log warning if status is SUCCESS/ACCEPTED instead of ACKNOWLEDGED
            if (response.getStatus() != null &&
                    ("SUCCESS".equalsIgnoreCase(response.getStatus()) ||
                     "ACCEPTED".equalsIgnoreCase(response.getStatus()))) {
                log.warn("Response status '{}' should be 'ACKNOWLEDGED'. Please update your callback response format.",
                        response.getStatus());
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to parse acknowledgement response: {}", e.getMessage());
            log.debug("Response body: {}", responseBody);
            return null;
        }
    }

    /**
     * Creates API request for callback delivery.
     */
    private ApiRequest createApiRequest(NotificationCallback notification, String callbackUrl, String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Event-Id", notification.getEventId());
        headers.set("X-Correlation-Id", notification.getCorrelationId());
        headers.set("X-Notification-Id", notification.getNotificationId());
        headers.set(HeaderConstants.X_TIMESTAMP, Instant.now().toString());

        return ApiRequest.builder()
                .url(callbackUrl)
                .method("POST")
                .headers(headers)
                .payload(payload)
                .timeout(apiTimeout)
                .build();
    }

    // ========================================
    // Own-Use Data Processor Handling
    // ========================================

    /**
     * Checks if a Data Processor is an "Own Use" DP for the given business.
     *
     * @param dpId Data Processor ID
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return true if the DP is an own-use DP
     */
    private boolean isOwnUseDataProcessor(String dpId, String businessId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("businessId").is(businessId)
                    .and("dataProcessorId").is(dpId)
                    .and("details").is("Own Use"));
            DataProcessor ownUseDp = mongoTemplate.findOne(query, DataProcessor.class);
            return ownUseDp != null;
        } catch (Exception e) {
            log.error("Error checking own-use DP: dpId={}, businessId={}, error={}", dpId, businessId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handles own-use DP callback creation and status update from the DF consumer path.
     * This avoids the race condition where System Registry looks for an ACKNOWLEDGED record
     * that the DP consumer hasn't created yet.
     *
     * For consent withdrawal/expiration events, this method:
     * 1. Finds the own-use DP for the business
     * 2. Creates a NotificationCallback record with ACKNOWLEDGED status
     * 3. Calls the notification module API to mark it as DELETED
     *
     * @param eventMessage The event message
     * @param mongoTemplate Tenant-specific MongoTemplate
     */
    private void handleOwnUseDpCallbackIfApplicable(EventMessage eventMessage, MongoTemplate mongoTemplate) {
        try {
            // Only handle consent withdrawal/expiration events
            if (!isConsentWithdrawEvent(eventMessage.getEventType())) {
                return;
            }

            // Find own-use DP for this business from the event's DP list
            List<String> dpIds = eventMessage.getDataProcessorIds();
            if (dpIds == null || dpIds.isEmpty()) {
                return;
            }

            Query query = new Query(Criteria.where("businessId").is(eventMessage.getBusinessId())
                    .and("dataProcessorId").in(dpIds)
                    .and("details").is("Own Use"));
            DataProcessor ownUseDp = mongoTemplate.findOne(query, DataProcessor.class);

            if (ownUseDp == null) {
                log.debug("No own-use DP found for businessId={}, skipping own-use DP handling", eventMessage.getBusinessId());
                return;
            }

            String dpId = ownUseDp.getDataProcessorId();
            log.info("Handling own-use DP callback from DF consumer path: eventId={}, dpId={}, businessId={}",
                    eventMessage.getEventId(), dpId, eventMessage.getBusinessId());

            // Idempotency check - skip if record already exists
            NotificationCallback existingCallback = findExistingCallbackNotification(
                    eventMessage.getEventId(), RecipientType.DATA_PROCESSOR.name(), dpId, mongoTemplate);
            if (existingCallback != null) {
                log.info("Own-use DP callback record already exists: notificationId={}, eventId={}, dpId={}",
                        existingCallback.getNotificationId(), eventMessage.getEventId(), dpId);
                return;
            }

            // Create NotificationCallback record with ACKNOWLEDGED status
            String notificationId = generateNotificationId("CALLBACK_DATA_PROCESSOR");

            StatusHistoryEntry acknowledgedEntry = StatusHistoryEntry.builder()
                    .timestamp(LocalDateTime.now())
                    .status(NotificationStatus.ACKNOWLEDGED.name())
                    .remark("Auto-acknowledged for own-use Data Processor")
                    .source(StatusHistorySource.SYSTEM)
                    .updatedBy("system")
                    .build();

            NotificationCallback callback = NotificationCallback.builder()
                    .notificationId(notificationId)
                    .eventId(eventMessage.getEventId())
                    .eventType(eventMessage.getEventType())
                    .correlationId(eventMessage.getCorrelationId())
                    .businessId(eventMessage.getBusinessId())
                    .recipientType(RecipientType.DATA_PROCESSOR.name())
                    .recipientId(dpId)
                    .callbackUrl(ownUseDp.getCallbackUrl() != null ? ownUseDp.getCallbackUrl() : "N/A")
                    .eventData(eventMessage.getEventPayload())
                    .status(NotificationStatus.ACKNOWLEDGED.name())
                    .priority(EventPriority.HIGH.name())
                    .attemptCount(0)
                    .maxAttempts(0)
                    .isAsync(true)
                    .statusHistory(List.of(acknowledgedEntry))
                    .build();

            callbackRepository.save(callback, mongoTemplate);
            log.info("Created own-use DP callback record: notificationId={}, eventId={}, dpId={}, status=ACKNOWLEDGED",
                    notificationId, eventMessage.getEventId(), dpId);

            // Update status to DELETED directly via DB
            StatusHistoryEntry deletedEntry = StatusHistoryEntry.builder()
                    .timestamp(LocalDateTime.now())
                    .status(NotificationStatus.DELETED.name())
                    .remark("Auto-deleted for own-use Data Processor - data managed by DF")
                    .source(StatusHistorySource.SYSTEM)
                    .updatedBy("system")
                    .build();

            callbackRepository.updateStatusWithHistory(
                    notificationId,
                    NotificationStatus.DELETED.name(),
                    deletedEntry,
                    mongoTemplate
            );

            log.info("Own-use DP callback marked as DELETED: notificationId={}, eventId={}, dpId={}",
                    notificationId, eventMessage.getEventId(), dpId);

        } catch (Exception e) {
            // Fire-and-forget: Log error but don't fail the DF callback flow
            log.error("Error handling own-use DP callback: eventId={}, error={}. DF callback processing continues.",
                    eventMessage.getEventId(), e.getMessage(), e);
        }
    }

    // ========================================
    // Consent Withdraw API Integration
    // ========================================

    /**
     * Calls the consent withdraw API if the event type requires it.
     * This is called before DF callbacks for CONSENT_EXPIRED and CONSENT_WITHDRAWN events.
     * The API saves withdrawal_data directly to the notification_events collection.
     *
     * This method follows fire-and-forget pattern - errors are logged but don't block callback processing.
     *
     * @param eventMessage The event message containing event details
     */
    private void callConsentWithdrawApiIfApplicable(EventMessage eventMessage) {
        try {
            // Check if event type requires withdraw API call
            if (!isConsentWithdrawEvent(eventMessage.getEventType())) {
                return;
            }

            // Extract consent ID from event payload
            String consentId = extractConsentId(eventMessage.getEventPayload());

            if (consentId == null || consentId.isEmpty()) {
                log.warn("Cannot call consent withdraw API - consentId not found in event payload. " +
                        "eventId={}, eventType={}", eventMessage.getEventId(), eventMessage.getEventType());
                return;
            }

            log.info("Calling consent withdraw API before DF callback: eventId={}, consentId={}, eventType={}",
                    eventMessage.getEventId(), consentId, eventMessage.getEventType());

            // Call the withdraw API (fire-and-forget)
            consentWithdrawService.withdrawConsent(
                    consentId,
                    eventMessage.getEventId(),
                    eventMessage.getTenantId(),
                    eventMessage.getBusinessId()
            );

        } catch (Exception e) {
            // Fire-and-forget: Log error but continue with callback processing
            log.error("Error in consent withdraw API call: eventId={}, error={}. Proceeding with DF callback.",
                    eventMessage.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Checks if the event type requires a consent withdraw API call.
     *
     * @param eventType The event type to check
     * @return true if consent withdraw API should be called
     */
    private boolean isConsentWithdrawEvent(String eventType) {
        return eventType != null && CONSENT_WITHDRAW_EVENT_TYPES.contains(eventType);
    }

    /**
     * Extracts the consent ID from the event payload.
     * Looks for 'consentId' in the event payload map.
     *
     * @param eventPayload The event payload map
     * @return The consent ID or null if not found
     */
    private String extractConsentId(Map<String, Object> eventPayload) {
        if (eventPayload == null) {
            return null;
        }

        // Try direct consentId field
        Object consentId = eventPayload.get("consentId");
        if (consentId != null) {
            return consentId.toString();
        }

        // Try nested eventData if present
        Object eventData = eventPayload.get("eventData");
        if (eventData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventDataMap = (Map<String, Object>) eventData;
            Object nestedConsentId = eventDataMap.get("consentId");
            if (nestedConsentId != null) {
                return nestedConsentId.toString();
            }
        }

        return null;
    }

}