package com.jio.digigov.notification.repository.notification.impl;

import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.StatusHistoryEntry;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
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
 * Implementation of NotificationCallbackRepository for callback notification data access.
 *
 * Provides optimized MongoDB operations for callback notifications with comprehensive
 * query support, webhook tracking, and retry management. All operations are
 * tenant-aware through MongoTemplate parameter injection.
 *
 * Performance Optimizations:
 * - Uses compound indexes for efficient queries
 * - Batch operations for bulk updates
 * - Projection queries for count operations
 * - Optimized sort and limit for large datasets
 * - Special webhook response tracking
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
@Slf4j
public class NotificationCallbackRepositoryImpl implements NotificationCallbackRepository {

    @Override
    public NotificationCallback save(NotificationCallback notification, MongoTemplate mongoTemplate) {
        try {
            // Manually set timestamps (workaround for multi-tenant auditing)
            LocalDateTime now = LocalDateTime.now();
            if (notification.getCreatedAt() == null) {
                notification.setCreatedAt(now);
            }
            notification.setUpdatedAt(now); // Always update the updatedAt timestamp

            return mongoTemplate.save(notification);
        } catch (Exception e) {
            log.error("Error saving callback notification: {}", e.getMessage());
            throw new RuntimeException("Failed to save callback notification", e);
        }
    }

    @Override
    public Optional<NotificationCallback> findByNotificationId(String notificationId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            NotificationCallback notification = mongoTemplate.findOne(query, NotificationCallback.class);
            return Optional.ofNullable(notification);
        } catch (Exception e) {
            log.error("Error finding callback notification by ID {}: {}", notificationId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<NotificationCallback> findByEventId(String eventId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("eventId").is(eventId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by event ID {}: {}", eventId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByCorrelationId(String correlationId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("correlationId").is(correlationId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by correlation ID {}: {}", correlationId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByStatus(String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status))
                    .with(Sort.by(Sort.Direction.ASC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by status {}: {}", status, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByStatusWithLimit(String status, int limit, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status))
                    .with(Sort.by(Sort.Direction.ASC, "createdAt"))
                    .limit(limit);
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by status {} with limit {}: {}",
                     status, limit, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByRecipientId(String recipientId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("recipientId").is(recipientId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by recipient ID {}: {}", recipientId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findRetryReady(LocalDateTime currentTime, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").in("FAILED", "RETRY_PENDING"),
                Criteria.where("nextRetryAt").lte(currentTime),
                Criteria.where("attemptCount").lt(5) // Higher retry count for webhooks
            );

            Query query = new Query(criteria)
                    .with(Sort.by(Sort.Direction.ASC, "nextRetryAt"))
                    .limit(50); // Smaller batch for webhook processing

            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding retry-ready callback notifications: {}", e.getMessage());
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

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification status for ID {}: {}", notificationId, e.getMessage());
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

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification status and processedAt for ID {}: {}",
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

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification retry info for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long updateWebhookResponse(String notificationId, String status, LocalDateTime acknowledgedAt,
                                     String webhookResponse, Integer httpStatusCode, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("acknowledgedAt", acknowledgedAt)
                    .set("webhookResponse", webhookResponse)
                    .set("httpStatusCode", httpStatusCode)
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification webhook response for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByStatus(String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status));
            return mongoTemplate.count(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error counting callback notifications by status {}: {}", status, e.getMessage());
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
            return mongoTemplate.count(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error counting callback notifications by business {} and status {}: {}",
                     businessId, status, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<NotificationCallback> findPermanentlyFailed(MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").is("FAILED"),
                Criteria.where("attemptCount").gte(5) // Higher threshold for webhooks
            );

            Query query = new Query(criteria)
                    .with(Sort.by(Sort.Direction.DESC, "lastAttemptAt"));

            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding permanently failed callback notifications: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long deleteOldRecords(LocalDateTime cutoffDate, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("createdAt").lt(cutoffDate),
                Criteria.where("status").in("ACKNOWLEDGED", "FAILED") // Only delete completed records
            );

            Query query = new Query(criteria);
            return mongoTemplate.remove(query, NotificationCallback.class).getDeletedCount();
        } catch (Exception e) {
            log.error("Error deleting old callback notification records: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long addStatusHistoryEntry(String notificationId, StatusHistoryEntry entry, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update().push("statusHistory", entry);
            update.set("updatedAt", LocalDateTime.now());

            var result = mongoTemplate.updateFirst(query, update, NotificationCallback.class);

            if (result.getModifiedCount() == 0) {
                log.warn("No notification found to add status history entry: notificationId={}", notificationId);
            }

            return result.getModifiedCount();
        } catch (Exception e) {
            log.error("Error adding status history entry for notificationId={}: {}",
                     notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to add status history entry", e);
        }
    }

    @Override
    public long updateStatusWithHistory(String notificationId, String status,
                                       StatusHistoryEntry entry, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                .set("status", status)
                .set("updatedAt", LocalDateTime.now())
                .push("statusHistory", entry);

            // Add acknowledgedAt timestamp if status is ACKNOWLEDGED
            if ("ACKNOWLEDGED".equals(status)) {
                update.set("acknowledgedAt", LocalDateTime.now());
            }

            var result = mongoTemplate.updateFirst(query, update, NotificationCallback.class);

            if (result.getModifiedCount() == 0) {
                log.warn("No notification found to update status with history: notificationId={}, status={}",
                        notificationId, status);
            } else {
                log.debug("Updated notification status with history: notificationId={}, status={}, entry={}",
                         notificationId, status, entry);
            }

            return result.getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating status with history for notificationId={}, status={}: {}",
                     notificationId, status, e.getMessage(), e);
            throw new RuntimeException("Failed to update status with history", e);
        }
    }

    @Override
    public Optional<NotificationCallback> findByIdAndRecipient(String notificationId, String recipientType,
                                                               String recipientId, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("notificationId").is(notificationId),
                Criteria.where("recipientType").is(recipientType),
                Criteria.where("recipientId").is(recipientId)
            );

            Query query = new Query(criteria);
            NotificationCallback notification = mongoTemplate.findOne(query, NotificationCallback.class);

            if (notification == null) {
                log.debug("Notification not found or not authorized: notificationId={}, recipientType={}, recipientId={}",
                         notificationId, recipientType, recipientId);
            }

            return Optional.ofNullable(notification);
        } catch (Exception e) {
            log.error("Error finding notification by ID and recipient: notificationId={}, recipientType={}, recipientId={}, error={}",
                     notificationId, recipientType, recipientId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}