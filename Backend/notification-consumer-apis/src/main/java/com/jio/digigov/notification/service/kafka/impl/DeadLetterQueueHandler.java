// package com.jio.digigov.notification.service.kafka.impl;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.jio.digigov.notification.dto.kafka.NotificationMessage;
// import com.jio.digigov.notification.entity.notification.NotificationCallback;
// import com.jio.digigov.notification.entity.notification.NotificationEmail;
// import com.jio.digigov.notification.entity.notification.NotificationSms;
// import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
// import com.jio.digigov.notification.repository.notification.NotificationEmailRepository;
// import com.jio.digigov.notification.repository.notification.NotificationSmsRepository;
// import com.jio.digigov.notification.util.MongoTemplateProvider;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.kafka.support.Acknowledgment;
// import org.springframework.kafka.support.KafkaHeaders;
// import org.springframework.messaging.handler.annotation.Header;
// import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.util.Map;
// import java.util.Optional;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.atomic.AtomicLong;

// /**
//  * Dead Letter Queue (DLQ) handler for processing permanently failed notification messages.
//  *
//  * This service handles messages that have exhausted all retry attempts or encountered
//  * non-retryable errors. It provides comprehensive failure analysis, notification
//  * status updates, and monitoring capabilities for failed message patterns.
//  *
//  * Key Responsibilities:
//  * 1. Process messages from DLQ topics for all notification channels
//  * 2. Update notification records with final failure status
//  * 3. Generate failure reports and analytics
//  * 4. Trigger alerts for systematic failures
//  * 5. Maintain failure metrics for monitoring dashboards
//  * 6. Support manual reprocessing of failed messages
//  *
//  * Monitoring Features:
//  * - Failure rate tracking per channel and tenant
//  * - Error pattern analysis for root cause identification
//  * - Alerting thresholds for systematic issues
//  * - Failure trend analysis for capacity planning
//  * - SLA breach notifications
//  *
//  * Recovery Features:
//  * - Manual message reprocessing capabilities
//  * - Bulk failure analysis and reporting
//  * - Automatic cleanup of old failure records
//  * - Integration with external monitoring systems
//  *
//  * @author Notification Service Team
//  * @version 1.0
//  * @since 2024-01-01
//  */
// @Service
// @Slf4j
// @RequiredArgsConstructor
// public class DeadLetterQueueHandler {

//     private final NotificationSmsRepository smsRepository;
//     private final NotificationEmailRepository emailRepository;
//     private final NotificationCallbackRepository callbackRepository;
//     private final MongoTemplateProvider mongoTemplateProvider;
//     private final ObjectMapper objectMapper;

//     // Failure metrics tracking
//     private final Map<String, AtomicLong> failureCountByChannel = new ConcurrentHashMap<>();
//     private final Map<String, AtomicLong> failureCountByTenant = new ConcurrentHashMap<>();
//     private final Map<String, AtomicLong> failureCountByErrorType = new ConcurrentHashMap<>();

//     /**
//      * Processes SMS notifications that have reached the dead letter queue.
//      */
//     @KafkaListener(
//         topics = "${kafka.dlq.sms}",
//         groupId = "${kafka.consumer.group-id}-dlq",
//         containerFactory = "dlqKafkaListenerContainerFactory"
//     )
//     public void handleSmsFailure(
//             @Payload NotificationMessage message,
//             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//             @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage,
//             Acknowledgment acknowledgment) {

//         log.error("Processing SMS DLQ message: messageId={}, eventId={}, error={}",
//                 message.getMessageId(), message.getEventId(), errorMessage);

//         try {
//             String notificationId = message.getMetadata().getNotificationId();
//             var mongoTemplate = mongoTemplateProvider.getTemplate(message.getTenantId());

//             // Find and update SMS notification record
//             Optional<NotificationSms> notificationOpt = smsRepository
//                     .findByNotificationId(notificationId, mongoTemplate);

//             if (notificationOpt.isPresent()) {
//                 NotificationSms notification = notificationOpt.get();

//                 // Update to permanent failure status
//                 smsRepository.updateStatusAndProcessedAt(
//                         notificationId,
//                         "PERMANENTLY_FAILED",
//                         LocalDateTime.now(),
//                         mongoTemplate
//                 );

//                 // Record failure metrics
//                 recordFailureMetrics("SMS", message.getTenantId(), errorMessage);

//                 log.error("SMS notification permanently failed: notificationId={}, mobile={}, error={}",
//                         notificationId, notification.getMobile(), errorMessage);

//                 // Generate failure alert if needed
//                 checkAndGenerateAlert("SMS", message.getTenantId(), errorMessage);

//             } else {
//                 log.error("SMS notification record not found in DLQ processing: notificationId={}", notificationId);
//             }

//             acknowledgment.acknowledge();

//         } catch (Exception e) {
//             log.error("Error processing SMS DLQ message: messageId={}, error={}",
//                     message.getMessageId(), e.getMessage(), e);
//             acknowledgment.acknowledge(); // Acknowledge to prevent infinite loop
//         }
//     }

