package com.jio.digigov.auditmodule.config;

import com.jio.digigov.auditmodule.util.TenantContextHolder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Slf4j
public class TenantAwareMongoDbFactory extends SimpleMongoClientDatabaseFactory {

    private final String sharedDatabase;
    private final String tenantDatabasePrefix;

    public TenantAwareMongoDbFactory(MongoClient mongoClient, String sharedDatabase, String tenantDatabasePrefix) {
        super(mongoClient, sharedDatabase); // Default database for SimpleMongoClientDatabaseFactory
        this.sharedDatabase = sharedDatabase;
        this.tenantDatabasePrefix = tenantDatabasePrefix;
    }

    @Override
    public MongoDatabase getMongoDatabase() {
        try {
            String tenantId = TenantContextHolder.getTenant();
            String dbName = tenantDatabasePrefix + tenantId;
            log.debug("TenantAwareMongoDbFactory: Retrieved tenantId: {}, Resolved DB Name: {}", tenantId, dbName);
            return super.getMongoDatabase(dbName); // Use the tenant-specific database
        } catch (IllegalStateException e) {
            log.warn("TenantAwareMongoDbFactory: No tenant context found, falling back to shared database: {}. This might indicate a missing tenantId in the request or an operation outside a tenant context.", sharedDatabase);
            return super.getMongoDatabase(sharedDatabase); // Fallback to shared
        }
    }
}
