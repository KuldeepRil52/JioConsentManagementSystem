package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.entity.NotificationEvent;
import com.jio.partnerportal.repository.NotificationEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationEventRepositoryImpl implements NotificationEventRepository {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public NotificationEventRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public NotificationEvent save(NotificationEvent notificationEvent) {
        return mongoTemplate.save(notificationEvent);
    }

    @Override
    public NotificationEvent findByNotificationId(String notificationId) {
        Criteria criteria = new Criteria();
        criteria.and("notificationId").is(notificationId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, NotificationEvent.class);
    }

    @Override
    public List<NotificationEvent> findByTenantId(String tenantId) {
        Criteria criteria = new Criteria();
        criteria.and("tenantId").is(tenantId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, NotificationEvent.class);
    }

    @Override
    public List<NotificationEvent> findByStatus(String status) {
        Criteria criteria = new Criteria();
        criteria.and("status").is(status);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, NotificationEvent.class);
    }
}

