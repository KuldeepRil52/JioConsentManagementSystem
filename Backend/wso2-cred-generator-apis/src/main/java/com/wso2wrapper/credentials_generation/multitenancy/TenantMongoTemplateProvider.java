package com.wso2wrapper.credentials_generation.multitenancy;

import com.mongodb.client.MongoClient;
import com.wso2wrapper.credentials_generation.repository.TenantRepository;
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

    private final TenantRepository tenantRepository;
    private final MongoClient mongoClient;
    private final AuditingEntityCallback auditingEntityCallback;

    private final Map<String, MongoTemplate> tenantTemplates = new ConcurrentHashMap<>();

    private MongoTemplate cmsTemplate; // central template for wso2_tenants & wso2_available_apis

    public TenantMongoTemplateProvider(TenantRepository tenantRepository, MongoClient mongoClient, AuditingEntityCallback auditingEntityCallback) {
        this.tenantRepository = tenantRepository;
        this.mongoClient = mongoClient;
        this.auditingEntityCallback = auditingEntityCallback;

        // create central template immediately
        this.cmsTemplate = createCmsTemplate();
    }

    public MongoTemplate getTenantTemplate(String tenantId) {
        return tenantTemplates.computeIfAbsent(tenantId, this::createTenantTemplate);
    }

    // Getter with name as requested
    public MongoTemplate cmsMongoTemplate() {
        return cmsTemplate;
    }

    private MongoTemplate createTenantTemplate(String tenantId) {
        MongoTemplate template = new MongoTemplate(mongoClient, "tenant_db_" + tenantId);
        template.setEntityCallbacks(EntityCallbacks.create(this.auditingEntityCallback));
        return template;
    }

    private MongoTemplate createCmsTemplate() {
        MongoTemplate template = new MongoTemplate(mongoClient, "cms_db_admin");
        template.setEntityCallbacks(EntityCallbacks.create(this.auditingEntityCallback));
        return template;
    }
}
