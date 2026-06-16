package com.example.scanner.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Slf4j
public class TenantAwareMongoDbFactory extends SimpleMongoClientDatabaseFactory {

    private final String sharedDatabase;
    private final String tenantDatabasePrefix;

    public TenantAwareMongoDbFactory(MongoClient mongoClient, String sharedDatabase, String tenantDatabasePrefix) {
        super(mongoClient, sharedDatabase);
        this.sharedDatabase = sharedDatabase;
        this.tenantDatabasePrefix = tenantDatabasePrefix;
    }

    @Override
    public MongoDatabase getMongoDatabase() {
        String tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("No tenant context found");
            return super.getMongoDatabase(sharedDatabase);
        }

        String dbName = tenantDatabasePrefix + tenantId;
        return super.getMongoDatabase(dbName);
    }
}