package com.jio.digigov.notification.repository.template;

import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Custom repository interface for tenant-aware notification template operations
 * Provides explicit tenant context handling for operations that need it
 */
public interface NotificationTemplateRepositoryCustom {

    /**
     * Find template by businessId with explicit tenant context
     */
    Optional<NotificationTemplate> findByBusinessIdCustom(String businessId);

    /**
     * Save template with explicit tenant context
     */
    NotificationTemplate saveCustom(NotificationTemplate template);

    /**
     * Delete template by businessId with explicit tenant context
     */
    void deleteByBusinessIdCustom(String businessId);

    /**
     * Find templates with complex filters using tenant context
     * Templates are retrieved if businessId matches either the provided businessId OR tenantId
     */
    Page<NotificationTemplate> findTemplatesWithFilters(
        String businessId,
        String tenantId,
        String eventType,
        String language,
        TemplateStatus status,
        NotificationType type,
        String searchText,
        Pageable pageable);

    /**
     * Get count breakdown by categories for tenant
     */
    List<NotificationTemplate> findActiveTemplatesForBusiness(String businessId);

    /**
     * Check if any templates exist for this business in the tenant database.
     * Used for onboarding validation to ensure clean slate before creating defaults.
     *
     * @param businessId Business identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return true if at least one template exists, false otherwise
     */
    boolean existsByBusinessId(String businessId, MongoTemplate mongoTemplate);

    /**
     * Count templates for this business in the tenant database.
     * Used for onboarding validation to provide detailed error messages.
     *
     * @param businessId Business identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return count of templates
     */
    long countByBusinessId(String businessId, MongoTemplate mongoTemplate);
}