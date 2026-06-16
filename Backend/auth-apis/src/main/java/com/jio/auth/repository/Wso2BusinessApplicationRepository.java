package com.jio.auth.repository;

import com.jio.auth.config.TenantMongoTemplateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.bson.Document;

@Repository
public class Wso2BusinessApplicationRepository {

    @Autowired
    private TenantMongoTemplateFactory tenantMongoTemplateFactory;

    private static final String COLLECTION_NAME = "wso2_business_applications";
    private static final String FIELD_BUSINESS_ID = "businessId";

    public Document findByBusinessId(String tenantId, String businessId) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where(FIELD_BUSINESS_ID).is(businessId));
        return template.findOne(query, Document.class, COLLECTION_NAME);
    }
}
