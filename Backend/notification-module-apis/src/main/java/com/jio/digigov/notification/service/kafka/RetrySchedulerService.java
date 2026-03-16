package com.jio.digigov.notification.service.kafka;

import com.jio.digigov.notification.constant.NotificationConstants;
import com.jio.digigov.notification.dto.kafka.EventMessage;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.entity.event.NotificationEvent;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.NotificationEmail;
import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.repository.notification.NotificationEmailRepository;
import com.jio.digigov.notification.repository.notification.NotificationSmsRepository;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Scheduled service for processing retry-scheduled notifications.
 *
 * This service runs as a background job that periodically scans the database for
 * notifications that are scheduled for retry (status = NotificationStatus.RETRY_SCHEDULED.name()) and
 * have reached their retry time (nextRetryAt <= now). It recreates the original
 * Kafka messages and republishes them to the appropriate topics for reprocessing.
 *
 * Key Features:
 * - Processes all notification types (SMS, Email, Callback)
 * - Updates notification status to prevent duplicate processing
 * - Handles tenant-specific databases with proper isolation
 * - Comprehensive error handling and logging
 * - Configurable scheduling interval and batch sizes
 * - Metrics tracking for monitoring retry processing
 *
 * Processing Flow:
 * 1. Query each tenant database for retry-scheduled notifications
 * 2. Filter notifications where nextRetryAt <= now
 * 3. Update status to NotificationStatus.PROCESSING.name() to prevent duplicate processing
 * 4. Create EventMessage from database record
 * 5. Publish message to appropriate Kafka topic
 * 6. Log success/failure for monitoring
 *
 * Configuration:
 * - retry.scheduler.enabled: Enable/disable scheduler (default: true)
 * - retry.scheduler.fixed-delay: Delay between runs in ms (default: 30000)
 * - retry.scheduler.batch-size: Max notifications per batch (default: 100)
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "retry.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class RetrySchedulerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final NotificationSmsRepository smsRepository;
    private final NotificationEmailRepository emailRepository;
    private final NotificationCallbackRepository callbackRepository;

    @Value("${kafka.topics.sms}")
    private String smsTopicName;

    @Value("${kafka.topics.email}")
    private String emailTopicName;

    @Value("${kafka.topics.callback}")
    private String callbackTopicName;

    @Value("${retry.scheduler.batch-size:100}")
    private int batchSize;

    @Value("${retry.scheduler.initial-delay:10000}")
    private long initialDelay;

    // Metrics tracking
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);

    public RetrySchedulerService(KafkaTemplate<String, Object> kafkaTemplate,
                                MongoTemplateProvider mongoTemplateProvider,
                                NotificationSmsRepository smsRepository,
                                NotificationEmailRepository emailRepository,
                                NotificationCallbackRepository callbackRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.smsRepository = smsRepository;
        this.emailRepository = emailRepository;
        this.callbackRepository = callbackRepository;

        log.info("RetrySchedulerService initialized - will process retries every {} ms with {} ms initial delay",
                "${retry.scheduler.fixed-delay:30000}", "${retry.scheduler.initial-delay:10000}");
    }

    /**
     * Startup recovery method to handle notifications stuck in PROCESSING status.
     * This can happen when the application crashes after setting status to PROCESSING
     * but before the consumer processes the message.
     */
    @PostConstruct
    public void recoverStuckNotifications() {
        try {
            log.info("Starting notification recovery for stuck PROCESSING notifications");

            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5); // Consider stuck if processing > 5 mins
            int recoveredCount = 0;

            List<String> tenantIds = List.copyOf(mongoTemplateProvider.getCachedTenants());

            for (String tenantId : tenantIds) {
                try {
                    MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

                    // Reset stuck SMS notifications
                    recoveredCount += recoverStuckNotifications(mongoTemplate, "notification_sms", cutoffTime);

                    // Reset stuck Email notifications
                    recoveredCount += recoverStuckNotifications(mongoTemplate, "notification_email", cutoffTime);

                    // Reset stuck Callback notifications
                    recoveredCount += recoverStuckNotifications(mongoTemplate, "notification_callback", cutoffTime);

                } catch (Exception e) {
                    log.error("Error recovering stuck notifications for tenant {}: {}", tenantId, e.getMessage());
                }
            }

            if (recoveredCount > 0) {
                log.info("Recovered {} stuck notifications on startup", recoveredCount);
            } else {
                log.debug("No stuck notifications found during startup recovery");
            }

        } catch (Exception e) {
            log.error("Error during startup notification recovery: {}", e.getMessage());
        }
    }

    /**
     * Recovers stuck notifications for a specific collection.
     */
    private int recoverStuckNotifications(MongoTemplate mongoTemplate, String collection, LocalDateTime cutoffTime) {
        try {
            // Find notifications stuck in PROCESSING status for more than cutoff time
            Query query = new Query(Criteria.where("status").is(NotificationStatus.PROCESSING.name())
                    .and("lastAttemptAt").lt(cutoffTime));

            Update update = new Update()
                    .set("status", NotificationStatus.RETRY_SCHEDULED.name())
                    .set("lastAttemptAt", LocalDateTime.now());

            var result = mongoTemplate.updateMulti(query, update, collection);
            int recovered = (int) result.getModifiedCount();

            if (recovered > 0) {
                log.info("Recovered {} stuck {} notifications", recovered, collection);
            }

            return recovered;

        } catch (Exception e) {
            log.error("Error recovering stuck notifications for collection {}: {}", collection, e.getMessage());
            return 0;
        }
    }

    /**
     * Main scheduled method that processes retry-scheduled notifications.
     * Runs every 30 seconds by default to check for notifications ready for retry.
     */
    @Scheduled(fixedDelayString = "${retry.scheduler.fixed-delay:30000}",
               initialDelayString = "${retry.scheduler.initial-delay:10000}")
    public void processScheduledRetries() {
        try {
            log.info("Starting scheduled retry processing at {}", LocalDateTime.now());

            LocalDateTime now = LocalDateTime.now();
            long batchStartTime = System.currentTimeMillis();

            // Process retries for all notification types
            int smsProcessed = processSmsRetries(now);
            int emailProcessed = processEmailRetries(now);
            int callbackProcessed = processCallbackRetries(now);

            int totalBatchProcessed = smsProcessed + emailProcessed + callbackProcessed;
            long batchDuration = System.currentTimeMillis() - batchStartTime;

            if (totalBatchProcessed > 0) {
                log.info("Completed retry processing: SMS={}, Email={}, Callback={}, total={}, duration={}ms",
                        smsProcessed, emailProcessed, callbackProcessed, totalBatchProcessed, batchDuration);
            } else {
                log.debug("No notifications ready for retry at {}", now);
            }

            totalProcessed.addAndGet(totalBatchProcessed);

        } catch (Exception e) {
            log.error("Error during scheduled retry processing: {}", e.getMessage());
            totalFailures.incrementAndGet();
        }
    }

    /**
     * Processes SMS notifications scheduled for retry.
     *
     * @param now Current timestamp for comparison
     * @return Number of SMS notifications processed
     */
    private int processSmsRetries(LocalDateTime now) {
        int processed = 0;

        try {
            // Get cached tenants (tenants that have processed notifications)
            List<String> tenantIds = List.copyOf(mongoTemplateProvider.getCachedTenants());

            log.debug("Processing SMS retries for {} cached tenants: {}", tenantIds.size(), tenantIds);

            for (String tenantId : tenantIds) {
                try {
                    MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

                    // Query for SMS notifications ready for retry
                    Query query = new Query(Criteria.where("status").is(NotificationStatus.RETRY_SCHEDULED.name())
                            .and("nextRetryAt").lte(now))
                            .limit(batchSize);

                    List<NotificationSms> notifications = mongoTemplate.find(query, NotificationSms.class);

                    log.debug("Found {} SMS notifications ready for retry in tenant {}", notifications.size(), tenantId);

                    // Group notifications by eventId to avoid sending duplicate events
                    Map<String, List<NotificationSms>> eventGroups = notifications.stream()
                            .collect(Collectors.groupingBy(NotificationSms::getEventId));

                    for (Map.Entry<String, List<NotificationSms>> entry : eventGroups.entrySet()) {
                        String eventId = entry.getKey();
                        List<NotificationSms> eventNotifications = entry.getValue();

                        try {
                            // Update all notifications to PROCESSING status
                            for (NotificationSms notification : eventNotifications) {
                                updateNotificationStatus(notification.getNotificationId(), NotificationStatus.PROCESSING.name(),
                                        tenantId, mongoTemplate, "notification_sms");
                            }

                            // Send EventMessage only to SMS topic for this event
                            if (processEventRetryForChannel(eventId, tenantId, mongoTemplate, "SMS")) {
                                processed += eventNotifications.size(); // Count all notifications in this event
                                totalSuccess.addAndGet(eventNotifications.size());
                            } else {
                                totalFailures.addAndGet(eventNotifications.size());
                            }
                        } catch (Exception e) {
                            log.error("Failed to process SMS event retry for eventId {}: {}", eventId, e.getMessage());
                            totalFailures.addAndGet(eventNotifications.size());
                        }
                    }

                } catch (Exception e) {
                    log.error("Error processing SMS retries for tenant {}: {}", tenantId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error getting tenant list for SMS retries: {}", e.getMessage());
        }

        return processed;
    }

    /**
     * Processes Email notifications scheduled for retry.
     *
     * @param now Current timestamp for comparison
     * @return Number of Email notifications processed
     */
    private int processEmailRetries(LocalDateTime now) {
        int processed = 0;

        try {
            // Get cached tenants (tenants that have processed notifications)
            List<String> tenantIds = List.copyOf(mongoTemplateProvider.getCachedTenants());

            for (String tenantId : tenantIds) {
                try {
                    MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

                    // Query for Email notifications ready for retry
                    Query query = new Query(Criteria.where("status").is(NotificationStatus.RETRY_SCHEDULED.name())
                            .and("nextRetryAt").lte(now))
                            .limit(batchSize);

                    List<NotificationEmail> notifications = mongoTemplate.find(query, NotificationEmail.class);

                    // Group notifications by eventId to avoid sending duplicate events
                    Map<String, List<NotificationEmail>> eventGroups = notifications.stream()
                            .collect(Collectors.groupingBy(NotificationEmail::getEventId));

                    for (Map.Entry<String, List<NotificationEmail>> entry : eventGroups.entrySet()) {
                        String eventId = entry.getKey();
                        List<NotificationEmail> eventNotifications = entry.getValue();

                        try {
                            // Update all notifications to PROCESSING status
                            for (NotificationEmail notification : eventNotifications) {
                                updateNotificationStatus(notification.getNotificationId(), NotificationStatus.PROCESSING.name(),
                                        tenantId, mongoTemplate, "notification_email");
                            }

                            // Send EventMessage only to Email topic for this event
                            if (processEventRetryForChannel(eventId, tenantId, mongoTemplate, "EMAIL")) {
                                processed += eventNotifications.size(); // Count all notifications in this event
                                totalSuccess.addAndGet(eventNotifications.size());
                            } else {
                                totalFailures.addAndGet(eventNotifications.size());
                            }
                        } catch (Exception e) {
                            log.error("Failed to process Email event retry for eventId {}: {}", eventId, e.getMessage());
                            totalFailures.addAndGet(eventNotifications.size());
                        }
                    }

                } catch (Exception e) {
                    log.error("Error processing Email retries for tenant {}: {}", tenantId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error getting tenant list for Email retries: {}", e.getMessage());
        }

        return processed;
    }

    /**
     * Processes Callback notifications scheduled for retry.
     *
     * @param now Current timestamp for comparison
     * @return Number of Callback notifications processed
     */
    private int processCallbackRetries(LocalDateTime now) {
        int processed = 0;

        try {
            // Get cached tenants (tenants that have processed notifications)
            List<String> tenantIds = List.copyOf(mongoTemplateProvider.getCachedTenants());

            log.debug("Processing Callback retries for {} cached tenants: {}", tenantIds.size(), tenantIds);

            for (String tenantId : tenantIds) {
                try {
                    MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

                    // Query for Callback notifications ready for retry
                    Query query = new Query(Criteria.where("status").is(NotificationStatus.RETRY_SCHEDULED.name())
                            .and("nextRetryAt").lte(now))
                            .limit(batchSize);

                    List<NotificationCallback> notifications = mongoTemplate.find(query, NotificationCallback.class);

                    log.debug("Found {} Callback notifications ready for retry in tenant {}", notifications.size(), tenantId);

                    // Process each notification individually to ensure only failed callbacks are retried
                    for (NotificationCallback notification : notifications) {
                        try {
                            // Update notification to PROCESSING status
                            updateNotificationStatus(notification.getNotificationId(), NotificationStatus.PROCESSING.name(),
                                    tenantId, mongoTemplate, "notification_callback");

                            // Send EventMessage only to Callback topic for this specific recipient
                            String messageKey = buildMessageKeyForRecipient(notification.getEventId(),
                                    notification.getRecipientType(), notification.getRecipientId());

                            if (processCallbackRetryForSpecificRecipient(notification.getEventId(), tenantId,
                                    mongoTemplate, notification.getRecipientType(), notification.getRecipientId(), messageKey)) {
                                processed++;
                                totalSuccess.incrementAndGet();
                                log.info("Successfully retried callback for notificationId={}, recipientType={}, recipientId={}",
                                        notification.getNotificationId(), notification.getRecipientType(), notification.getRecipientId());
                            } else {
                                totalFailures.incrementAndGet();
                            }
                        } catch (Exception e) {
                            log.error("Failed to process Callback retry for notificationId {}: {}",
                                    notification.getNotificationId(), e.getMessage(), e);
                            totalFailures.incrementAndGet();
                        }
                    }

                } catch (Exception e) {
                    log.error("Error processing Callback retries for tenant {}: {}", tenantId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error getting tenant list for Callback retries: {}", e.getMessage());
        }

        return processed;
    }

    /**
     * Builds a message key for a specific recipient (DF or DP).
     * This matches the key format used in the producer.
     */
    private String buildMessageKeyForRecipient(String eventId, String recipientType, String recipientId) {
        if (RecipientType.DATA_FIDUCIARY.name().equals(recipientType)) {
            return eventId + NotificationConstants.MESSAGE_KEY_SUFFIX_DATA_FIDUCIARY;
        } else if (RecipientType.DATA_PROCESSOR.name().equals(recipientType)) {
            return eventId + NotificationConstants.MESSAGE_KEY_SUFFIX_DATA_PROCESSOR + recipientId;
        } else {
            throw new IllegalArgumentException("Unknown recipient type: " + recipientType);
        }
    }

    /**
     * Processes callback retry for a specific recipient only (DF or specific DP).
     * This ensures that only the failed callback is retried, not all callbacks for the event.
     */
    private boolean processCallbackRetryForSpecificRecipient(String eventId, String tenantId,
                                                              MongoTemplate mongoTemplate,
                                                              String recipientType, String recipientId,
                                                              String messageKey) {
        try {
            // Look up the original event data
            Query eventQuery = new Query(Criteria.where("eventId").is(eventId));
            NotificationEvent event = mongoTemplate.findOne(eventQuery, NotificationEvent.class);
            if (event == null) {
                log.error("Original event not found for eventId: {}, tenantId: {}", eventId, tenantId);
                return false;
            }

            // Create EventMessage from the original event data
            EventMessage.EventMessageBuilder messageBuilder = EventMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .eventId(event.getEventId())
                    .correlationId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .businessId(event.getBusinessId())
                    .transactionId("RETRY_" + System.currentTimeMillis())
                    .eventType(event.getEventType())
                    .resource(event.getResource())
                    .source("retry-scheduler")
                    .customerIdentifiers(EventMessage.CustomerIdentifiers.builder()
                            .type(event.getCustomerIdentifiers().getType())
                            .value(event.getCustomerIdentifiers().getValue())
                            .build())
                    .language(event.getLanguage())
                    .eventPayload(event.getEventPayload())
                    .timestamp(LocalDateTime.now());

            // For DATA_PROCESSOR, include only the specific DP ID that needs retry
            if (RecipientType.DATA_PROCESSOR.name().equals(recipientType)) {
                messageBuilder.dataProcessorIds(List.of(recipientId));
            } else {
                // For DATA_FIDUCIARY, include all DP IDs (if any) from original event
                messageBuilder.dataProcessorIds(event.getDataProcessorIds());
            }

            EventMessage eventMessage = messageBuilder.build();

            // Send to callback topic with the specific message key
            kafkaTemplate.send(callbackTopicName, messageKey, eventMessage);
            log.info("Successfully republished callback event for retry: eventId={}, recipientType={}, recipientId={}, messageKey={}",
                    eventId, recipientType, recipientId, messageKey);

            return true;

        } catch (Exception e) {
            log.error("Failed to process callback retry for eventId {}, recipientType {}, recipientId {}: {}",
                    eventId, recipientType, recipientId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Processes any notification retry by recreating the original EventMessage.
     *
     * This method looks up the original event data and sends EventMessage to all topics,
     * allowing consumers to process the event again with the same logic as initial processing.
     */
    private boolean processEventRetryForChannel(String eventId, String tenantId, MongoTemplate mongoTemplate, String channel) {
        try {
            // Look up the original event data using MongoTemplate directly
            Query eventQuery = new Query(Criteria.where("eventId").is(eventId));
            NotificationEvent event = mongoTemplate.findOne(eventQuery, NotificationEvent.class);
            if (event == null) {
                log.error("Original event not found for eventId: {}, tenantId: {}", eventId, tenantId);
                return false;
            }

            // Create EventMessage from the original event data
            EventMessage eventMessage = EventMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .eventId(event.getEventId())
                    .correlationId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .businessId(event.getBusinessId())
                    .transactionId("RETRY_" + System.currentTimeMillis())
                    .eventType(event.getEventType())
                    .resource(event.getResource())
                    .source("retry-scheduler")
                    .customerIdentifiers(EventMessage.CustomerIdentifiers.builder()
                            .type(event.getCustomerIdentifiers().getType())
                            .value(event.getCustomerIdentifiers().getValue())
                            .build())
                    .language(event.getLanguage())
                    .dataProcessorIds(event.getDataProcessorIds())
                    .eventPayload(event.getEventPayload())
                    .timestamp(LocalDateTime.now())
                    .build();

            // Send EventMessage only to the specific channel topic
            publishEventToSpecificTopic(eventMessage, eventId, channel);

            log.info("Successfully republished event for retry: eventId={}, tenantId={}, channel={}", eventId, tenantId, channel);
            return true;

        } catch (Exception e) {
            log.error("Failed to process event retry for eventId {}, channel {}: {}", eventId, channel, e.getMessage());
            return false;
        }
    }

    /**
     * Publishes EventMessage to a specific Kafka topic based on channel.
     */
    private void publishEventToSpecificTopic(EventMessage eventMessage, String eventId, String channel) {
        try {
            switch (channel.toUpperCase()) {
                case NotificationConstants.CHANNEL_SMS:
                    kafkaTemplate.send(smsTopicName, eventId, eventMessage);
                    log.debug("Sent retry event message to SMS topic: eventId={}", eventId);
                    break;

                case NotificationConstants.CHANNEL_EMAIL:
                    kafkaTemplate.send(emailTopicName, eventId, eventMessage);
                    log.debug("Sent retry event message to Email topic: eventId={}", eventId);
                    break;

                case NotificationConstants.CHANNEL_CALLBACK:
                    // Send to Callback topic for Data Fiduciary
                    kafkaTemplate.send(callbackTopicName,
                            eventId + NotificationConstants.MESSAGE_KEY_SUFFIX_DATA_FIDUCIARY,
                            eventMessage);
                    log.debug("Sent retry event message to Callback topic for DF: eventId={}", eventId);

                    // Send separate messages to Callback topic for each Data Processor
                    List<String> dataProcessorIds = eventMessage.getDataProcessorIds();
                    if (dataProcessorIds != null && !dataProcessorIds.isEmpty()) {
                        for (int i = 0; i < dataProcessorIds.size(); i++) {
                            String dpId = dataProcessorIds.get(i);
                            // Create a copy of the event message with only this DP ID
                            EventMessage dpEventMessage = EventMessage.builder()
                                    .messageId(eventMessage.getMessageId() + NotificationConstants.MESSAGE_KEY_SUFFIX_DATA_PROCESSOR + i)
                                    .eventId(eventMessage.getEventId())
                                    .correlationId(eventMessage.getCorrelationId())
                                    .tenantId(eventMessage.getTenantId())
                                    .businessId(eventMessage.getBusinessId())
                                    .transactionId(eventMessage.getTransactionId())
                                    .eventType(eventMessage.getEventType())
                                    .resource(eventMessage.getResource())
                                    .source(eventMessage.getSource())
                                    .customerIdentifiers(eventMessage.getCustomerIdentifiers())
                                    .language(eventMessage.getLanguage())
                                    .dataProcessorIds(List.of(dpId)) // Only this DP ID
                                    .eventPayload(eventMessage.getEventPayload())
                                    .timestamp(eventMessage.getTimestamp())
                                    .build();

                            kafkaTemplate.send(callbackTopicName,
                                    eventId + NotificationConstants.MESSAGE_KEY_SUFFIX_DATA_PROCESSOR + dpId,
                                    dpEventMessage);
                            log.debug("Sent retry event message to Callback topic for DP {}: eventId={}", dpId, eventId);
                        }
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown channel: " + channel);
            }

            log.info("Published retry event to {} topic: eventId={}", channel, eventId);

        } catch (Exception e) {
            log.error("Failed to publish retry event to {} topic: eventId={}, error={}", channel, eventId, e.getMessage());
            throw new RuntimeException("Failed to publish retry event to Kafka", e);
        }
    }

    // Note: Old processNotificationRetry methods removed - now using processEventRetry with EventMessage

    /**
     * Updates notification status in MongoDB.
     */
    private void updateNotificationStatus(String notificationId, String status, String tenantId,
                                        MongoTemplate mongoTemplate, String collection) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("lastAttemptAt", LocalDateTime.now());

            mongoTemplate.updateFirst(query, update, collection);

        } catch (Exception e) {
            log.error("Failed to update notification status: notificationId={}, status={}, error={}",
                    notificationId, status, e.getMessage(), e);
        }
    }

    /**
     * Gets retry processing metrics for monitoring.
     *
     * @return Map containing retry processing statistics
     */
    public Map<String, Object> getRetryMetrics() {
        return Map.of(
                "totalProcessed", totalProcessed.get(),
                "totalSuccess", totalSuccess.get(),
                "totalFailures", totalFailures.get(),
                "successRate", totalProcessed.get() > 0 ?
                    (double) totalSuccess.get() / totalProcessed.get() * 100 : 0.0
        );
    }

    /**
     * Resets retry processing metrics.
     */
    public void resetMetrics() {
        totalProcessed.set(0);
        totalSuccess.set(0);
        totalFailures.set(0);
        log.info("Retry scheduler metrics reset");
    }
}
