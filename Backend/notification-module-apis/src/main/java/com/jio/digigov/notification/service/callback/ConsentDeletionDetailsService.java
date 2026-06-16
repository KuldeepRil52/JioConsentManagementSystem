package com.jio.digigov.notification.service.callback;

import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDetailsResponseDto;

/**
 * Service interface for retrieving detailed consent deletion information.
 * Aggregates data from multiple collections to provide comprehensive deletion details.
 */
public interface ConsentDeletionDetailsService {

    /**
     * Retrieves detailed consent deletion information for a specific event.
     *
     * @param eventId    Event identifier
     * @param tenantId   Tenant identifier for database routing
     * @param businessId Business identifier for data filtering
     * @return ConsentDeletionDetailsResponseDto containing all consent deletion details
     * @throws com.jio.digigov.notification.exception.ResourceNotFoundException if event is not found
     * @throws IllegalArgumentException if parameters are invalid
     */
    ConsentDeletionDetailsResponseDto getConsentDeletionDetails(
            String eventId,
            String tenantId,
            String businessId);
}
