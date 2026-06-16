package com.jio.digigov.notification.service.notification;

import com.jio.digigov.notification.dto.request.notification.NotificationFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.notification.NotificationCountResponseDto;
import com.jio.digigov.notification.dto.response.notification.NotificationResponseDto;
import com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto;

import java.util.Optional;

/**
 * Service interface for comprehensive notification operations.
 *
 * Provides business logic for querying notification events and all associated
 * notifications (SMS, Email, Callback) across tenant-specific databases.
 * Handles multi-tenant routing, filtering, and aggregation of notification data.
 */
public interface NotificationService {

    /**
     * Get all notifications with comprehensive filtering and pagination.
     *
     * Returns a flattened list of notifications (SMS, Email, Callback) with a notificationType discriminator.
     * Each notification is a standalone record following REST best practices.
     *
     * @param tenantId Tenant identifier for database routing
     * @param filterRequest Comprehensive filter criteria
     * @return Paged response containing matching unified notifications
     * @throws IllegalArgumentException if tenantId is null or filter validation fails
     */
    PagedResponseDto<UnifiedNotificationDto> getAllNotifications(String tenantId,
                                                      NotificationFilterRequestDto filterRequest);

    /**
     * Get notification count with detailed breakdown.
     *
     * Provides comprehensive statistics about notifications including counts
     * by type, status, event type, and other dimensions based on filter criteria.
     *
     * @param tenantId Tenant identifier for database routing
     * @param filterRequest Filter criteria for counting
     * @return Detailed count response with breakdowns
     * @throws IllegalArgumentException if tenantId is null or filter validation fails
     */
    NotificationCountResponseDto getNotificationCount(String tenantId,
                                                  NotificationFilterRequestDto filterRequest);

    /**
     * Get a single notification by event ID.
     *
     * Retrieves complete notification information including the event and all
     * associated SMS, Email, and Callback notifications.
     *
     * @param tenantId Tenant identifier for database routing
     * @param eventId Event identifier
     * @return Optional containing the complete notification if found
     * @throws IllegalArgumentException if tenantId or eventId is null
     */
    Optional<NotificationResponseDto> getNotificationByEventId(String tenantId, String eventId);

    /**
     * Get notifications for a specific business.
     *
     * Retrieves all notifications belonging to the specified business with
     * basic pagination support.
     *
     * @param tenantId Tenant identifier for database routing
     * @param businessId Business identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paged response containing business notifications
     * @throws IllegalArgumentException if tenantId or businessId is null
     */
    PagedResponseDto<NotificationResponseDto> getNotificationsByBusinessId(String tenantId, String businessId,
                                                               int page, int size);

    /**
     * Get notifications by event type.
     *
     * Retrieves all notifications for the specified event type with
     * pagination support.
     *
     * @param tenantId Tenant identifier for database routing
     * @param eventType Event type identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paged response containing event type notifications
     * @throws IllegalArgumentException if tenantId or eventType is null
     */
    PagedResponseDto<NotificationResponseDto> getNotificationsByEventType(String tenantId, String eventType,
                                                              int page, int size);

    /**
     * Get notifications by status.
     *
     * Retrieves all notifications with the specified status with
     * pagination support.
     *
     * @param tenantId Tenant identifier for database routing
     * @param status Status filter
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paged response containing status-filtered notifications
     * @throws IllegalArgumentException if tenantId or status is null
     */
    PagedResponseDto<NotificationResponseDto> getNotificationsByStatus(String tenantId, String status,
                                                           int page, int size);

    /**
     * Get recent notifications.
     *
     * Retrieves the most recently created notifications up to the specified limit.
     *
     * @param tenantId Tenant identifier for database routing
     * @param limit Maximum number of notifications to return
     * @return List of recent notifications
     * @throws IllegalArgumentException if tenantId is null or limit is invalid
     */
    PagedResponseDto<NotificationResponseDto> getRecentNotifications(String tenantId, int limit);

    /**
     * Get notifications with failed deliveries.
     *
     * Retrieves notifications that have at least one failed notification
     * across SMS, Email, or Callback types.
     *
     * @param tenantId Tenant identifier for database routing
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paged response containing notifications with failures
     * @throws IllegalArgumentException if tenantId is null
     */
    PagedResponseDto<NotificationResponseDto> getNotificationsWithFailures(String tenantId, int page, int size);

    /**
     * Get notifications with pending deliveries.
     *
     * Retrieves notifications that have at least one pending notification
     * across SMS, Email, or Callback types.
     *
     * @param tenantId Tenant identifier for database routing
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paged response containing notifications with pending deliveries
     * @throws IllegalArgumentException if tenantId is null
     */
    PagedResponseDto<NotificationResponseDto> getNotificationsWithPendingDelivery(String tenantId, int page, int size);

    /**
     * Get notification statistics summary.
     *
     * Provides high-level statistics about all notifications without detailed
     * filtering. Useful for dashboard and monitoring purposes.
     *
     * @param tenantId Tenant identifier for database routing
     * @return Basic notification statistics
     * @throws IllegalArgumentException if tenantId is null
     */
    NotificationCountResponseDto getNotificationStatistics(String tenantId);

    /**
     * Search notifications by customer identifier.
     *
     * Finds all notifications sent to a specific customer using their mobile
     * number or email address.
     *
     * @param tenantId Tenant identifier for database routing
     * @param identifierType Type of identifier (MOBILE or EMAIL)
     * @param identifierValue Value of the identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paged response containing customer notifications
     * @throws IllegalArgumentException if parameters are invalid
     */
    PagedResponseDto<UnifiedNotificationDto> searchNotificationsByCustomer(String tenantId,
                                                                String identifierType,
                                                                String identifierValue,
                                                                int page, int size);

    /**
     * Get notifications for Data Processor.
     *
     * Retrieves callback notifications sent to a specific Data Processor.
     *
     * @param tenantId Tenant identifier for database routing
     * @param dataProcessorId Data Processor identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paged response containing Data Processor notifications
     * @throws IllegalArgumentException if parameters are invalid
     */
    PagedResponseDto<UnifiedNotificationDto> getNotificationsForDataProcessor(String tenantId,
                                                                   String dataProcessorId,
                                                                   int page, int size);

    /**
     * Validate filter request parameters.
     *
     * Performs comprehensive validation of filter request parameters to ensure
     * they are valid and consistent.
     *
     * @param filterRequest Filter request to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateFilterRequest(NotificationFilterRequestDto filterRequest);

    /**
     * Get notification export data.
     *
     * Retrieves notification data suitable for export/reporting purposes
     * without pagination limits (use with caution).
     *
     * @param tenantId Tenant identifier for database routing
     * @param filterRequest Filter criteria for export
     * @param maxRecords Maximum number of records to export
     * @return List of notifications for export
     * @throws IllegalArgumentException if parameters are invalid
     * @throws UnsupportedOperationException if export size exceeds limits
     */
    PagedResponseDto<UnifiedNotificationDto> getNotificationExportData(String tenantId,
                                                            NotificationFilterRequestDto filterRequest,
                                                            int maxRecords);
}