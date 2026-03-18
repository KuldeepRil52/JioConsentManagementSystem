package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.SmtpConfig;
import com.jio.partnerportal.entity.SystemConfig;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.SystemConfigRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private TenantMongoTemplateProvider mongoTemplateProvider;

    @Autowired
    public SystemConfigRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public SystemConfig save(SystemConfig config) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(config);
    }

    @Override
    public SystemConfig findByConfigId(String configId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("configId").is(configId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, SystemConfig.class);
    }

    @Override
    public List<SystemConfig> findConfigByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, SystemConfig.class);

    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), SmtpConfig.class);
    }

    @Override
    public boolean existByScopeLevel(String scopeLevel) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = Query.query(Criteria.where("scopeLevel").is(scopeLevel));
        return mongoTemplate.exists(query, SystemConfig.class);
    }

    @Override
    public boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("scopeLevel").is(scopeLevel);
        criteria.and("businessId").is(businessId);
        return mongoTemplate.exists(new Query(criteria), SystemConfig.class);
    }

}
