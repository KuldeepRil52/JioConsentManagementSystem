package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.entity.DataBreachReport.NotificationStatus;
import com.jio.partnerportal.entity.NotificationTriggerCentral;
import com.jio.partnerportal.repository.NotificationTriggerCentralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationTriggerCentralRepositoryImpl implements NotificationTriggerCentralRepository {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public NotificationTriggerCentralRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public NotificationTriggerCentral save(NotificationTriggerCentral notificationTriggerCentral) {
        return mongoTemplate.save(notificationTriggerCentral);
    }

    @Override
    public NotificationTriggerCentral findByTriggerId(String triggerId) {
        Criteria criteria = new Criteria();
        criteria.and("triggerId").is(triggerId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, NotificationTriggerCentral.class);
    }

    @Override
    public List<NotificationTriggerCentral> findByBusinessId(String businessId) {
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, NotificationTriggerCentral.class);
    }

    @Override
    public List<NotificationTriggerCentral> findByStatus(NotificationStatus status) {
        Criteria criteria = new Criteria();
        criteria.and("status").is(status);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, NotificationTriggerCentral.class);
    }
}

