package com.jio.schedular.multitenancy;

import com.jio.schedular.repository.TenantRegistryRepository;
import com.mongodb.client.MongoClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AuditingEntityCallback;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantMongoTemplateProvider {

    TenantRegistryRepository tenantRepository;
    MongoClient mongoClient;
    AuditingEntityCallback auditingEntityCallback;

    @Autowired
    public TenantMongoTemplateProvider(TenantRegistryRepository tenantRepository, MongoClient mongoClient, AuditingEntityCallback auditingEntityCallback) {
        this.tenantRepository = tenantRepository;
        this.mongoClient = mongoClient;
        this.auditingEntityCallback = auditingEntityCallback;
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
        return mongoTemplate;
    }
}
