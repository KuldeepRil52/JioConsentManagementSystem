package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.BusinessApplication;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.BusinessApplicationRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class BusinessApplicationRepositoryImpl implements BusinessApplicationRepository {
    private final TenantMongoTemplateProvider mongoTemplateProvider;

    @Autowired
    public BusinessApplicationRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    public BusinessApplication findBusinessApplicationByApplicationId(String businessApplicationId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessApplicationId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, BusinessApplication.class);
    }

    @Override
    public BusinessApplication findByBusinessId(String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, BusinessApplication.class);
    }

    @Override
    public List<BusinessApplication> findByBusinessIds(List<String> businessIds) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").in(businessIds);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, BusinessApplication.class);
    }

    @Override
    public BusinessApplication save(BusinessApplication businessApplication) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(businessApplication);

    }

    @Override
    public List<BusinessApplication> findBusinessApplicationByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, BusinessApplication.class);

    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), BusinessApplication.class);
    }

    @Override
    public boolean existByScopeLevel(String scopeLevel) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = Query.query(Criteria.where("scopeLevel").is(scopeLevel));
        return mongoTemplate.exists(query, BusinessApplication.class);
    }

    @Override
    public boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("scopeLevel").is(scopeLevel);
        criteria.and("businessId").is(businessId);
        return mongoTemplate.exists(new Query(criteria), BusinessApplication.class);
    }
}

