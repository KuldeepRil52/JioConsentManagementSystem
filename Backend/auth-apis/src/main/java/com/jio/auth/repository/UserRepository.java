package com.jio.auth.repository;

import com.jio.auth.config.TenantMongoTemplateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String COLLECTION_TOKENS = "users";

    @Autowired
    private TenantMongoTemplateFactory tenantMongoTemplateFactory;

    public Document findByBusinessId(String tenantId, String sub) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where("userId").is(sub));
        return template.findOne(query, Document.class,COLLECTION_TOKENS );
    }

    public boolean existsByUserId(String tenantId, String sub) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where("userId").is(sub));
        return template.exists(query, COLLECTION_TOKENS);
    }

}
