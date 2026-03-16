package com.jio.auth.config;

import com.mongodb.client.MongoClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class TenantMongoTemplateFactory {

    private final MongoClient mongoClient;

    public TenantMongoTemplateFactory(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoTemplate getTemplateForTenant(String tenantId) {
        String dbName = "tenant_db_" + tenantId;
        return new MongoTemplate(mongoClient, dbName);
    }
}
