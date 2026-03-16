package com.jio.digigov.notification.repository.notification;

import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.StatusHistoryEntry;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Callback Notification operations.
 *
 * Provides data access methods for NotificationCallback entities with support for
 * multi-tenant database operations, webhook tracking, and query optimization.
 * All operations are tenant-aware through MongoTemplate parameter injection.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface NotificationCallbackRepository {

    NotificationCallback save(NotificationCallback notification, MongoTemplate mongoTemplate);

    Optional<NotificationCallback> findByNotificationId(String notificationId, MongoTemplate mongoTemplate);

    List<NotificationCallback> findByEventId(String eventId, MongoTemplate mongoTemplate);

    List<NotificationCallback> findByCorrelationId(String correlationId, MongoTemplate mongoTemplate);

    List<NotificationCallback> findByStatus(String status, MongoTemplate mongoTemplate);

    List<NotificationCallback> findByStatusWithLimit(String status, int limit, MongoTemplate mongoTemplate);

    List<NotificationCallback> findByRecipientId(String recipientId, MongoTemplate mongoTemplate);

    List<NotificationCallback> findRetryReady(LocalDateTime currentTime, MongoTemplate mongoTemplate);

    long updateStatus(String notificationId, String status, MongoTemplate mongoTemplate);

    long updateStatusAndProcessedAt(String notificationId, String status,
                                   LocalDateTime processedAt, MongoTemplate mongoTemplate);

    long updateRetryInfo(String notificationId, int attemptCount, LocalDateTime nextRetryAt,
                        String errorMessage, MongoTemplate mongoTemplate);

    long updateWebhookResponse(String notificationId, String status, LocalDateTime acknowledgedAt,
                              String webhookResponse, Integer httpStatusCode, MongoTemplate mongoTemplate);

    long countByStatus(String status, MongoTemplate mongoTemplate);

    long countByBusinessIdAndStatus(String businessId, String status, MongoTemplate mongoTemplate);

    List<NotificationCallback> findPermanentlyFailed(MongoTemplate mongoTemplate);

    long deleteOldRecords(LocalDateTime cutoffDate, MongoTemplate mongoTemplate);

    /**
     * Adds a status history entry to the notification's statusHistory array.
     *
     * @param notificationId The notification ID
     * @param entry The status history entry to add
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of documents modified (should be 1 if successful)
     */
    long addStatusHistoryEntry(String notificationId, StatusHistoryEntry entry, MongoTemplate mongoTemplate);

    /**
     * Updates the notification's status field and adds a history entry atomically.
     *
     * @param notificationId The notification ID
     * @param status The new status value
     * @param entry The status history entry to add
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of documents modified (should be 1 if successful)
     */
    long updateStatusWithHistory(String notificationId, String status, StatusHistoryEntry entry, MongoTemplate mongoTemplate);

    /**
     * Finds a notification by ID and validates it belongs to the specified recipient.
     *
     * @param notificationId The notification ID
     * @param recipientType The recipient type (DATA_FIDUCIARY or DATA_PROCESSOR)
     * @param recipientId The recipient ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the notification if found and authorized, empty otherwise
     */
    Optional<NotificationCallback> findByIdAndRecipient(String notificationId, String recipientType,
                                                        String recipientId, MongoTemplate mongoTemplate);
}