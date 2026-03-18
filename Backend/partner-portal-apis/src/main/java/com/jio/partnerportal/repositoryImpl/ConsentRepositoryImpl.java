package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.ConsentConfig;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.ConsentRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ConsentRepositoryImpl implements ConsentRepository {

    private TenantMongoTemplateProvider mongoTemplateProvider;

    @Autowired
    public ConsentRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public ConsentConfig save(ConsentConfig config) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(config);
    }

    @Override
    public ConsentConfig findByConfigId(String configId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("configId").is(configId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ConsentConfig.class);
    }

    @Override
    public List<ConsentConfig> findConfigByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, ConsentConfig.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), ConsentConfig.class);
    }

    @Override
    public boolean existByScopeLevel(String scopeLevel) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = Query.query(Criteria.where("scopeLevel").is(scopeLevel));
        return mongoTemplate.exists(query, ConsentConfig.class);
    }

    @Override
    public boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("scopeLevel").is(scopeLevel);
        criteria.and("businessId").is(businessId);
        return mongoTemplate.exists(new Query(criteria), ConsentConfig.class);
    }
}
