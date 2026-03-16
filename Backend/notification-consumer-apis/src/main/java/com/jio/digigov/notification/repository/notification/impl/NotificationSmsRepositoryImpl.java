package com.jio.digigov.notification.repository.notification.impl;

import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.repository.notification.NotificationSmsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of NotificationSmsRepository for SMS notification data access.
 *
 * Provides optimized MongoDB operations for SMS notifications with comprehensive
 * query support, status tracking, and retry management. All operations are
 * tenant-aware through MongoTemplate parameter injection.
 *
 * Performance Optimizations:
 * - Uses compound indexes for efficient queries
 * - Batch operations for bulk updates
 * - Projection queries for count operations
 * - Optimized sort and limit for large datasets
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
@Slf4j
public class NotificationSmsRepositoryImpl implements NotificationSmsRepository {

    @Override
    public NotificationSms save(NotificationSms notification, MongoTemplate mongoTemplate) {
        try {
            // Manually set timestamps (workaround for multi-tenant auditing)
            LocalDateTime now = LocalDateTime.now();
            if (notification.getCreatedAt() == null) {
                notification.setCreatedAt(now);
            }
            notification.setUpdatedAt(now); // Always update the updatedAt timestamp

            return mongoTemplate.save(notification);
        } catch (Exception e) {
            log.error("Error saving SMS notification: {}", e.getMessage());
            throw new RuntimeException("Failed to save SMS notification", e);
        }
    }

    @Override
    public Optional<NotificationSms> findByNotificationId(String notificationId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            NotificationSms notification = mongoTemplate.findOne(query, NotificationSms.class);
            return Optional.ofNullable(notification);
        } catch (Exception e) {
            log.error("Error finding SMS notification by ID {}: {}", notificationId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<NotificationSms> findByEventId(String eventId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("eventId").is(eventId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error finding SMS notifications by event ID {}: {}", eventId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationSms> findByCorrelationId(String correlationId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("correlationId").is(correlationId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error finding SMS notifications by correlation ID {}: {}", correlationId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationSms> findByStatus(String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status))
                    .with(Sort.by(Sort.Direction.ASC, "createdAt"));
            return mongoTemplate.find(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error finding SMS notifications by status {}: {}", status, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationSms> findByStatusWithLimit(String status, int limit, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status))
                    .with(Sort.by(Sort.Direction.ASC, "createdAt"))
                    .limit(limit);
            return mongoTemplate.find(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error finding SMS notifications by status {} with limit {}: {}",
                     status, limit, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<NotificationSms> findRetryReady(LocalDateTime currentTime, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").in(NotificationStatus.FAILED.name(), "RETRY_PENDING"),
                Criteria.where("nextRetryAt").lte(currentTime),
                Criteria.where("attemptCount").lt(3) // Assuming max 3 attempts
            );

            Query query = new Query(criteria)
                    .with(Sort.by(Sort.Direction.ASC, "nextRetryAt"))
                    .limit(100); // Limit for batch processing

            return mongoTemplate.find(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error finding retry-ready SMS notifications: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long updateStatus(String notificationId, String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationSms.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating SMS notification status for ID {}: {}", notificationId, e.getMessage());
            return 0;
        }
    }

    @Override
    public long updateStatusAndProcessedAt(String notificationId, String status,
                                          LocalDateTime processedAt, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("processedAt", processedAt)
                    .set("lastAttemptAt", LocalDateTime.now())
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationSms.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating SMS notification status and processedAt for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long updateRetryInfo(String notificationId, int attemptCount, LocalDateTime nextRetryAt,
                               String errorMessage, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("attemptCount", attemptCount)
                    .set("nextRetryAt", nextRetryAt)
                    .set("lastErrorMessage", errorMessage)
                    .set("lastAttemptAt", LocalDateTime.now())
                    .set("status", "RETRY_PENDING")
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationSms.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating SMS notification retry info for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long updateDeliveryInfo(String notificationId, String status, LocalDateTime sentAt,
                                  Object digiGovResponse, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("sentAt", sentAt)
                    .set("digiGovResponse", digiGovResponse)
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationSms.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating SMS notification delivery info for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByStatus(String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status));
            return mongoTemplate.count(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error counting SMS notifications by status {}: {}", status, e.getMessage());
            return 0;
        }
    }

    @Override
    public long countByBusinessIdAndStatus(String businessId, String status, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("businessId").is(businessId),
                Criteria.where("status").is(status)
            );
            Query query = new Query(criteria);
            return mongoTemplate.count(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error counting SMS notifications by business {} and status {}: {}",
                     businessId, status, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<NotificationSms> findPermanentlyFailed(MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").is(NotificationStatus.FAILED.name()),
                Criteria.where("attemptCount").gte(3) // Exceeded max attempts
            );

            Query query = new Query(criteria)
                    .with(Sort.by(Sort.Direction.DESC, "lastAttemptAt"));

            return mongoTemplate.find(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error finding permanently failed SMS notifications: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationSms> findByMobileAndDateRange(String mobile, LocalDateTime fromDate,
                                                         LocalDateTime toDate, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("mobile").is(mobile),
                Criteria.where("createdAt").gte(fromDate).lte(toDate)
            );

            Query query = new Query(criteria)
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));

            return mongoTemplate.find(query, NotificationSms.class);
        } catch (Exception e) {
            log.error("Error finding SMS notifications by mobile {} and date range: {}",
                     mobile, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public long deleteOldRecords(LocalDateTime cutoffDate, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("createdAt").lt(cutoffDate),
                Criteria.where("status").in(NotificationStatus.DELIVERED.name(), NotificationStatus.FAILED.name()) // Only delete completed records
            );

            Query query = new Query(criteria);
            return mongoTemplate.remove(query, NotificationSms.class).getDeletedCount();
        } catch (Exception e) {
            log.error("Error deleting old SMS notification records: {}", e.getMessage());
            return 0;
        }
    }
}