package com.jio.auth.repository;

import com.jio.auth.config.TenantMongoTemplateFactory;
import com.jio.auth.model.Wso2AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class Wso2AccessTokenRepository {

    private static final String COLLECTION_TOKENS = "wso2_access_tokens";

    @Autowired
    private TenantMongoTemplateFactory tenantMongoTemplateFactory;

    public Wso2AccessToken findByBusinessId(String tenantId, String businessId) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where("businessId").is(businessId));
        return template.findOne(query, Wso2AccessToken.class,COLLECTION_TOKENS );
    }

    public void save(String tenantId, Wso2AccessToken token) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        template.save(token, COLLECTION_TOKENS);

    }

    public void deleteByBusinessId(String tenantId, String businessId) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where("businessId").is(businessId));
        template.remove(query, Wso2AccessToken.class, COLLECTION_TOKENS);
    }
}
