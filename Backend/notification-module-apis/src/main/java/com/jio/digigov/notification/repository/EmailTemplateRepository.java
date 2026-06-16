package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.TemplateStatus;
import com.jio.digigov.notification.entity.EmailTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for EmailTemplate
 * Operates on tenant-specific database (tenant_db_{tenantId})
 */
@Repository
public interface EmailTemplateRepository extends MongoRepository<EmailTemplate, String> {
    
    /**
     * Find template by templateId (DigiGov templateId)
     */
    Optional<EmailTemplate> findByTemplateId(String templateId);
    
    /**
     * Find templates by business, scope and type
     */
    List<EmailTemplate> findByBusinessIdAndScopeLevelAndType(
        String businessId, ScopeLevel scopeLevel, NotificationType type);
    
    /**
     * Find active templates by business ID
     */
    List<EmailTemplate> findByBusinessIdAndStatus(String businessId, TemplateStatus status);
    
    /**
     * Find templates by business ID and status
     */
    @Query("{'business_id': ?0, 'status': {'$in': ?1}}")
    List<EmailTemplate> findByBusinessIdAndStatusIn(String businessId, List<TemplateStatus> statuses);
    
    /**
     * Count templates by business and status
     */
    long countByBusinessIdAndStatus(String businessId, TemplateStatus status);
    
    /**
     * Check if template exists by templateId
     */
    boolean existsByTemplateId(String templateId);
}