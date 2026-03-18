package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.NotificationConfig;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.NotificationRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final TenantMongoTemplateProvider mongoTemplateProvider;

    public NotificationRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public NotificationConfig save(NotificationConfig notificationConfig) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(notificationConfig);
    }

    @Override
    public NotificationConfig findByConfigId(String configId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("configId").is(configId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, NotificationConfig.class);
    }

    @Override
    public NotificationConfig findByBusinessId(String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, NotificationConfig.class);
    }

    @Override
    public List<NotificationConfig> findNotificationConfigByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, NotificationConfig.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), NotificationConfig.class);
    }

    @Override
    public boolean existByScopeLevel(String scopeLevel) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = Query.query(Criteria.where("scopeLevel").is(scopeLevel));
        return mongoTemplate.exists(query, NotificationConfig.class);
    }

    @Override
    public boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = Query.query(Criteria.where("scopeLevel").is(scopeLevel).and("businessId").is(businessId));
        return mongoTemplate.exists(query, NotificationConfig.class);
    }
}
