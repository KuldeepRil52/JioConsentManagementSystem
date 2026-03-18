package com.jio.partnerportal.multitenancy;

import com.jio.partnerportal.repository.TenantRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexCreator;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.time.Duration;

import org.springframework.data.mongodb.core.mapping.event.AuditingEntityCallback;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
public class TenantMongoTemplateProvider {

    TenantRepository tenantRepository;
    MongoClient mongoClient;
    AuditingEntityCallback auditingEntityCallback;
    MongoMappingContext mongoMappingContext;
    Environment environment;

    private final MongoTemplate centralMongoTemplate;

    @Autowired
    public TenantMongoTemplateProvider(TenantRepository tenantRepository, MongoClient mongoClient, 
                                       AuditingEntityCallback auditingEntityCallback,
                                       MongoMappingContext mongoMappingContext,
                                       Environment environment,
                                       MongoTemplate centralMongoTemplate) {
        this.tenantRepository = tenantRepository;
        this.mongoClient = mongoClient;
        this.auditingEntityCallback = auditingEntityCallback;
        this.mongoMappingContext = mongoMappingContext;
        this.environment = environment;
        this.centralMongoTemplate = centralMongoTemplate;
    }
    @Value("${otp.time.to.live}")
    private String otpTimeToLive;

    @Value("${auth.secret.time.to.live:PT3M}")
    private String authSecretTimeToLive;

    private final Map<String, MongoTemplate> tenantTemplates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Ensure auth_secret TTL index on main database
        ensureAuthSecretTtlIndex(centralMongoTemplate);
        
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

        // Ensure OTP TTL index
        ensureOtpTtlIndex(mongoTemplate);
        
        return mongoTemplate;
    }
    
    private void ensureIndexesFromAnnotations(MongoTemplate mongoTemplate) {
        try {
            // Use MongoPersistentEntityIndexCreator to process @Indexed annotations
            // The constructor automatically processes all entities and creates indexes
            new MongoPersistentEntityIndexCreator(mongoMappingContext, mongoTemplate);
        } catch (Exception e) {
            // Log error but don't fail template creation
            log.error("Error creating indexes from annotations: " + e);
        }
    }
    private void ensureOtpTtlIndex(MongoTemplate mongoTemplate) {
        // Get the MongoCollection (Spring wraps the driver)
        MongoCollection<Document> collection = mongoTemplate.getCollection("otp");

        long ttlSeconds = Duration.parse(otpTimeToLive).getSeconds();
        IndexOptions options = new IndexOptions()
                .expireAfter(ttlSeconds, java.util.concurrent.TimeUnit.SECONDS); // 10 minutes TTL

        collection.createIndex(Indexes.ascending("createdAt"), options);
    }

    private void ensureAuthSecretTtlIndex(MongoTemplate mongoTemplate) {
        // Get the MongoCollection (Spring wraps the driver)
        MongoCollection<Document> collection = mongoTemplate.getCollection("auth_secret");

        long ttlSeconds = Duration.parse(authSecretTimeToLive).getSeconds();
        IndexOptions options = new IndexOptions()
                .expireAfter(ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);

        collection.createIndex(Indexes.ascending("createdAt"), options);
    }
}
