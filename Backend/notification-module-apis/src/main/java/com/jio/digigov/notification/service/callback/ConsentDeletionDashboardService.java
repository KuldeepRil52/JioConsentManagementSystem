package com.jio.digigov.notification.service.callback;

import com.jio.digigov.notification.dto.request.notification.ConsentDeletionFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDashboardResponseDto;

/**
 * Service interface for Consent Deletion Dashboard operations.
 *
 * Provides methods for retrieving consent deletion statistics and lists
 * for CONSENT_EXPIRED and CONSENT_WITHDRAWN events.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public interface ConsentDeletionDashboardService {

    /**
     * Retrieves the consent deletion dashboard with overview metrics and paginated list.
     *
     * Returns comprehensive consent deletion data including:
     * - Overview metrics (total deletion requests, completed, deferred, in-progress)
     * - Paginated list of consent deletion items with status breakdown
     *
     * The dashboard groups data by unique consentId, showing the latest event
     * for each consent when multiple events exist.
     *
     * Overview Metrics:
     * - deletionRequests: Count of unique consentIds for CONSENT_EXPIRED/CONSENT_WITHDRAWN events
     * - completed: Consents where ALL recipients (DF + all DPs) have DELETED status
     * - deferred: Consents where ANY recipient has DEFERRED status
     * - inProgress: deletionRequests - completed - deferred
     *
     * List Item Fields:
     * - consentId: From event_payload.consentId
     * - dataPrincipal: From customer_identifiers.value
     * - trigger: Event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN)
     * - dfStatus: DF callback status (DELETED, DEFERRED, PENDING, or NOT_APPLICABLE)
     * - processors: "X/Y Done" format showing DP completion
     * - overall: Done, Partial, or Deferred (priority: Deferred > Partial > Done)
     *
     * @param tenantId Tenant identifier for database routing
     * @param businessId Business identifier for filtering events
     * @param filter Filter criteria (eventType, overallStatus, processorId, consentId, dataPrincipal, dateRange, pagination)
     * @return ConsentDeletionDashboardResponseDto containing overview metrics and paginated list
     * @throws IllegalArgumentException if tenantId or businessId is invalid
     * @throws RuntimeException if database operation fails
     */
    ConsentDeletionDashboardResponseDto getConsentDeletionDashboard(
            String tenantId,
            String businessId,
            ConsentDeletionFilterRequestDto filter);
}
