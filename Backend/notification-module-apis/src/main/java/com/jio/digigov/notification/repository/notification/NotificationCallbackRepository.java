package com.jio.digigov.notification.repository.notification;

import com.jio.digigov.notification.dto.request.notification.CallbackPurgeStatsFilterRequestDto;
import com.jio.digigov.notification.dto.request.notification.CallbackStatsFilterRequestDto;
import com.jio.digigov.notification.dto.request.notification.ConsentDeletionFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.CallbackPurgeStatsResponseDto;
import com.jio.digigov.notification.dto.response.notification.CallbackStatsResponseDto;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDashboardResponseDto;
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

    /**
     * Retrieves comprehensive callback notification statistics with breakdown by Data Fiduciary and Data Processor.
     *
     * Uses MongoDB aggregation pipeline to efficiently compute statistics including:
     * - Global statistics (total, success, failure counts and percentages)
     * - Data Fiduciary breakdown with names from business_applications collection
     * - Data Processor breakdown with names from data_processors collection
     * - Event-type-specific statistics at each level
     *
     * The aggregation uses $lookup to join recipient names and $facet for parallel processing
     * of global, DF, and DP statistics in a single query.
     *
     * @param filter Filter criteria (eventType, recipientType, recipientId, fromDate, toDate)
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return CallbackStatsResponseDto containing comprehensive statistics
     */
    CallbackStatsResponseDto getCallbackStatistics(CallbackStatsFilterRequestDto filter, MongoTemplate mongoTemplate);

    /**
     * Retrieves callback purge statistics for CONSENT_EXPIRED and CONSENT_WITHDRAWN events.
     *
     * Analyzes statusHistory to categorize callbacks into:
     * - Purged: Callbacks that transitioned from ACKNOWLEDGED to DELETED in statusHistory
     * - Pending: Callbacks that are ACKNOWLEDGED but not DELETED (within SLA)
     * - Overdue: Callbacks that are ACKNOWLEDGED but not DELETED and exceeded SLA time
     *
     * Uses MongoDB aggregation pipeline to efficiently compute statistics including:
     * - Global purge statistics (total, purged, pending, overdue counts and percentages)
     * - Data Fiduciary breakdown with names and complete data from business_applications collection
     * - Data Processor breakdown with names and complete data from data_processors collection
     * - Event-type-specific statistics at each level
     *
     * The aggregation uses:
     * - $lookup to join recipient names and complete recipient data
     * - $facet for parallel processing of global, DF, and DP statistics
     * - $filter to extract ACKNOWLEDGED and DELETED timestamps from statusHistory
     * - Conditional logic to determine purge status based on SLA threshold
     *
     * @param filter Filter criteria (eventType, recipientType, recipientId, fromDate, toDate)
     * @param slaHours SLA threshold in hours for determining overdue callbacks
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return CallbackPurgeStatsResponseDto containing comprehensive purge statistics
     */
    CallbackPurgeStatsResponseDto getPurgeStatistics(CallbackPurgeStatsFilterRequestDto filter, int slaHours, MongoTemplate mongoTemplate);

    /**
     * Retrieves consent deletion dashboard data with overview metrics and paginated list.
     *
     * This method provides a comprehensive view of consent deletion requests,
     * grouped by consentId, for CONSENT_EXPIRED and CONSENT_WITHDRAWN events.
     *
     * Uses MongoDB aggregation pipeline to:
     * 1. Join notification_events with notification_callback by eventId
     * 2. Filter by CONSENT_EXPIRED and CONSENT_WITHDRAWN event types
     * 3. Group by consentId, taking the latest event for each consent
     * 4. Calculate DF status, DP deletion counts, and overall status
     * 5. Compute overview metrics (total, completed, deferred, in-progress)
     * 6. Return paginated list of consent deletion items
     *
     * Overview Metrics:
     * - deletionRequests: Count of unique consentIds
     * - completed: Consents where ALL recipients (DF + all DPs) have DELETED status
     * - deferred: Consents where ANY recipient has DEFERRED status
     * - inProgress: deletionRequests - completed - deferred
     *
     * List Item Fields:
     * - consentId: From event_payload.consentId
     * - dataPrincipal: From customer_identifiers.value
     * - trigger: Event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN)
     * - dfStatus: DF callback status (DELETED, DEFERRED, or PENDING)
     * - processors: "X/Y Done" format showing DP completion
     * - overall: Done, Partial, or Deferred (priority: Deferred > Partial > Done)
     *
     * @param businessId Business ID for filtering events
     * @param filter Filter criteria (eventType, overallStatus, processorId, consentId, dataPrincipal, dateRange, pagination)
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return ConsentDeletionDashboardResponseDto containing overview metrics and paginated list
     */
    ConsentDeletionDashboardResponseDto getConsentDeletionDashboard(
            String businessId,
            ConsentDeletionFilterRequestDto filter,
            MongoTemplate mongoTemplate);
}