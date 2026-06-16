package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.entity.DataBreachReport.NotificationStatus;
import com.jio.partnerportal.entity.NotificationTrigger;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.NotificationTriggerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationTriggerRepositoryImpl implements NotificationTriggerRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public NotificationTriggerRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public NotificationTrigger save(NotificationTrigger notificationTrigger, String tenantId) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        return mongoTemplate.save(notificationTrigger);
    }

    @Override
    public NotificationTrigger findByTriggerId(String triggerId, String tenantId) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        Criteria criteria = new Criteria();
        criteria.and("triggerId").is(triggerId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, NotificationTrigger.class);
    }

    @Override
    public List<NotificationTrigger> findByBusinessId(String businessId, String tenantId) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, NotificationTrigger.class);
    }

    @Override
    public List<NotificationTrigger> findByStatus(NotificationStatus status, String tenantId) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        Criteria criteria = new Criteria();
        criteria.and("status").is(status);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, NotificationTrigger.class);
    }
}

