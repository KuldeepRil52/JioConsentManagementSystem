package com.jio.digigov.notification.service.event;

import com.jio.digigov.notification.dto.request.event.CreateDataProcessorRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.event.DataProcessorResponseDto;
import com.jio.digigov.notification.entity.event.DataProcessor;

import java.util.List;

/**
 * Service interface for Data Processor management
 */
public interface DataProcessorService {
    
    /**
     * Creates a new data processor
     * @param request Data processor creation request
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param transactionId Transaction identifier for tracking
     * @return Created data processor response
     */
    DataProcessorResponseDto createDataProcessor(CreateDataProcessorRequestDto request, 
                                            String tenantId, 
                                            String businessId, 
                                            String transactionId);
    
    /**
     * Retrieves all data processors with filtering and pagination
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param status Status filter (optional)
     * @param page Page number
     * @param pageSize Page size
     * @param sort Sort field and order
     * @return Paginated data processor list
     */
    PagedResponseDto<DataProcessorResponseDto> getAllDataProcessors(String tenantId, 
                                                             String businessId, 
                                                             String status,
                                                             Integer page, 
                                                             Integer pageSize, 
                                                             String sort);
    
    /**
     * Retrieves specific data processor by ID
     * @param dataProcessorId Data processor identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @return Data processor details
     */
    DataProcessorResponseDto getDataProcessorById(String dataProcessorId,
                                             String tenantId,
                                             String businessId);
    
    /**
     * Updates an existing data processor
     * @param dataProcessorId Data processor identifier
     * @param request Update request
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param transactionId Transaction identifier
     * @return Updated data processor response
     */
    DataProcessorResponseDto updateDataProcessor(String dataProcessorId,
                                            CreateDataProcessorRequestDto request,
                                            String tenantId,
                                            String businessId,
                                            String transactionId);
    
    /**
     * Deletes a data processor
     * @param dataProcessorId Data processor identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param transactionId Transaction identifier
     */
    void deleteDataProcessor(String dataProcessorId,
                           String tenantId,
                           String businessId,
                           String transactionId);
    
    /**
     * Gets count of data processors with breakdown
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param status Status filter (optional)
     * @return Count response with breakdown
     */
    CountResponseDto getDataProcessorCount(String tenantId, 
                                       String businessId, 
                                       String status);
    
    /**
     * Validates multiple data processor IDs exist and are active
     * @param dataProcessorIds List of data processor IDs to validate
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @return List of validated active data processors
     * @throws ValidationException if any DP ID is invalid or inactive
     */
    List<DataProcessor> validateDataProcessorIds(List<String> dataProcessorIds,
                                                String tenantId,
                                                String businessId);
}