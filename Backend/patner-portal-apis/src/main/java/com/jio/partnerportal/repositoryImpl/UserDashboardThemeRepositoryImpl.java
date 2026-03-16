package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.UserDashboardTheme;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.UserDashboardThemeRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class UserDashboardThemeRepositoryImpl implements UserDashboardThemeRepository {

    private final TenantMongoTemplateProvider mongoTemplateProvider;

    public UserDashboardThemeRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public UserDashboardTheme save(UserDashboardTheme userDashboardTheme) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(userDashboardTheme);
    }

    @Override
    public UserDashboardTheme findByTenantIdAndBusinessId(String tenantId, String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("tenantId").is(tenantId);
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, UserDashboardTheme.class);
    }

    @Override
    public boolean existsByTenantIdAndBusinessId(String tenantId, String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("tenantId").is(tenantId);
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.exists(query, UserDashboardTheme.class);
    }
}