//     /**
//      * Processes Email notifications that have reached the dead letter queue.
//      */
//     @KafkaListener(
//         topics = "${kafka.dlq.email}",
//         groupId = "${kafka.consumer.group-id}-dlq",
//         containerFactory = "dlqKafkaListenerContainerFactory"
//     )
//     public void handleEmailFailure(
//             @Payload NotificationMessage message,
//             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//             @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage,
//             Acknowledgment acknowledgment) {

//         log.error("Processing Email DLQ message: messageId={}, eventId={}, error={}",
//                 message.getMessageId(), message.getEventId(), errorMessage);

//         try {
//             String notificationId = message.getMetadata().getNotificationId();
//             var mongoTemplate = mongoTemplateProvider.getTemplate(message.getTenantId());

//             // Find and update Email notification record
//             Optional<NotificationEmail> notificationOpt = emailRepository
//                     .findByNotificationId(notificationId, mongoTemplate);

//             if (notificationOpt.isPresent()) {
//                 NotificationEmail notification = notificationOpt.get();

//                 // Update to permanent failure status
//                 emailRepository.updateStatusAndProcessedAt(
//                         notificationId,
//                         "PERMANENTLY_FAILED",
//                         LocalDateTime.now(),
//                         mongoTemplate
//                 );

//                 // Record failure metrics
//                 recordFailureMetrics("EMAIL", message.getTenantId(), errorMessage);

//                 log.error("Email notification permanently failed: notificationId={}, email={}, error={}",
//                         notificationId, notification.getToEmail(), errorMessage);

//                 // Generate failure alert if needed
//                 checkAndGenerateAlert("EMAIL", message.getTenantId(), errorMessage);

//             } else {
//                 log.error("Email notification record not found in DLQ processing: notificationId={}", notificationId);
//             }

//             acknowledgment.acknowledge();

//         } catch (Exception e) {
//             log.error("Error processing Email DLQ message: messageId={}, error={}",
//                     message.getMessageId(), e.getMessage(), e);
//             acknowledgment.acknowledge(); // Acknowledge to prevent infinite loop
//         }
//     }

//     /**
//      * Processes Callback notifications that have reached the dead letter queue.
//      */
//     @KafkaListener(
//         topics = "${kafka.dlq.callback}",
//         groupId = "${kafka.consumer.group-id}-dlq",
//         containerFactory = "dlqKafkaListenerContainerFactory"
//     )
//     public void handleCallbackFailure(
//             @Payload NotificationMessage message,
//             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//             @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage,
//             Acknowledgment acknowledgment) {

//         log.error("Processing Callback DLQ message: messageId={}, eventId={}, error={}",
//                 message.getMessageId(), message.getEventId(), errorMessage);

//         try {
//             String notificationId = message.getMetadata().getNotificationId();
//             var mongoTemplate = mongoTemplateProvider.getTemplate(message.getTenantId());

//             // Find and update Callback notification record
//             Optional<NotificationCallback> notificationOpt = callbackRepository
//                     .findByNotificationId(notificationId, mongoTemplate);

//             if (notificationOpt.isPresent()) {
//                 NotificationCallback notification = notificationOpt.get();

//                 // Update to permanent failure status
//                 callbackRepository.updateStatusAndProcessedAt(
//                         notificationId,
//                         "PERMANENTLY_FAILED",
//                         LocalDateTime.now(),
//                         mongoTemplate
//                 );

//                 // Record failure metrics
//                 recordFailureMetrics("CALLBACK", message.getTenantId(), errorMessage);

//                 log.error("Callback notification permanently failed: notificationId={}, webhookUrl={}, error={}",
//                         notificationId, notification.getWebhookUrl(), errorMessage);

//                 // Generate failure alert if needed
//                 checkAndGenerateAlert("CALLBACK", message.getTenantId(), errorMessage);

//             } else {
//                 log.error("Callback notification record not found in DLQ processing: notificationId={}", notificationId);
//             }

//             acknowledgment.acknowledge();

//         } catch (Exception e) {
//             log.error("Error processing Callback DLQ message: messageId={}, error={}",
//                     message.getMessageId(), e.getMessage(), e);
//             acknowledgment.acknowledge(); // Acknowledge to prevent infinite loop
//         }
//     }

//     /**
//      * Records failure metrics for monitoring and analysis.
//      */
//     private void recordFailureMetrics(String channel, String tenantId, String errorMessage) {
//         try {
//             // Increment channel failure count
//             failureCountByChannel.computeIfAbsent(channel, k -> new AtomicLong(0))
//                     .incrementAndGet();

//             // Increment tenant failure count
//             failureCountByTenant.computeIfAbsent(tenantId, k -> new AtomicLong(0))
//                     .incrementAndGet();

//             // Classify and count error types
//             String errorType = classifyError(errorMessage);
//             failureCountByErrorType.computeIfAbsent(errorType, k -> new AtomicLong(0))
//                     .incrementAndGet();

