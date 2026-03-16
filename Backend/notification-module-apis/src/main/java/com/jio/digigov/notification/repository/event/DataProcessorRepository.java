package com.jio.digigov.notification.repository.event;

import com.jio.digigov.notification.entity.event.DataProcessor;
import com.jio.digigov.notification.enums.DataProcessorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Data Processor operations using multi-tenant approach
 * No direct MongoRepository extension - uses explicit MongoTemplate per tenant
 */
@Repository
public interface DataProcessorRepository {
    
    /**
     * Find data processor by dataProcessorId and businessId
     * @param dataProcessorId Data processor ID
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional DataProcessor
     */
    Optional<DataProcessor> findByDataProcessorIdAndBusinessId(String dataProcessorId, String businessId, MongoTemplate mongoTemplate);
    
    /**
     * Find all data processors for a business
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of DataProcessors
     */
    List<DataProcessor> findByBusinessId(String businessId, MongoTemplate mongoTemplate);
    
    /**
     * Find data processors by business and status
     * @param businessId Business ID
     * @param status Status to filter by
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of DataProcessors
     */
    List<DataProcessor> findByBusinessIdAndStatus(String businessId, DataProcessorStatus status, MongoTemplate mongoTemplate);
    
    /**
     * Find data processors with pagination
     * @param businessId Business ID
     * @param pageable Pagination parameters
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Page of DataProcessors
     */
    Page<DataProcessor> findByBusinessId(String businessId, Pageable pageable, MongoTemplate mongoTemplate);
    
    /**
     * Save data processor
     * @param dataProcessor DataProcessor to save
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Saved DataProcessor
     */
    DataProcessor save(DataProcessor dataProcessor, MongoTemplate mongoTemplate);
    
    /**
     * Delete data processor by dataProcessorId
     * @param dataProcessorId Data processor ID
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of deleted records
     */
    long deleteByDataProcessorIdAndBusinessId(String dataProcessorId, String businessId, MongoTemplate mongoTemplate);
    
    /**
     * Check if data processor exists
     * @param dataProcessorId Data processor ID
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return true if exists
     */
    boolean existsByDataProcessorIdAndBusinessId(String dataProcessorId, String businessId, MongoTemplate mongoTemplate);
    
    /**
     * Count data processors by business
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count
     */
    long countByBusinessId(String businessId, MongoTemplate mongoTemplate);
    
    /**
     * Count data processors by business and status
     * @param businessId Business ID
     * @param status Status
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count
     */
    long countByBusinessIdAndStatus(String businessId, DataProcessorStatus status, MongoTemplate mongoTemplate);
    
    /**
     * Validate multiple data processor IDs exist and are active
     * @param dataProcessorIds List of data processor IDs
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of found active DataProcessors
     */
    List<DataProcessor> findActiveByDataProcessorIdsAndBusinessId(List<String> dataProcessorIds, String businessId, MongoTemplate mongoTemplate);
}