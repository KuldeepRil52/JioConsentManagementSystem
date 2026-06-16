package com.jio.multitranslator.multitenancy;

import com.jio.multitranslator.repository.TenantRegistryRepository;
import com.mongodb.client.MongoClient;
import jakarta.annotation.PostConstruct;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AuditingEntityCallback;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides MongoTemplate instances for multi-tenant database access.
 */
@Slf4j
@Component
public class TenantMongoTemplateProvider {

    private static final String TENANT_DB_PREFIX = "tenant_db_";

    private final TenantRegistryRepository tenantRegistryRepository;
    private final MongoClient mongoClient;
    private final AuditingEntityCallback auditingEntityCallback;
    private final Map<String, MongoTemplate> tenantTemplates = new ConcurrentHashMap<>();

    public TenantMongoTemplateProvider(
            TenantRegistryRepository tenantRegistryRepository,
            MongoClient mongoClient,
            AuditingEntityCallback auditingEntityCallback) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.mongoClient = mongoClient;
        this.auditingEntityCallback = auditingEntityCallback;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing tenant MongoTemplates");
        try {
            tenantRegistryRepository.findAll().forEach(tenant -> {
                String tenantId = tenant.getTenantId();
                if (tenantId != null && !tenantId.isEmpty()) {
                    tenantTemplates.put(tenantId, createMongoTemplate(tenantId));
                    log.debug("Initialized MongoTemplate for tenant: {}", tenantId);
                }
            });
            log.info("Initialized {} tenant MongoTemplate(s)", tenantTemplates.size());
        } catch (Exception e) {
            log.error("Error initializing tenant MongoTemplates", e);
        }
    }

    /**
     * Gets or creates a MongoTemplate for the given tenant ID.
     *
     * @param tenantId the tenant ID
     * @return the MongoTemplate for the tenant
     */
    public MongoTemplate getMongoTemplate(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("Tenant ID is null or empty");
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        return tenantTemplates.computeIfAbsent(tenantId, this::createMongoTemplate);
    }

    /**
     * Creates a new MongoTemplate for the given tenant ID.
     *
     * @param tenantId the tenant ID
     * @return a new MongoTemplate instance
     */
    private MongoTemplate createMongoTemplate(String tenantId) {
        String databaseName = TENANT_DB_PREFIX + tenantId;
        log.debug("Creating MongoTemplate for tenant: {}, database: {}", tenantId, databaseName);
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
        mongoTemplate.setEntityCallbacks(EntityCallbacks.create(this.auditingEntityCallback));
        return mongoTemplate;
    }
}
