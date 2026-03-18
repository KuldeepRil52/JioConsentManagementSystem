package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.User;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.TenantUserRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class TenantUserRepositoryImpl implements TenantUserRepository {

    TenantMongoTemplateProvider tenantMongoTemplateProvider;
    @Autowired
    public TenantUserRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public boolean findByUserId(String userId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("userId").is(userId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.exists(query, User.class);
    }
}
