package com.jio.digigov.notification.service.callback.impl;

import com.jio.digigov.notification.dto.request.notification.ConsentDeletionFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDashboardResponseDto;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.service.callback.ConsentDeletionDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation of ConsentDeletionDashboardService.
 *
 * Provides consent deletion dashboard data retrieval with comprehensive validation,
 * error handling, and proper tenant context management. Returns overview metrics
 * and paginated list of consent deletion requests for CONSENT_EXPIRED and
 * CONSENT_WITHDRAWN events.
 *
 * Business Logic:
 * - Groups data by unique consentId
 * - Shows latest event when multiple events exist for same consent
 * - Calculates DF status from notification_callback statusHistory
 * - Counts DP deletions for "X/Y Done" format
 * - Determines overall status: Deferred > Partial > Done
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentDeletionDashboardServiceImpl implements ConsentDeletionDashboardService {

    private final NotificationCallbackRepository callbackRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ConsentDeletionDashboardResponseDto getConsentDeletionDashboard(
            String tenantId,
            String businessId,
            ConsentDeletionFilterRequestDto filter) {

        log.info("Getting consent deletion dashboard for tenant: {}, businessId: {}, filter: {}",
                tenantId, businessId, filter);

        // Validate input parameters
        validateTenantId(tenantId);
        validateBusinessId(businessId);
        validateAndNormalizeFilter(filter);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder
            // The repository will use it to query the tenant-specific database
            ConsentDeletionDashboardResponseDto dashboard = callbackRepository.getConsentDeletionDashboard(
                    businessId,
                    filter,
                    mongoTemplate
            );

            log.info("Successfully retrieved consent deletion dashboard for tenant: {}, businessId: {}, " +
                            "deletionRequests: {}, completed: {}, deferred: {}, inProgress: {}, listSize: {}",
                    tenantId,
                    businessId,
                    dashboard.getOverview() != null ? dashboard.getOverview().getDeletionRequests() : 0,
                    dashboard.getOverview() != null ? dashboard.getOverview().getCompleted() : 0,
                    dashboard.getOverview() != null ? dashboard.getOverview().getDeferred() : 0,
                    dashboard.getOverview() != null ? dashboard.getOverview().getInProgress() : 0,
                    dashboard.getDeletionRequests() != null && dashboard.getDeletionRequests().getData() != null
                            ? dashboard.getDeletionRequests().getData().size() : 0);

            return dashboard;

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for tenant: {}, businessId: {}, error: {}",
                    tenantId, businessId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting consent deletion dashboard for tenant: {}, businessId: {}, filter: {}, error: {}",
                    tenantId, businessId, filter, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve consent deletion dashboard: " + e.getMessage(), e);
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

        // Additional validation for tenant ID format
        if (tenantId.length() < 3 || tenantId.length() > 50) {
            log.error("Invalid tenant ID length: {}", tenantId.length());
            throw new IllegalArgumentException("Tenant ID must be between 3 and 50 characters");
        }
    }

    /**
     * Validates business ID.
     *
     * @param businessId Business identifier
     * @throws IllegalArgumentException if business ID is null or empty
     */
    private void validateBusinessId(String businessId) {
        if (!StringUtils.hasText(businessId)) {
            log.error("Business ID is null or empty");
            throw new IllegalArgumentException("Business ID cannot be null or empty");
        }

        // Additional validation for business ID format
        if (businessId.length() < 3 || businessId.length() > 100) {
            log.error("Invalid business ID length: {}", businessId.length());
            throw new IllegalArgumentException("Business ID must be between 3 and 100 characters");
        }
    }

    /**
     * Validates and normalizes the filter request.
     *
     * @param filter Filter criteria
     * @throws IllegalArgumentException if filter validation fails
     */
    private void validateAndNormalizeFilter(ConsentDeletionFilterRequestDto filter) {
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
