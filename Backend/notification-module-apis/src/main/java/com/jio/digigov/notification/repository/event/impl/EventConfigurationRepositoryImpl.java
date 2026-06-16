package com.jio.digigov.notification.repository.event.impl;

import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.repository.event.EventConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of EventConfigurationRepository using multi-tenant MongoTemplate approach.
 * 
 * This repository handles CRUD operations for Event Configuration entities in a multi-tenant
 * environment where each tenant has its own dedicated MongoDB database. The repository accepts
 * a tenant-specific MongoTemplate for all operations to ensure data isolation between tenants.
 * 
 * Key Features:
 * - Multi-tenant data isolation using separate MongoTemplate instances
 * - Event configuration management by business ID and event type
 * - Support for filtering by priority, active status
 * - Pagination support for large datasets
 * - Soft delete functionality to maintain data integrity
 * - Comprehensive count operations for analytics
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
public class EventConfigurationRepositoryImpl implements EventConfigurationRepository {

    /**
     * Finds an event configuration by business ID and event type.
     * 
     * This method searches for a specific event configuration that matches both the business ID
     * and event type. Each business can have multiple event configurations, but only one per
     * event type.
     * 
     * @param businessId The unique identifier for the business unit
     * @param eventType The type of event (e.g., "USER_REGISTRATION", "DATA_BREACH")
     * @param mongoTemplate The tenant-specific MongoTemplate for database operations
     * @return Optional containing the EventConfiguration if found, empty otherwise
     * @throws IllegalArgumentException if businessId or eventType is null or empty
     */
    @Override
    public Optional<EventConfiguration> findByBusinessIdAndEventType(String businessId, String eventType, MongoTemplate mongoTemplate) {
        log.debug("Finding event configuration by businessId: {} and eventType: {}", businessId, eventType);
        
        Query query = new Query(Criteria.where("businessId").is(businessId).and("eventType").is(eventType));
        EventConfiguration result = mongoTemplate.findOne(query, EventConfiguration.class);
        return Optional.ofNullable(result);
    }

    @Override
    public List<EventConfiguration> findByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding all event configurations by businessId: {}", businessId);
        
