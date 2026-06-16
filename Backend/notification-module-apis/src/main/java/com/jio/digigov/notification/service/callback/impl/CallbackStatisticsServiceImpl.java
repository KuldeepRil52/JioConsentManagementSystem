package com.jio.digigov.notification.service.callback.impl;

import com.jio.digigov.notification.dto.request.notification.CallbackStatsFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.CallbackStatsResponseDto;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.service.callback.CallbackStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation of CallbackStatisticsService with comprehensive business logic.
 *
 * Handles multi-tenant callback statistics retrieval with validation, error handling,
 * and proper tenant context management. Provides comprehensive filtering and
 * aggregation capabilities for callback notifications.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackStatisticsServiceImpl implements CallbackStatisticsService {

    private final NotificationCallbackRepository callbackRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public CallbackStatsResponseDto getCallbackStatistics(String tenantId, CallbackStatsFilterRequestDto filter) {
        log.info("Getting callback statistics for tenant: {}, filter: {}", tenantId, filter);

        // Validate input parameters
        validateTenantId(tenantId);
        validateAndNormalizeFilter(filter);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder
            // The repository will use it to query the tenant-specific database

            CallbackStatsResponseDto stats = callbackRepository.getCallbackStatistics(filter, mongoTemplate);

            log.info("Successfully retrieved callback statistics for tenant: {}, " +
                    "totalCallbacks: {}, DFs: {}, DPs: {}",
                    tenantId,
                    stats.getStats() != null ? stats.getStats().getTotalCallbacks() : 0,
                    stats.getDataFiduciary() != null ? stats.getDataFiduciary().size() : 0,
                    stats.getDataProcessor() != null ? stats.getDataProcessor().size() : 0);

            return stats;

        } catch (IllegalArgumentException e) {
            log.error("Invalid filter parameters for tenant: {}, error: {}", tenantId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting callback statistics for tenant: {}, filter: {}, error: {}",
                    tenantId, filter, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve callback statistics: " + e.getMessage(), e);
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
    private void validateAndNormalizeFilter(CallbackStatsFilterRequestDto filter) {
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
