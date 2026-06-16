package com.example.scanner.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiTenantMongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${multi-tenant.shared-database}")
    private String sharedDatabase;

    @Value("${multi-tenant.tenant-database-prefix}")
    private String tenantDatabasePrefix;

    @Override
    protected String getDatabaseName() {
        return sharedDatabase;
    }

    @Bean
    @Override
    public MongoClient mongoClient() {
        log.info("Connecting to MongoDB");
        return MongoClients.create(mongoUri);
    }

    /**
     * Primary MongoTemplate for tenant-specific DB operations
     */
    @Bean
    @Primary
    public MongoTemplate tenantMongoTemplate(MongoClient mongoClient) {
        log.info("Initializing primary tenant-aware MongoTemplate.");
        TenantAwareMongoDbFactory tenantAwareFactory =
                new TenantAwareMongoDbFactory(mongoClient, sharedDatabase, tenantDatabasePrefix);
        return new TenantAwareMongoTemplate(tenantAwareFactory, tenantAwareFactory);
    }

    /**
     * MongoTemplate for shared database operations
     */
    @Bean
    public MongoTemplate sharedMongoTemplate(MongoClient mongoClient) {
        MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient, sharedDatabase);
        return new MongoTemplate(factory);
    }

    /**
     * Helper method to get tenant-specific MongoTemplate programmatically
     */
    public MongoTemplate getMongoTemplateForTenant(String tenantId) {
        String dbName = tenantDatabasePrefix + tenantId;
        log.info("Getting MongoTemplate");
        MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient(), dbName);
        return new MongoTemplate(factory);
    }

}
