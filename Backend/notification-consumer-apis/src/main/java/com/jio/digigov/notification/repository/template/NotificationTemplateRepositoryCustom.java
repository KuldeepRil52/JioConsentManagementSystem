package com.jio.digigov.notification.repository.template;

import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     */
    Page<NotificationTemplate> findTemplatesWithFilters(
        String businessId, 
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
}