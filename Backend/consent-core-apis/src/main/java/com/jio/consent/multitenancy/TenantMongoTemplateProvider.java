package com.jio.consent.multitenancy;

import com.jio.consent.repository.TenantRegistryRepository;
import com.mongodb.client.MongoClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexCreator;
import org.springframework.data.mongodb.core.mapping.event.AuditingEntityCallback;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantMongoTemplateProvider {

    TenantRegistryRepository tenantRepository;
    MongoClient mongoClient;
    AuditingEntityCallback auditingEntityCallback;
    MongoMappingContext mongoMappingContext;

    @Autowired
    public TenantMongoTemplateProvider(TenantRegistryRepository tenantRepository,
                                       MongoClient mongoClient,
                                       AuditingEntityCallback auditingEntityCallback,
                                       MongoMappingContext mongoMappingContext) {
        this.tenantRepository = tenantRepository;
        this.mongoClient = mongoClient;
        this.auditingEntityCallback = auditingEntityCallback;
        this.mongoMappingContext = mongoMappingContext;
    }

    private final Map<String, MongoTemplate> tenantTemplates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Load all tenants and initialize their MongoTemplates
        this.tenantRepository.findAll().forEach(tenant -> {
            String tenantId = tenant.getTenantId();
            tenantTemplates.put(tenantId, createMongoTemplate(tenantId));
        });
    }

    public MongoTemplate getMongoTemplate(String tenantId) {
        return tenantTemplates.computeIfAbsent(tenantId,
                this::createMongoTemplate); // DB name = tenantId
    }

    public MongoTemplate createMongoTemplate(String tenantId) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, "tenant_db_" + tenantId);
        mongoTemplate.setEntityCallbacks(EntityCallbacks.create(this.auditingEntityCallback));
        ensureIndexesFromAnnotations(mongoTemplate);
        return mongoTemplate;
    }

    private void ensureIndexesFromAnnotations(MongoTemplate mongoTemplate) {
        try {
            // Use MongoPersistentEntityIndexCreator to process @Indexed annotations
            // The constructor automatically processes all entities and creates indexes
            new MongoPersistentEntityIndexCreator(mongoMappingContext, mongoTemplate);
        } catch (Exception e) {
            // Log error but don't fail template creation
            System.err.println("Error creating indexes from annotations: " + e.getMessage());
        }
    }

}
