package com.jio.digigov.notification.config;

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
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.callback.EntityCallbacks;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MultiTenantMongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${multi-tenant.shared-database}")
    private String sharedDatabase;

    @Value("${multi-tenant.tenant-database-prefix}")
    private String tenantDatabasePrefix;

    @Autowired
    private ApplicationContext applicationContext;

    // Cache MongoTemplates per tenant to reuse instances with auditing enabled
    private final Map<String, MongoTemplate> tenantTemplateCache = new ConcurrentHashMap<>();

    @Override
    protected String getDatabaseName() {
        // Required by AbstractMongoClientConfiguration
        return sharedDatabase;
    }

    @Bean
    @Override
    public MongoClient mongoClient() {
        log.info("Connecting to MongoDB using URI: {}", mongoUri);
        return MongoClients.create(mongoUri);
    }

    /**
     * Primary MongoTemplate for tenant-specific DB operations
     * with auditing support enabled via EntityCallbacks
     */
    @Bean
    @Primary
    public MongoTemplate tenantMongoTemplate(MongoClient mongoClient) {
        log.info("Initializing primary tenant-aware MongoTemplate with auditing support.");
        TenantAwareMongoDbFactory tenantAwareFactory =
                new TenantAwareMongoDbFactory(mongoClient, sharedDatabase, tenantDatabasePrefix);

        // Get the converter bean which has auditing callbacks registered
        MappingMongoConverter converter = applicationContext.getBean(MappingMongoConverter.class);

        TenantAwareMongoTemplate template = new TenantAwareMongoTemplate(
                tenantAwareFactory, tenantAwareFactory, converter);

        // Register entity callbacks (including auditing callbacks) from Spring context
        try {
            EntityCallbacks entityCallbacks = applicationContext.getBean(EntityCallbacks.class);
            template.setEntityCallbacks(entityCallbacks);
            log.info("Primary TenantAwareMongoTemplate created with auditing callbacks enabled");
        } catch (Exception e) {
            log.warn("EntityCallbacks bean not found, auditing may not work properly: {}", e.getMessage());
        }

        return template;
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
     * with auditing support enabled. MongoTemplates are cached per tenant.
     *
     * Special handling:
     * - If tenantId is "SYSTEM", returns sharedMongoTemplate pointing to cms_db_admin
     * - Otherwise, creates tenant-specific MongoTemplate for tenant_db_{tenantId}
     */
    public MongoTemplate getMongoTemplateForTenant(String tenantId) {
        // Route system-wide notifications to shared database
        if ("SYSTEM".equalsIgnoreCase(tenantId)) {
            log.debug("Routing to shared database for system tenant (tenantId: SYSTEM, database: {})", sharedDatabase);
            return sharedMongoTemplate(mongoClient());
        }

        // Return cached template if available
        return tenantTemplateCache.computeIfAbsent(tenantId, tid -> {
            String dbName = tenantDatabasePrefix + tid;
            log.info("Creating new MongoTemplate for tenantId: {} using database: {}", tid, dbName);

            MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient(), dbName);

            // Use the converter bean from Spring context which has auditing callbacks registered
            MappingMongoConverter converter = applicationContext.getBean(MappingMongoConverter.class);

            MongoTemplate template = new MongoTemplate(factory, converter);

            // CRITICAL: Register entity callbacks (including auditing callbacks) from Spring context
            try {
                EntityCallbacks entityCallbacks = applicationContext.getBean(EntityCallbacks.class);
                template.setEntityCallbacks(entityCallbacks);
                log.info("MongoTemplate created for tenant {} with auditing callbacks enabled", tid);
            } catch (Exception e) {
                log.warn("EntityCallbacks bean not found, auditing may not work properly for tenant {}: {}", tid, e.getMessage());
            }

            return template;
        });
    }

    /**
     * Helper method to get shared database MongoTemplate
     * (Can be used for programmatic access if needed)
     */
    // This method is no longer needed as sharedMongoTemplate() is a bean
    // public MongoTemplate getSharedMongoTemplate() {
    //     return sharedMongoTemplate();
    // }
}