//             log.debug("Recorded failure metrics: channel={}, tenant={}, errorType={}, totalChannelFailures={}",
//                     channel, tenantId, errorType, failureCountByChannel.get(channel).get());

//         } catch (Exception e) {
//             log.error("Error recording failure metrics: {}", e.getMessage());
//         }
//     }

//     /**
//      * Classifies error messages into categories for better analysis.
//      */
//     private String classifyError(String errorMessage) {
//         if (errorMessage == null || errorMessage.trim().isEmpty()) {
//             return "UNKNOWN";
//         }

//         String upperError = errorMessage.toUpperCase();

//         if (upperError.contains("TIMEOUT") || upperError.contains("CONNECTION")) {
//             return "NETWORK_TIMEOUT";
//         } else if (upperError.contains("AUTHENTICATION") || upperError.contains("AUTHORIZATION")) {
//             return "AUTH_FAILURE";
//         } else if (upperError.contains("INVALID") || upperError.contains("MALFORMED")) {
//             return "VALIDATION_ERROR";
//         } else if (upperError.contains("RATE_LIMIT") || upperError.contains("QUOTA")) {
//             return "RATE_LIMIT";
//         } else if (upperError.contains("SERVICE_UNAVAILABLE") || upperError.contains("INTERNAL_SERVER_ERROR")) {
//             return "SERVICE_ERROR";
//         } else if (upperError.contains("NOT_FOUND") || upperError.contains("TEMPLATE")) {
//             return "CONFIGURATION_ERROR";
//         } else {
//             return "OTHER";
//         }
//     }

//     /**
//      * Checks if failure patterns warrant generating alerts.
//      */
//     private void checkAndGenerateAlert(String channel, String tenantId, String errorMessage) {
//         try {
//             long channelFailures = failureCountByChannel.getOrDefault(channel, new AtomicLong(0)).get();
//             long tenantFailures = failureCountByTenant.getOrDefault(tenantId, new AtomicLong(0)).get();

//             // Alert thresholds (these could be configurable)
//             int channelAlertThreshold = 100; // 100 failures per channel
//             int tenantAlertThreshold = 50;   // 50 failures per tenant

//             if (channelFailures % channelAlertThreshold == 0 && channelFailures > 0) {
//                 log.error("ALERT: High failure count for channel {}: {} failures", channel, channelFailures);
//                 // Here you could integrate with external alerting systems
//                 generateSystemAlert("HIGH_CHANNEL_FAILURES", channel, channelFailures);
//             }

//             if (tenantFailures % tenantAlertThreshold == 0 && tenantFailures > 0) {
//                 log.error("ALERT: High failure count for tenant {}: {} failures", tenantId, tenantFailures);
//                 // Here you could integrate with external alerting systems
//                 generateSystemAlert("HIGH_TENANT_FAILURES", tenantId, tenantFailures);
//             }

//         } catch (Exception e) {
//             log.error("Error checking alert conditions: {}", e.getMessage());
//         }
//     }

//     /**
//      * Generates system alerts for external monitoring systems.
//      */
//     private void generateSystemAlert(String alertType, String entity, long failureCount) {
//         // This method would integrate with external alerting systems
//         // such as PagerDuty, Slack, email alerts, etc.
//         log.warn("Generated system alert: type={}, entity={}, failureCount={}", alertType, entity, failureCount);

//         // Example integration points:
//         // - Send to monitoring dashboard
//         // - Trigger PagerDuty incident
//         // - Send Slack notification
//         // - Email operations team
//         // - Update service health status
//     }

//     /**
//      * Gets current failure metrics for monitoring dashboards.
//      */
//     public Map<String, Object> getFailureMetrics() {
//         return Map.of(
//             "failuresByChannel", Map.copyOf(failureCountByChannel),
//             "failuresByTenant", Map.copyOf(failureCountByTenant),
//             "failuresByErrorType", Map.copyOf(failureCountByErrorType),
//             "timestamp", LocalDateTime.now()
//         );
//     }

//     /**
//      * Resets failure metrics (useful for testing or periodic cleanup).
//      */
//     public void resetMetrics() {
//         failureCountByChannel.clear();
//         failureCountByTenant.clear();
//         failureCountByErrorType.clear();
//         log.info("DLQ failure metrics reset");
//     }

//     /**
//      * Gets failure statistics for a specific channel.
//      */
//     public long getChannelFailureCount(String channel) {
//         return failureCountByChannel.getOrDefault(channel, new AtomicLong(0)).get();
//     }

//     /**
//      * Gets failure statistics for a specific tenant.
//      */
//     public long getTenantFailureCount(String tenantId) {
//         return failureCountByTenant.getOrDefault(tenantId, new AtomicLong(0)).get();
//     }

//     /**
//      * Gets failure statistics for a specific error type.
//      */
//     public long getErrorTypeFailureCount(String errorType) {
//         return failureCountByErrorType.getOrDefault(errorType, new AtomicLong(0)).get();
//     }
// }