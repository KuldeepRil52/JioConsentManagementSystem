package com.example.scanner.config;

import com.mongodb.client.MongoDatabase;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

public class TenantAwareMongoTemplate extends MongoTemplate {

    private final TenantAwareMongoDbFactory tenantAwareMongoDbFactory;

    public TenantAwareMongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, TenantAwareMongoDbFactory tenantAwareMongoDbFactory) {
        super(mongoDatabaseFactory);
        this.tenantAwareMongoDbFactory = tenantAwareMongoDbFactory;
    }

    @Override
    public MongoDatabase getDb() {
        // Always get the database from the tenant-aware factory
        return tenantAwareMongoDbFactory.getMongoDatabase();
    }
}