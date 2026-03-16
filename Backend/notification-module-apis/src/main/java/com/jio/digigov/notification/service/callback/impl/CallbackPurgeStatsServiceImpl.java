package com.jio.digigov.notification.service.callback.impl;

import com.jio.digigov.notification.config.SlaConfiguration;
import com.jio.digigov.notification.dto.request.notification.CallbackPurgeStatsFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.CallbackPurgeStatsResponseDto;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.service.callback.CallbackPurgeStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation of CallbackPurgeStatsService with comprehensive business logic.
 *
 * Handles multi-tenant callback purge statistics retrieval with validation, error handling,
 * and proper tenant context management. Provides comprehensive filtering and aggregation
 * capabilities for purge statistics of CONSENT_EXPIRED and CONSENT_WITHDRAWN events.
 *
 * SLA Management:
 * - Loads SLA threshold from configuration (default: 24 hours)
 * - Categorizes callbacks as purged, pending, or overdue based on SLA compliance
 * - Purged: ACKNOWLEDGED then DELETED in statusHistory
 * - Pending: ACKNOWLEDGED but not DELETED, within SLA
 * - Overdue: ACKNOWLEDGED but not DELETED, exceeded SLA
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackPurgeStatsServiceImpl implements CallbackPurgeStatsService {

    private final NotificationCallbackRepository callbackRepository;
    private final MongoTemplate mongoTemplate;
    private final SlaConfiguration slaConfiguration;

    @Override
    public CallbackPurgeStatsResponseDto getPurgeStatistics(String tenantId, CallbackPurgeStatsFilterRequestDto filter) {
        log.info("Getting callback purge statistics for tenant: {}, filter: {}", tenantId, filter);

        // Validate input parameters
        validateTenantId(tenantId);
        validateAndNormalizeFilter(filter);

        try {
            // Get SLA hours from configuration
            int slaHours = slaConfiguration.getHours();
            log.debug("Using SLA threshold: {} hours", slaHours);

            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder
            // The repository will use it to query the tenant-specific database
            CallbackPurgeStatsResponseDto stats = callbackRepository.getPurgeStatistics(
                    filter,
                    slaHours,
                    mongoTemplate
            );

            log.info("Successfully retrieved callback purge statistics for tenant: {}, " +
                            "totalEvents: {}, purgedEvents: {}, pendingEvents: {}, overdueEvents: {}, " +
                            "DFs: {}, DPs: {}",
                    tenantId,
                    stats.getStats() != null ? stats.getStats().getTotalEvents() : 0,
                    stats.getStats() != null ? stats.getStats().getPurgedEvents() : 0,
                    stats.getStats() != null ? stats.getStats().getPendingEvents() : 0,
                    stats.getStats() != null ? stats.getStats().getOverdueEvents() : 0,
                    stats.getDataFiduciary() != null ? stats.getDataFiduciary().size() : 0,
                    stats.getDataProcessor() != null ? stats.getDataProcessor().size() : 0);

            return stats;

        } catch (IllegalArgumentException e) {
            log.error("Invalid filter parameters for tenant: {}, error: {}", tenantId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting callback purge statistics for tenant: {}, filter: {}, error: {}",
                    tenantId, filter, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve callback purge statistics: " + e.getMessage(), e);
        }
    }

    /**
     * Validates tenant ID.
     *
     * @param tenantId Tenant identifier
     * @throws IllegalArgumentException if tenant ID is null or empty
     */
    private void validateTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            log.error("Tenant ID is null or empty");
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }

        // Additional validation for tenant ID format if needed
        if (tenantId.length() < 3 || tenantId.length() > 50) {
            log.error("Invalid tenant ID length: {}", tenantId.length());
            throw new IllegalArgumentException("Tenant ID must be between 3 and 50 characters");
        }
    }

    /**
     * Validates and normalizes the filter request.
     *
     * @param filter Filter criteria
     * @throws IllegalArgumentException if filter validation fails
     */
    private void validateAndNormalizeFilter(CallbackPurgeStatsFilterRequestDto filter) {
        if (filter == null) {
            log.error("Filter request is null");
            throw new IllegalArgumentException("Filter request cannot be null");
        }

        try {
            // Normalize filter values (trim and uppercase)
            filter.normalize();

            // Validate filter parameters
            filter.validate();

            log.debug("Filter validation passed for filter: {}", filter);

        } catch (IllegalArgumentException e) {
            log.error("Filter validation failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid filter parameters: " + e.getMessage(), e);
        }
    }
}
