package com.jio.digigov.notification.repository.notification;

import com.jio.digigov.notification.entity.notification.NotificationSms;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SMS Notification operations.
 *
 * Provides data access methods for NotificationSms entities with support for
 * multi-tenant database operations, status tracking, and query optimization.
 * All operations are tenant-aware through MongoTemplate parameter injection.
 *
 * Multi-Tenant Strategy:
 * - Uses injected MongoTemplate for tenant-specific database access
 * - No tenant isolation at repository level (handled by service layer)
 * - Supports dynamic database switching for different tenants
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface NotificationSmsRepository {

    /**
     * Saves an SMS notification record to the tenant-specific database.
     *
     * @param notification SMS notification to save
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Saved SMS notification with generated ID
     */
    NotificationSms save(NotificationSms notification, MongoTemplate mongoTemplate);

    /**
     * Finds an SMS notification by its unique notification ID.
     *
     * @param notificationId Unique notification identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the notification if found
     */
    Optional<NotificationSms> findByNotificationId(String notificationId, MongoTemplate mongoTemplate);

    /**
     * Finds all SMS notifications for a specific event.
     *
     * @param eventId Event identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of SMS notifications for the event
     */
    List<NotificationSms> findByEventId(String eventId, MongoTemplate mongoTemplate);

    /**
     * Finds SMS notifications by correlation ID.
     *
     * @param correlationId Correlation identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of related SMS notifications
     */
    List<NotificationSms> findByCorrelationId(String correlationId, MongoTemplate mongoTemplate);

    /**
     * Finds SMS notifications by status.
     *
     * @param status Notification status
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notifications with the specified status
     */
    List<NotificationSms> findByStatus(String status, MongoTemplate mongoTemplate);

    /**
     * Finds SMS notifications by status with pagination.
     *
     * @param status Notification status
     * @param limit Maximum number of results
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notifications with the specified status
     */
    List<NotificationSms> findByStatusWithLimit(String status, int limit, MongoTemplate mongoTemplate);

    /**
     * Finds SMS notifications ready for retry.
     *
     * @param currentTime Current timestamp for retry scheduling
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notifications ready for retry
     */
    List<NotificationSms> findRetryReady(LocalDateTime currentTime, MongoTemplate mongoTemplate);

    /**
     * Updates the status of an SMS notification.
     *
     * @param notificationId Notification identifier
     * @param status New status
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of documents updated
     */
    long updateStatus(String notificationId, String status, MongoTemplate mongoTemplate);

    /**
     * Updates the status and processing timestamp.
     *
     * @param notificationId Notification identifier
     * @param status New status
     * @param processedAt Processing timestamp
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of documents updated
     */
    long updateStatusAndProcessedAt(String notificationId, String status,
                                   LocalDateTime processedAt, MongoTemplate mongoTemplate);

    /**
     * Updates retry information after failed attempt.
     *
     * @param notificationId Notification identifier
     * @param attemptCount New attempt count
     * @param nextRetryAt Next retry timestamp
     * @param errorMessage Error message from failed attempt
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of documents updated
     */
    long updateRetryInfo(String notificationId, int attemptCount, LocalDateTime nextRetryAt,
                        String errorMessage, MongoTemplate mongoTemplate);

    /**
     * Updates delivery information after successful sending.
     *
     * @param notificationId Notification identifier
     * @param status New status
     * @param sentAt Sent timestamp
     * @param digiGovResponse DigiGov API response
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of documents updated
     */
    long updateDeliveryInfo(String notificationId, String status, LocalDateTime sentAt,
                           Object digiGovResponse, MongoTemplate mongoTemplate);

    /**
     * Counts SMS notifications by status.
     *
     * @param status Notification status
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count of notifications with the status
     */
    long countByStatus(String status, MongoTemplate mongoTemplate);

    /**
     * Counts SMS notifications by business ID and status.
     *
     * @param businessId Business identifier
     * @param status Notification status
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count of notifications for the business with the status
     */
    long countByBusinessIdAndStatus(String businessId, String status, MongoTemplate mongoTemplate);

    /**
     * Finds SMS notifications that have exceeded maximum retry attempts.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of permanently failed notifications
     */
    List<NotificationSms> findPermanentlyFailed(MongoTemplate mongoTemplate);

    /**
     * Finds SMS notifications by mobile number and date range.
     *
     * @param mobile Mobile number
     * @param fromDate Start date
     * @param toDate End date
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of SMS notifications for the mobile in the date range
     */
    List<NotificationSms> findByMobileAndDateRange(String mobile, LocalDateTime fromDate,
                                                  LocalDateTime toDate, MongoTemplate mongoTemplate);

    /**
     * Deletes old SMS notification records based on retention policy.
     *
     * @param cutoffDate Date before which records should be deleted
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of records deleted
     */
    long deleteOldRecords(LocalDateTime cutoffDate, MongoTemplate mongoTemplate);
}