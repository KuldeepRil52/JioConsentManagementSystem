package com.jio.digigov.notification.service.event;

import com.jio.digigov.notification.dto.request.event.CreateEventConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.event.EventConfigurationResponseDto;
import com.jio.digigov.notification.entity.event.EventConfiguration;

/**
 * Service interface for Event Configuration management
 */
public interface EventConfigurationService {
    
    /**
     * Creates a new event configuration
     * @param request Event configuration creation request
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param transactionId Transaction identifier for tracking
     * @return Created event configuration response
     */
    EventConfigurationResponseDto createEventConfiguration(CreateEventConfigurationRequestDto request, 
                                                      String tenantId, 
                                                      String businessId, 
                                                      String transactionId);
    
    /**
     * Retrieves all event configurations with filtering and pagination
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param isActive Active status filter (optional)
     * @param priority Priority filter (optional)
     * @param page Page number
     * @param pageSize Page size
     * @param sort Sort field and order
     * @return Paginated event configuration list
     */
    PagedResponseDto<EventConfigurationResponseDto> getAllEventConfigurations(String tenantId, 
                                                                       String businessId, 
                                                                       Boolean isActive,
                                                                       String priority,
                                                                       Integer page, 
                                                                       Integer pageSize, 
                                                                       String sort);
    
    /**
     * Retrieves specific event configuration by business and event type
     * @param businessId Business identifier
     * @param eventType Event type identifier
     * @param tenantId Tenant identifier
     * @return Event configuration details
     */
    EventConfigurationResponseDto getEventConfigurationByEventType(String businessId, 
                                                              String eventType, 
                                                              String tenantId);
    
    /**
     * Updates an existing event configuration
     * @param businessId Business identifier
     * @param eventType Event type identifier
     * @param request Update request
     * @param tenantId Tenant identifier
     * @param transactionId Transaction identifier
     * @return Updated event configuration response
     */
    EventConfigurationResponseDto updateEventConfiguration(String businessId,
                                                      String eventType,
                                                      CreateEventConfigurationRequestDto request, 
                                                      String tenantId, 
                                                      String transactionId);
    
    /**
     * Deletes an event configuration (soft delete)
     * @param businessId Business identifier
     * @param eventType Event type identifier
     * @param tenantId Tenant identifier
     * @param transactionId Transaction identifier
     */
    void deleteEventConfiguration(String businessId, 
                                String eventType, 
                                String tenantId, 
                                String transactionId);
    
    /**
     * Gets count of event configurations with breakdown
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param isActive Active status filter (optional)
     * @param priority Priority filter (optional)
     * @return Count response with breakdown
     */
    CountResponseDto getEventConfigurationCount(String tenantId, 
                                           String businessId, 
                                           Boolean isActive,
                                           String priority);
    
    /**
     * Validates event configuration exists and is active for triggering
     * @param businessId Business identifier
     * @param eventType Event type identifier
     * @param tenantId Tenant identifier
     * @return Active event configuration for processing
     * @throws ValidationException if event configuration is not found or inactive
     */
    EventConfiguration validateEventConfiguration(String businessId, 
                                                String eventType, 
                                                String tenantId);
}