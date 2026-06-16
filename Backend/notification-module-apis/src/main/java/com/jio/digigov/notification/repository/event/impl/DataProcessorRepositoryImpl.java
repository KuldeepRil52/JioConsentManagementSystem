package com.jio.digigov.notification.repository.event.impl;

import com.jio.digigov.notification.entity.event.DataProcessor;
import com.jio.digigov.notification.enums.DataProcessorStatus;
import com.jio.digigov.notification.repository.event.DataProcessorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of DataProcessorRepository using multi-tenant MongoTemplate approach.
 * 
 * This repository manages Data Processor entities in a multi-tenant environment where each tenant
 * maintains their own MongoDB database. Data Processors represent third-party entities that
 * process personal data on behalf of businesses and require notification when data protection
 * events occur.
 * 
 * Key Features:
 * - Multi-tenant data isolation using tenant-specific MongoTemplate instances
 * - Data processor lifecycle management (ACTIVE, INACTIVE, SUSPENDED)
 * - Bulk operations for efficient data processor retrieval
 * - Pagination support for large datasets
 * - Comprehensive filtering by status and business association
 * 
 * Data Processor Lifecycle:
 * - ACTIVE: Processor is operational and receiving notifications
 * - INACTIVE: Processor is temporarily disabled
 * - SUSPENDED: Processor is suspended due to compliance issues
 * 
 * Thread Safety: This repository is thread-safe as it's stateless and relies on
 * thread-safe MongoTemplate operations.
 * 
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class DataProcessorRepositoryImpl implements DataProcessorRepository {
    
    @Override
    public Optional<DataProcessor> findByDataProcessorIdAndBusinessId(String dataProcessorId, String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding data processor by dataProcessorId: {} and businessId: {}", dataProcessorId, businessId);

        Query query = Query.query(
            Criteria.where("dataProcessorId").is(dataProcessorId)
                    .and("businessId").is(businessId)
        );

        DataProcessor result = mongoTemplate.findOne(query, DataProcessor.class);
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<DataProcessor> findByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding all data processors for businessId: {}", businessId);
        
        Query query = Query.query(Criteria.where("businessId").is(businessId));
        return mongoTemplate.find(query, DataProcessor.class);
    }
    
    @Override
    public List<DataProcessor> findByBusinessIdAndStatus(String businessId, DataProcessorStatus status, MongoTemplate mongoTemplate) {
        log.debug("Finding data processors for businessId: {} with status: {}", businessId, status);
        
        Query query = Query.query(
            Criteria.where("businessId").is(businessId)
                    .and("status").is(status)
        );
        
        return mongoTemplate.find(query, DataProcessor.class);
    }
    
    @Override
    public Page<DataProcessor> findByBusinessId(String businessId, Pageable pageable, MongoTemplate mongoTemplate) {
        log.debug("Finding data processors with pagination for businessId: {}, page: {}, size: {}", 
                 businessId, pageable.getPageNumber(), pageable.getPageSize());
        
        Query query = Query.query(Criteria.where("businessId").is(businessId))
                          .with(pageable);
        
        List<DataProcessor> dataProcessors = mongoTemplate.find(query, DataProcessor.class);
        long total = mongoTemplate.count(Query.query(Criteria.where("businessId").is(businessId)), DataProcessor.class);
        
        return new PageImpl<>(dataProcessors, pageable, total);
    }
    
    /**
     * Saves a data processor entity to the database.
     * 
     * This method handles both create and update operations with automatic timestamp management.
     * For new entities (where ID is null), it sets the creation timestamp and creator.
     * The updatedAt timestamp and updatedBy fields are always set to current values.
     * 
     * @param dataProcessor The data processor entity to save
     * @param mongoTemplate The tenant-specific MongoTemplate for database operations
     * @return The saved DataProcessor with updated timestamps and generated ID (if new)
     * @throws org.springframework.dao.DuplicateKeyException if a processor with the same dpId and businessId already exists
     * @throws IllegalArgumentException if dataProcessor is null
     */
    @Override
    public DataProcessor save(DataProcessor dataProcessor, MongoTemplate mongoTemplate) {
        log.debug("Saving data processor: {}", dataProcessor.getDataProcessorId());

        // Manually set timestamps (workaround for multi-tenant auditing)
        LocalDateTime now = LocalDateTime.now();
        if (dataProcessor.getCreatedAt() == null) {
            dataProcessor.setCreatedAt(now);
        }
        dataProcessor.setUpdatedAt(now); // Always update the updatedAt timestamp

        return mongoTemplate.save(dataProcessor);
    }
    
    @Override
    public long deleteByDataProcessorIdAndBusinessId(String dataProcessorId, String businessId, MongoTemplate mongoTemplate) {
        log.debug("Deleting data processor by dataProcessorId: {} and businessId: {}", dataProcessorId, businessId);

        Query query = Query.query(
            Criteria.where("dataProcessorId").is(dataProcessorId)
                    .and("businessId").is(businessId)
        );

        return mongoTemplate.remove(query, DataProcessor.class).getDeletedCount();
    }
    
    @Override
    public boolean existsByDataProcessorIdAndBusinessId(String dataProcessorId, String businessId, MongoTemplate mongoTemplate) {
        log.debug("Checking existence of data processor by dataProcessorId: {} and businessId: {}", dataProcessorId, businessId);

        Query query = Query.query(
            Criteria.where("dataProcessorId").is(dataProcessorId)
                    .and("businessId").is(businessId)
        );

        return mongoTemplate.exists(query, DataProcessor.class);
    }
    
    @Override
    public long countByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Counting data processors for businessId: {}", businessId);
        
        Query query = Query.query(Criteria.where("businessId").is(businessId));
        return mongoTemplate.count(query, DataProcessor.class);
    }
    
    @Override
    public long countByBusinessIdAndStatus(String businessId, DataProcessorStatus status, MongoTemplate mongoTemplate) {
        log.debug("Counting data processors for businessId: {} with status: {}", businessId, status);
        
        Query query = Query.query(
            Criteria.where("businessId").is(businessId)
                    .and("status").is(status)
        );
        
        return mongoTemplate.count(query, DataProcessor.class);
    }
    
    /**
     * Finds active data processors by their IDs and business association.
     *
     * This method is optimized for bulk retrieval of active data processors based on a list
     * of data processor IDs. It's commonly used during event processing to identify which
     * processors should receive notifications for a specific business event.
     *
     * Only processors with ACTIVE status are returned, ensuring that suspended or inactive
     * processors don't receive notifications.
     *
     * @param dataProcessorIds List of data processor IDs to search for
     * @param businessId The business ID that the processors must be associated with
     * @param mongoTemplate The tenant-specific MongoTemplate for database operations
     * @return List of active DataProcessor entities matching the criteria
     * @throws IllegalArgumentException if dataProcessorIds is null or empty, or businessId is null
     */
    @Override
    public List<DataProcessor> findActiveByDataProcessorIdsAndBusinessId(List<String> dataProcessorIds, String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding active data processors for businessId: {} with dataProcessorIds: {}", businessId, dataProcessorIds);

        Query query = Query.query(
            Criteria.where("dataProcessorId").in(dataProcessorIds)
                    .and("businessId").is(businessId)
                    .and("status").is(DataProcessorStatus.ACTIVE)  // Only return active processors
        );

        return mongoTemplate.find(query, DataProcessor.class);
    }

    /**
     * Get current authenticated user from security context.
     * For now returns "system" as a default until proper security context is implemented.
     * TODO: Implement proper Spring Security integration to get actual authenticated user
     *
     * @return Current user identifier or "system" as fallback
     */
    private String getCurrentUser() {
        // In a future enhancement, this would integrate with Spring Security:
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
        //     return auth.getName();
        // }
        return "system"; // Fallback for system operations
    }
}