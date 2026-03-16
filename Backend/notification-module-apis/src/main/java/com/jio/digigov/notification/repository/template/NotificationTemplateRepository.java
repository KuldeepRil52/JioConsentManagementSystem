package com.jio.digigov.notification.repository.template;

import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String>, NotificationTemplateRepositoryCustom {
    
    // Find by DigiGov template ID
    Optional<NotificationTemplate> findByTemplateId(String templateId);
    
    // Find unique template by business, event type, language, and type  
    Optional<NotificationTemplate> findByBusinessIdAndEventTypeAndLanguageAndType(
        String businessId, String eventType, String language, NotificationType type);
    
    // Find templates by business and status
    List<NotificationTemplate> findByBusinessIdAndStatus(
        String businessId, TemplateStatus status);
    
    // Find templates by business (paginated)
    @Query("{'business_id': ?0}")
    Page<NotificationTemplate> findByBusiness(String businessId, Pageable pageable);
    
    // Find templates with multiple filters
    @Query("{'business_id': ?0, 'status': ?1}")
    Page<NotificationTemplate> findByBusinessAndStatus(
        String businessId, TemplateStatus status, Pageable pageable);
    
    @Query("{'business_id': ?0, 'event_type': ?1}")
    Page<NotificationTemplate> findByBusinessAndEventType(
        String businessId, String eventType, Pageable pageable);
    
    @Query("{'business_id': ?0, 'language': ?1}")
    Page<NotificationTemplate> findByBusinessAndLanguage(
        String businessId, String language, Pageable pageable);
    
    @Query("{'business_id': ?0, 'type': ?1}")
    Page<NotificationTemplate> findByBusinessAndType(
        String businessId, NotificationType type, Pageable pageable);
    
    // Count methods
    long countByBusinessId(String businessId);
    
    long countByBusinessIdAndStatus(String businessId, TemplateStatus status);
    
    long countByBusinessIdAndEventType(String businessId, String eventType);
    
    long countByBusinessIdAndLanguage(String businessId, String language);
    
    long countByBusinessIdAndType(String businessId, NotificationType type);
    
    // Search in template content
    @Query("{'business_id': ?0, $or: [" +
           "{'sms_config.template': {$regex: ?1, $options: 'i'}}, " +
           "{'email_config.template_body': {$regex: ?1, $options: 'i'}}, " +
           "{'email_config.template_subject': {$regex: ?1, $options: 'i'}}" +
           "]}")
    Page<NotificationTemplate> findByBusinessAndSearchText(
        String businessId, String searchText, Pageable pageable);
    
    // Complex query with multiple optional filters
    @Query("{'business_id': ?0, " +
           "$and: [" +
           "?#{[1] == null ? {} : {'event_type': ?1}}, " +
           "?#{[2] == null ? {} : {'language': ?2}}, " +
           "?#{[3] == null ? {} : {'status': ?3}}, " +
           "?#{[4] == null ? {} : {'type': ?4}}" +
           "]}")
    Page<NotificationTemplate> findWithFilters(
        String businessId, String eventType, 
        String language, TemplateStatus status, NotificationType type, Pageable pageable);
}