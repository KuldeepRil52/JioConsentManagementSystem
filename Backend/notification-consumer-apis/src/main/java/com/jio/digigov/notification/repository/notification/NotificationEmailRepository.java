package com.jio.digigov.notification.repository.notification;

import com.jio.digigov.notification.entity.notification.NotificationEmail;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Email Notification operations.
 *
 * Provides data access methods for NotificationEmail entities with support for
 * multi-tenant database operations, status tracking, and query optimization.
 * All operations are tenant-aware through MongoTemplate parameter injection.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface NotificationEmailRepository {

    NotificationEmail save(NotificationEmail notification, MongoTemplate mongoTemplate);

    Optional<NotificationEmail> findByNotificationId(String notificationId, MongoTemplate mongoTemplate);

    List<NotificationEmail> findByEventId(String eventId, MongoTemplate mongoTemplate);

    List<NotificationEmail> findByCorrelationId(String correlationId, MongoTemplate mongoTemplate);

    List<NotificationEmail> findByStatus(String status, MongoTemplate mongoTemplate);

    List<NotificationEmail> findByStatusWithLimit(String status, int limit, MongoTemplate mongoTemplate);

    List<NotificationEmail> findRetryReady(LocalDateTime currentTime, MongoTemplate mongoTemplate);

    long updateStatus(String notificationId, String status, MongoTemplate mongoTemplate);

    long updateStatusAndProcessedAt(String notificationId, String status,
                                   LocalDateTime processedAt, MongoTemplate mongoTemplate);

    long updateRetryInfo(String notificationId, int attemptCount, LocalDateTime nextRetryAt,
                        String errorMessage, MongoTemplate mongoTemplate);

    long updateDeliveryInfo(String notificationId, String status, LocalDateTime sentAt,
                           Object digiGovResponse, MongoTemplate mongoTemplate);

    long countByStatus(String status, MongoTemplate mongoTemplate);

    long countByBusinessIdAndStatus(String businessId, String status, MongoTemplate mongoTemplate);

    List<NotificationEmail> findPermanentlyFailed(MongoTemplate mongoTemplate);

    long deleteOldRecords(LocalDateTime cutoffDate, MongoTemplate mongoTemplate);
}