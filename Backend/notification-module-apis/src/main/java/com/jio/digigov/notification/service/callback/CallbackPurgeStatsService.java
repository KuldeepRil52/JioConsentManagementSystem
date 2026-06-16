package com.jio.digigov.notification.service.callback;

import com.jio.digigov.notification.dto.request.notification.CallbackPurgeStatsFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.CallbackPurgeStatsResponseDto;

/**
 * Service interface for callback purge statistics operations.
 *
 * Provides business logic for retrieving comprehensive callback purge statistics
 * for CONSENT_EXPIRED and CONSENT_WITHDRAWN events. Analyzes statusHistory to categorize
 * callbacks as purged, pending, or overdue based on SLA compliance.
 *
 * Purge Categories:
 * - Purged: Callbacks that transitioned from ACKNOWLEDGED to DELETED in statusHistory
 * - Pending: Callbacks that are ACKNOWLEDGED but not yet DELETED (within SLA)
 * - Overdue: Callbacks that are ACKNOWLEDGED but not DELETED and exceeded SLA time
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public interface CallbackPurgeStatsService {

    /**
     * Retrieves comprehensive callback purge statistics.
     *
     * Returns statistics including:
     * - Global purge statistics (total, purged, pending, overdue counts and percentages)
     * - Data Fiduciary breakdown with organization names and complete data
     * - Data Processor breakdown with processor names and complete data
     * - Event-type-specific statistics at each level
     *
     * The statistics respect all applied filters and only include callbacks
     * matching the filter criteria. Only CONSENT_EXPIRED and CONSENT_WITHDRAWN
     * events are included in purge statistics.
     *
     * SLA Compliance:
     * - Callbacks are considered overdue if they remain ACKNOWLEDGED (not DELETED)
     *   for longer than the configured SLA threshold (default: 24 hours)
     * - SLA is calculated from the ACKNOWLEDGED timestamp in statusHistory
     *
     * @param tenantId Tenant identifier for database routing
     * @param filter Filter criteria (eventType, recipientType, recipientId, fromDate, toDate)
     * @return CallbackPurgeStatsResponseDto containing comprehensive purge statistics
     * @throws IllegalArgumentException if tenantId is null or filter validation fails
     * @throws RuntimeException if database operation fails
     */
    CallbackPurgeStatsResponseDto getPurgeStatistics(String tenantId, CallbackPurgeStatsFilterRequestDto filter);
}
