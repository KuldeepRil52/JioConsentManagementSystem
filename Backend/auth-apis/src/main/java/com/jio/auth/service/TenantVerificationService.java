package com.jio.auth.service;

import com.jio.auth.config.TenantMongoTemplateFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class TenantVerificationService {

    private final TenantMongoTemplateFactory tenantMongoFactory;

    // Collection names
    private static final String COLLECTION_BUSINESS = "business_applications";
    private static final String COLLECTION_USERS = "users";

    // Field names
    private static final String FIELD_BUSINESS_ID = "businessId";
    private static final String FIELD_MOBILE = "mobile";
    private static final String FIELD_EMAIL = "email";

    public TenantVerificationService(TenantMongoTemplateFactory tenantMongoFactory) {
        this.tenantMongoFactory = tenantMongoFactory;
    }

    public boolean verifyTenant(String tenantId) {
        try {
            MongoTemplate tenantTemplate = tenantMongoFactory.getTemplateForTenant(tenantId);
            tenantTemplate.getDb().listCollectionNames(); // quick sanity check
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyBusiness(String tenantId, String businessId) {
        MongoTemplate tenantTemplate = tenantMongoFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where(FIELD_BUSINESS_ID).is(businessId));
        return tenantTemplate.exists(query, COLLECTION_BUSINESS);
    }

    public boolean verifyUser(String tenantId, String identity, String identityType) {
        MongoTemplate tenantTemplate = tenantMongoFactory.getTemplateForTenant(tenantId);
        String field = identityType.equalsIgnoreCase("MOBILE") ? FIELD_MOBILE : FIELD_EMAIL;
        Query query = new Query(Criteria.where(field).is(identity));
        return tenantTemplate.exists(query, COLLECTION_USERS);
    }
}
