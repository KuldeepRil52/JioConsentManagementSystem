package com.jio.digigov.notification.repository.event;

import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.enums.EventPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Event Configuration operations using multi-tenant approach
 */
public interface EventConfigurationRepository {
    
    /**
     * Find event configuration by business and event type with 3-step fallback
     * Step 1: businessId match
     * Step 2: scopeLevel=TENANT match
     * Step 3: tenantId in businessId field match
     * @param businessId Business ID
     * @param tenantId Tenant ID
     * @param eventType Event type
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional EventConfiguration
     */
    Optional<EventConfiguration> findByBusinessIdAndEventType(String businessId, String tenantId, String eventType, MongoTemplate mongoTemplate);
    
    /**
     * Find all event configurations for a business
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of EventConfigurations
     */
    List<EventConfiguration> findByBusinessId(String businessId, MongoTemplate mongoTemplate);
    
    /**
     * Find event configurations with pagination and filters
     * @param businessId Business ID
     * @param pageable Pagination parameters
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Page of EventConfigurations
     */
    Page<EventConfiguration> findByBusinessId(String businessId, Pageable pageable, MongoTemplate mongoTemplate);
    
    /**
     * Find event configurations by business and active status
     * @param businessId Business ID
     * @param isActive Active status
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of EventConfigurations
     */
    List<EventConfiguration> findByBusinessIdAndIsActive(String businessId, Boolean isActive, MongoTemplate mongoTemplate);
    
    /**
     * Find event configurations by business and priority
     * @param businessId Business ID
     * @param priority Event priority
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return List of EventConfigurations
     */
    List<EventConfiguration> findByBusinessIdAndPriority(String businessId, EventPriority priority, MongoTemplate mongoTemplate);
    
    /**
     * Save event configuration
     * @param eventConfiguration EventConfiguration to save
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Saved EventConfiguration
     */
    EventConfiguration save(EventConfiguration eventConfiguration, MongoTemplate mongoTemplate);
    
    /**
     * Delete event configuration by business and event type (soft delete)
     * @param businessId Business ID
     * @param eventType Event type
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of updated records
     */
    long softDeleteByBusinessIdAndEventType(String businessId, String eventType, MongoTemplate mongoTemplate);

    /**
     * Delete event configuration by business and event type (hard delete)
     * @param businessId Business ID
     * @param eventType Event type
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of deleted records
     */
    long deleteByBusinessIdAndEventType(String businessId, String eventType, MongoTemplate mongoTemplate);
    
    /**
     * Check if event configuration exists
     * @param businessId Business ID
     * @param eventType Event type
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return true if exists
     */
    boolean existsByBusinessIdAndEventType(String businessId, String eventType, MongoTemplate mongoTemplate);
    
    /**
     * Count event configurations by business
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count
     */
    long countByBusinessId(String businessId, MongoTemplate mongoTemplate);
    
    /**
     * Count active event configurations by business
     * @param businessId Business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count
     */
    long countByBusinessIdAndIsActive(String businessId, Boolean isActive, MongoTemplate mongoTemplate);
    
    /**
     * Count event configurations by business and priority
     * @param businessId Business ID
     * @param priority Event priority
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Count
     */
    long countByBusinessIdAndPriority(String businessId, EventPriority priority, MongoTemplate mongoTemplate);
}