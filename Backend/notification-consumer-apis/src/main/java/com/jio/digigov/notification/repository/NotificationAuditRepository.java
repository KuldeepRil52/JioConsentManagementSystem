package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.NotificationAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationAudit entity
 * Provides CRUD operations for audit records in tenant-specific database
 */
@Repository
public interface NotificationAuditRepository extends MongoRepository<NotificationAudit, String> {
    
    /**
     * Find audit record by transaction ID
     */
    Optional<NotificationAudit> findByTxnId(String txnId);
    
    /**
     * Find all audit records for a specific template ID
     */
    List<NotificationAudit> findByTemplateIdOrderByCreatedAtDesc(String templateId);
    
    /**
     * Find audit records by operation type (event type contains the operation)
     */
    @Query("{'eventType': {$regex: ?0, $options: 'i'}}")
    List<NotificationAudit> findByEventTypeContainingIgnoreCaseOrderByCreatedAtDesc(String operationType);
    
    /**
     * Find audit records by tenant and business within date range
     */
    List<NotificationAudit> findByTenantIdAndBusinessIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, String businessId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Find audit records by tenant within date range
     */
    List<NotificationAudit> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Find audit records older than specified date (for archiving)
     */
    List<NotificationAudit> findByCreatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * Find audit records by status
     */
    List<NotificationAudit> findByStatusOrderByCreatedAtDesc(String status);
    
    /**
     * Count audit records by tenant and business
     */
    long countByTenantIdAndBusinessId(String tenantId, String businessId);
    
    /**
     * Find recent audit records for a tenant (limit to last N records)
     */
    List<NotificationAudit> findTop100ByTenantIdOrderByCreatedAtDesc(String tenantId);
    
    /**
     * Find audit records by template ID and event type
     */
    List<NotificationAudit> findByTemplateIdAndEventTypeOrderByCreatedAtDesc(String templateId, String eventType);
    
    /**
     * Find audit records by operation
     */
    List<NotificationAudit> findByOperationOrderByCreatedAtDesc(String operation);
}