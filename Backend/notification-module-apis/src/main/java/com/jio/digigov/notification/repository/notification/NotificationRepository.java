package com.jio.digigov.notification.repository.notification;

import com.jio.digigov.notification.dto.request.notification.NotificationFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.NotificationCountResponseDto;
import com.jio.digigov.notification.dto.response.notification.NotificationResponseDto;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for cross-collection notification queries.
 *
 * Provides data access methods that aggregate information from notification_events
 * and all associated notification collections (SMS, Email, Callback) to provide
 * comprehensive notification views with support for multi-tenant operations.
 *
 * Uses MongoDB aggregation pipelines to efficiently join and filter data across
 * multiple collections while maintaining tenant isolation through MongoTemplate
 * parameter injection.
 */
public interface NotificationRepository {

    /**
     * Find notifications with comprehensive filtering and pagination.
     *
     * Aggregates data from notification_events and all notification types
     * based on the provided filter criteria.
     *
     * @param filterRequest Filter criteria and pagination parameters
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of aggregated notification DTOs
     */
    List<NotificationResponseDto> findNotificationsWithFilter(NotificationFilterRequestDto filterRequest,
                                                      MongoTemplate mongoTemplate);

    /**
     * Count notifications with comprehensive filtering.
     *
     * Provides detailed count breakdown by type, status, and other dimensions
     * based on the provided filter criteria.
     *
     * @param filterRequest Filter criteria for counting
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Detailed count response with breakdowns
     */
    NotificationCountResponseDto countNotificationsWithFilter(NotificationFilterRequestDto filterRequest,
                                                           MongoTemplate mongoTemplate);

    /**
     * Find a single notification by event ID.
     *
     * Returns complete notification information including all associated
     * SMS, Email, and Callback notifications for the specified event.
     *
     * @param eventId Event identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the complete notification if found
     */
    Optional<NotificationResponseDto> findNotificationByEventId(String eventId, MongoTemplate mongoTemplate);

    /**
     * Find notifications by business ID with pagination.
     *
     * Returns all notifications for a specific business with basic pagination.
     *
     * @param businessId Business identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notification DTOs for the business
     */
    List<NotificationResponseDto> findNotificationsByBusinessId(String businessId, int page, int size,
                                                        MongoTemplate mongoTemplate);

    /**
     * Find notifications by event type with pagination.
     *
     * Returns all notifications for a specific event type with pagination.
     *
     * @param eventType Event type identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notification DTOs for the event type
     */
    List<NotificationResponseDto> findNotificationsByEventType(String eventType, int page, int size,
                                                       MongoTemplate mongoTemplate);

    /**
     * Find notifications by status with pagination.
     *
     * Returns all notifications with the specified status with pagination.
     *
     * @param status Status filter
     * @param page Page number (0-based)
     * @param size Page size
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notification DTOs with the status
     */
    List<NotificationResponseDto> findNotificationsByStatus(String status, int page, int size,
                                                    MongoTemplate mongoTemplate);

    /**
     * Count total notifications for a business.
     *
     * @param businessId Business identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Total count of notifications for the business
     */
    long countNotificationsByBusinessId(String businessId, MongoTemplate mongoTemplate);

    /**
     * Count notifications by event type.
     *
     * @param eventType Event type identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count of notifications for the event type
     */
    long countNotificationsByEventType(String eventType, MongoTemplate mongoTemplate);

    /**
     * Count notifications by status.
     *
     * @param status Status filter
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count of notifications with the status
     */
    long countNotificationsByStatus(String status, MongoTemplate mongoTemplate);

    /**
     * Find recent notifications with limit.
     *
     * Returns the most recently created notifications up to the specified limit.
     *
     * @param limit Maximum number of notifications to return
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of recent notification DTOs
     */
    List<NotificationResponseDto> findRecentNotifications(int limit, MongoTemplate mongoTemplate);

    /**
     * Find notifications with failed status across all types.
     *
     * Returns notifications that have failed delivery across SMS, Email, or Callback types.
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notification DTOs with failed notifications
     */
    List<NotificationResponseDto> findNotificationsWithFailures(int page, int size, MongoTemplate mongoTemplate);

    /**
     * Find notifications with pending status across all types.
     *
     * Returns notifications that have pending delivery across SMS, Email, or Callback types.
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of notification DTOs with pending notifications
     */
    List<NotificationResponseDto> findNotificationsWithPendingDelivery(int page, int size, MongoTemplate mongoTemplate);

    /**
     * Get notification statistics summary.
     *
     * Provides high-level statistics about notifications without detailed breakdowns.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Basic notification count response with summary statistics
     */
    NotificationCountResponseDto getNotificationStatistics(MongoTemplate mongoTemplate);

    /**
     * OPTIMIZED: Find flattened notifications with filtering and pagination at MongoDB level.
     *
     * This method performs all flattening, filtering, and pagination in MongoDB aggregation
     * pipeline, avoiding expensive Java-side processing. Returns UnifiedNotificationDto directly.
     *
     * @param filterRequest Filter criteria and pagination parameters
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of flattened and filtered notification DTOs
     */
    List<com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto> findFlattenedNotificationsWithFilter(
        NotificationFilterRequestDto filterRequest, MongoTemplate mongoTemplate);

    /**
     * OPTIMIZED: Count flattened notifications with filtering at MongoDB level.
     *
     * Counts individual notifications (not events) after flattening and filtering.
     *
     * @param filterRequest Filter criteria
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Total count of flattened notifications matching the filter
     */
    long countFlattenedNotifications(NotificationFilterRequestDto filterRequest, MongoTemplate mongoTemplate);
}