        Query query = new Query(Criteria.where("businessId").is(businessId));
        return mongoTemplate.find(query, EventConfiguration.class);
    }

    @Override
    public Page<EventConfiguration> findByBusinessId(String businessId, Pageable pageable, MongoTemplate mongoTemplate) {
        log.debug("Finding event configurations by businessId: {} with pagination", businessId);
        
        Query query = new Query(Criteria.where("businessId").is(businessId));
        
        long total = mongoTemplate.count(query, EventConfiguration.class);
        
        query.with(pageable);
        List<EventConfiguration> results = mongoTemplate.find(query, EventConfiguration.class);
        
        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public List<EventConfiguration> findByBusinessIdAndIsActive(String businessId, Boolean isActive, MongoTemplate mongoTemplate) {
        log.debug("Finding event configurations by businessId: {} and isActive: {}", businessId, isActive);
        
        Query query = new Query(Criteria.where("businessId").is(businessId).and("isActive").is(isActive));
        return mongoTemplate.find(query, EventConfiguration.class);
    }

    @Override
    public List<EventConfiguration> findByBusinessIdAndPriority(String businessId, EventPriority priority, MongoTemplate mongoTemplate) {
        log.debug("Finding event configurations by businessId: {} and priority: {}", businessId, priority);
        
        Query query = new Query(Criteria.where("businessId").is(businessId).and("priority").is(priority));
        return mongoTemplate.find(query, EventConfiguration.class);
    }

    /**
     * Saves an event configuration to the database.
     * 
     * This method handles both create and update operations. For new entities (where createdAt is null),
     * it sets the creation timestamp. The updatedAt timestamp is always set to the current time.
     * 
     * The save operation is atomic and will either succeed completely or fail without partial updates.
     * 
     * @param eventConfiguration The event configuration entity to save
     * @param mongoTemplate The tenant-specific MongoTemplate for database operations
     * @return The saved EventConfiguration with updated timestamps and generated ID (if new)
     * @throws org.springframework.dao.DuplicateKeyException if an entity with the same business ID and event type already exists
     * @throws IllegalArgumentException if eventConfiguration is null
     */
    @Override
    public EventConfiguration save(EventConfiguration eventConfiguration, MongoTemplate mongoTemplate) {
        log.debug("Saving event configuration for businessId: {}, eventType: {}",
                eventConfiguration.getBusinessId(), eventConfiguration.getEventType());

        // Manually set timestamps (workaround for multi-tenant auditing)
        LocalDateTime now = LocalDateTime.now();
        if (eventConfiguration.getCreatedAt() == null) {
            eventConfiguration.setCreatedAt(now);
        }
        eventConfiguration.setUpdatedAt(now); // Always update the updatedAt timestamp

        return mongoTemplate.save(eventConfiguration);
    }

    /**
     * Performs a soft delete of an event configuration by setting isActive to false.
     * 
     * Soft delete is preferred over hard delete to maintain data integrity and audit trails.
     * The configuration remains in the database but is marked as inactive, preventing it from
     * being used for new event processing while preserving historical data.
     * 
     * @param businessId The unique identifier for the business unit
     * @param eventType The type of event configuration to deactivate
     * @param mongoTemplate The tenant-specific MongoTemplate for database operations
     * @return The number of documents modified (0 if not found, 1 if successfully deactivated)
     * @throws IllegalArgumentException if businessId or eventType is null or empty
     */
    @Override
    public long softDeleteByBusinessIdAndEventType(String businessId, String eventType, MongoTemplate mongoTemplate) {
        log.debug("Soft deleting event configuration by businessId: {} and eventType: {}", businessId, eventType);
        
        Query query = new Query(Criteria.where("businessId").is(businessId).and("eventType").is(eventType));
        Update update = new Update()
                .set("isActive", false)  // Mark as inactive instead of deleting
                .set("updatedAt", LocalDateTime.now());  // Update modification timestamp
        
        return mongoTemplate.updateFirst(query, update, EventConfiguration.class).getModifiedCount();
    }

    @Override
    public boolean existsByBusinessIdAndEventType(String businessId, String eventType, MongoTemplate mongoTemplate) {
        log.debug("Checking existence of event configuration by businessId: {} and eventType: {}", businessId, eventType);

        Query query = new Query(Criteria.where("businessId").is(businessId).and("eventType").is(eventType));
        return mongoTemplate.exists(query, EventConfiguration.class);
    }

    @Override
    public boolean existsByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Checking if any event configurations exist for businessId: {}", businessId);

        Query query = new Query(Criteria.where("businessId").is(businessId));
        query.limit(1); // Optimization: only need to know if at least one exists

        return mongoTemplate.exists(query, EventConfiguration.class);
    }

    @Override
    public long countByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Counting event configurations by businessId: {}", businessId);

        Query query = new Query(Criteria.where("businessId").is(businessId));
        return mongoTemplate.count(query, EventConfiguration.class);
    }

    @Override
    public long countByBusinessIdAndIsActive(String businessId, Boolean isActive, MongoTemplate mongoTemplate) {
        log.debug("Counting event configurations by businessId: {} and isActive: {}", businessId, isActive);
        
        Query query = new Query(Criteria.where("businessId").is(businessId).and("isActive").is(isActive));
        return mongoTemplate.count(query, EventConfiguration.class);
    }

    @Override
    public long countByBusinessIdAndPriority(String businessId, EventPriority priority, MongoTemplate mongoTemplate) {
        log.debug("Counting event configurations by businessId: {} and priority: {}", businessId, priority);

        Query query = new Query(Criteria.where("businessId").is(businessId).and("priority").is(priority));
        return mongoTemplate.count(query, EventConfiguration.class);
    }

    @Override
    public long deleteByBusinessIdAndEventType(String businessId, String eventType, MongoTemplate mongoTemplate) {
        log.debug("Hard deleting event configuration by businessId: {} and eventType: {}", businessId, eventType);

        Query query = new Query(Criteria.where("businessId").is(businessId).and("eventType").is(eventType));

        return mongoTemplate.remove(query, EventConfiguration.class).getDeletedCount();
    }
}