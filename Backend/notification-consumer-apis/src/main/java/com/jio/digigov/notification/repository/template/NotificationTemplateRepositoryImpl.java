package com.jio.digigov.notification.repository.template;

import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.TemplateStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Custom implementation for tenant-aware notification template operations
 * Uses the primary MongoTemplate bean which is tenant-aware
 */
@Slf4j
@Repository
public class NotificationTemplateRepositoryImpl implements NotificationTemplateRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public NotificationTemplateRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        log.info("NotificationTemplateRepositoryImpl initialized with MongoTemplate: {}", 
                mongoTemplate.getClass().getSimpleName());
    }

    @Override
    public Optional<NotificationTemplate> findByBusinessIdCustom(String businessId) {
        log.debug("Finding template by businessId: {} using tenant-aware MongoTemplate", businessId);
        Query query = new Query(Criteria.where("businessId").is(businessId));
        return Optional.ofNullable(mongoTemplate.findOne(query, NotificationTemplate.class));
    }

    @Override
    public NotificationTemplate saveCustom(NotificationTemplate template) {
        log.debug("Saving template: {} using tenant-aware MongoTemplate", template.getTemplateId());
        // Manually set timestamps if not already set (workaround for multi-tenant auditing)
        LocalDateTime now = LocalDateTime.now();
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(now);
        }
        template.setUpdatedAt(now); // Always update the updatedAt timestamp
        return mongoTemplate.save(template);
    }

    @Override
    public void deleteByBusinessIdCustom(String businessId) {
        log.debug("Deleting templates by businessId: {} using tenant-aware MongoTemplate", businessId);
        Query query = new Query(Criteria.where("businessId").is(businessId));
        mongoTemplate.remove(query, NotificationTemplate.class);
    }

    @Override
    public Page<NotificationTemplate> findTemplatesWithFilters(
            String businessId, 
            String eventType, 
            String language, 
            TemplateStatus status, 
            NotificationType type,
            String searchText,
            Pageable pageable) {
        
        log.debug("Finding templates with filters: businessId={}, eventType={}, language={}, status={}, type={}, search={}",
                businessId, eventType, language, status, type, searchText);

        Criteria criteria = Criteria.where("businessId").is(businessId);

        if (eventType != null && !eventType.trim().isEmpty()) {
            criteria.and("eventType").is(eventType);
        }
        
        if (language != null && !language.trim().isEmpty()) {
            criteria.and("language").is(language);
        }
        
        if (status != null) {
            criteria.and("status").is(status);
        }
        
        if (type != null) {
            criteria.and("type").is(type);
        }

        // Add search text criteria
        if (searchText != null && !searchText.trim().isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                Criteria.where("smsConfig.template").regex(searchText, "i"),
                Criteria.where("emailConfig.templateBody").regex(searchText, "i"),
                Criteria.where("emailConfig.templateSubject").regex(searchText, "i")
            );
            criteria.andOperator(searchCriteria);
        }

        Query query = new Query(criteria);
        
        // Get total count
        long total = mongoTemplate.count(query, NotificationTemplate.class);
        
        // Apply pagination
        query.with(pageable);
        
        List<NotificationTemplate> templates = mongoTemplate.find(query, NotificationTemplate.class);
        
        return new PageImpl<>(templates, pageable, total);
    }

    @Override
    public List<NotificationTemplate> findActiveTemplatesForBusiness(String businessId) {
        log.debug("Finding active templates for businessId: {} using tenant-aware MongoTemplate", businessId);
        Query query = new Query(
            Criteria.where("businessId").is(businessId)
                   .and("status").is(TemplateStatus.ACTIVE)
        );
        return mongoTemplate.find(query, NotificationTemplate.class);
    }
}