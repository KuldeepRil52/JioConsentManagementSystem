package com.jio.digigov.notification.service.callback;

import com.jio.digigov.notification.dto.request.notification.CallbackStatsFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.CallbackStatsResponseDto;

/**
 * Service interface for callback notification statistics operations.
 *
 * Provides business logic for retrieving comprehensive callback notification statistics
 * with breakdowns by Data Fiduciary and Data Processor, including event-type-specific metrics.
 * Handles multi-tenant routing, filter validation, and data aggregation.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public interface CallbackStatisticsService {

    /**
     * Retrieves comprehensive callback notification statistics.
     *
     * Returns statistics including:
     * - Global statistics (total, success, failure counts and percentages)
     * - Data Fiduciary breakdown with organization names
     * - Data Processor breakdown with processor names
     * - Event-type-specific statistics at each level
     *
     * The statistics respect all applied filters and only include callbacks
     * matching the filter criteria.
     *
     * @param tenantId Tenant identifier for database routing
     * @param filter Filter criteria (eventType, recipientType, recipientId, fromDate, toDate)
     * @return CallbackStatsResponseDto containing comprehensive statistics
     * @throws IllegalArgumentException if tenantId is null or filter validation fails
     * @throws RuntimeException if database operation fails
     */
    CallbackStatsResponseDto getCallbackStatistics(String tenantId, CallbackStatsFilterRequestDto filter);
}
