package com.jio.multitranslator.repositoryimpl;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.dto.request.Config;
import com.jio.multitranslator.entity.BusinessApplication;
import com.jio.multitranslator.entity.TranslateConfig;
import com.jio.multitranslator.exceptions.BodyValidationException;
import com.jio.multitranslator.exceptions.CustomException;
import com.jio.multitranslator.multitenancy.TenantMongoTemplateProvider;
import com.jio.multitranslator.repository.TranslateConfigRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;


@Slf4j
@Repository
public class TranslateConfigRepositoryImpl implements TranslateConfigRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    public TranslateConfigRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    /**
     * Gets the MongoTemplate for the current tenant from ThreadContext.
     */
    private MongoTemplate getMongoTemplate() {
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("Tenant ID is null or empty in ThreadContext");
            throw new IllegalStateException("Tenant ID not found in ThreadContext");
        }
        return tenantMongoTemplateProvider.getMongoTemplate(tenantId);
    }

    /**
     * Gets the MongoTemplate for a specific tenant ID.
     */
    private MongoTemplate getMongoTemplate(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("Tenant ID is null or empty");
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        return tenantMongoTemplateProvider.getMongoTemplate(tenantId);
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "translateConfigs", allEntries = true)
    public TranslateConfig save(TranslateConfig translateConfig) throws BodyValidationException {
        String businessId = translateConfig.getBusinessId();
        log.debug("Saving translation config - BusinessId: {}, ConfigId: {}", businessId, translateConfig.getConfigId());
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        Query query = new Query(Criteria.where(Constants.MONGO_BUSINESS_ID_FIELD).is(businessId));
        boolean exists = mongoTemplate.exists(query, TranslateConfig.class);
        
        if (exists) {
            log.warn("Translation config already exists - BusinessId: {}", businessId);
            throw new CustomException(
                    "Translation configuration already exists",
                    HttpStatus.BAD_REQUEST, 
                    Constants.DUP_BUSINESSID);
        }
        
        TranslateConfig savedConfig = mongoTemplate.save(translateConfig);
        log.info("Translation config saved successfully - BusinessId: {}, ConfigId: {}", 
                businessId, savedConfig.getConfigId());
        return savedConfig;
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(
            value = "translateConfigs",
            key = "#tenantId + '_' + #businessId",
            unless = "#result == null"
    )
    public TranslateConfig getConfigurationDeatils(String tenantId, String businessId) {
        log.debug("Fetching translation config - TenantId: {}, BusinessId: {}", tenantId, businessId);
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        Query query = new Query(
                Criteria.where(Constants.MONGO_TENANT_ID_FIELD).is(tenantId)
                        .and(Constants.MONGO_BUSINESS_ID_FIELD).is(businessId)
        );
        query.fields().exclude("_id");

        TranslateConfig config = mongoTemplate.findOne(query, TranslateConfig.class);

        // Fallback: If not found, retry using tenantId as businessId
        if (config == null) {
            log.debug("Config not found with businessId, trying fallback with tenantId as businessId - TenantId: {}", tenantId);
            Query fallbackQuery = new Query(
                    Criteria.where(Constants.MONGO_TENANT_ID_FIELD).is(tenantId)
                            .and(Constants.MONGO_BUSINESS_ID_FIELD).is(tenantId)
            );
            fallbackQuery.fields().exclude("_id");
            config = mongoTemplate.findOne(fallbackQuery, TranslateConfig.class);
        }

        if (config != null) {
            log.debug("Translation config found - TenantId: {}, BusinessId: {}, ConfigId: {}", 
                    tenantId, businessId, config.getConfigId());
        } else {
            log.debug("Translation config not found - TenantId: {}, BusinessId: {}", tenantId, businessId);
        }
        return config;
    }

    @Override
    public List<TranslateConfig> getAllConfig(String tenantId, String businessId, String provider) {
        log.debug("Fetching all translation configs - TenantId: {}, BusinessId: {}, Provider: {}", 
                tenantId, businessId, provider);
        
        MongoTemplate mongoTemplate = getMongoTemplate(tenantId);
        Criteria criteria = Criteria.where(Constants.MONGO_TENANT_ID_FIELD).is(tenantId);

        if (businessId != null && !businessId.isEmpty()) {
            criteria = criteria.and(Constants.MONGO_BUSINESS_ID_FIELD).is(businessId);
        }

        if (provider != null && !provider.isEmpty()) {
            criteria = criteria.and(Constants.MONGO_CONFIG_PROVIDER_FIELD).is(provider);
        }

        Query query = new Query(criteria);
        query.fields().exclude("id", "updatedAt", "createdAt");
        
        List<TranslateConfig> configs = mongoTemplate.find(query, TranslateConfig.class);
        log.debug("Found {} translation config(s) - TenantId: {}, BusinessId: {}, Provider: {}", 
                configs.size(), tenantId, businessId, provider);
        return configs;
    }



    @Override
    public boolean checkBusinessExists(String businessId) {
        log.debug("Checking if business exists - BusinessId: {}", businessId);
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        Query query = new Query(Criteria.where(Constants.MONGO_BUSINESS_ID_FIELD).is(businessId));
        boolean exists = mongoTemplate.exists(query, BusinessApplication.class);
        
        log.debug("Business existence check - BusinessId: {}, Exists: {}", businessId, exists);
        return exists;
    }

    @Override
    public boolean checkAlreadyExist(String businessId, String provider) {
        log.debug("Checking if config already exists - BusinessId: {}, Provider: {}", businessId, provider);
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        Query query = new Query(
                Criteria.where(Constants.MONGO_BUSINESS_ID_FIELD).is(businessId)
                        .and(Constants.MONGO_CONFIG_PROVIDER_FIELD).is(provider)
        );
        boolean exists = mongoTemplate.exists(query, TranslateConfig.class);
        
        log.debug("Config existence check - BusinessId: {}, Provider: {}, Exists: {}", businessId, provider, exists);
        return exists;
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "translateConfigs", allEntries = true)
    public TranslateConfig updateConfig(String businessId, String scopeLevel, Config config) {
        log.debug("Updating translation config - BusinessId: {}, Provider: {}, ScopeLevel: {}", 
                businessId, config.getProvider(), scopeLevel);
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        Query query = new Query(
                Criteria.where(Constants.MONGO_BUSINESS_ID_FIELD).is(businessId)
                        .and(Constants.MONGO_CONFIG_PROVIDER_FIELD).is(config.getProvider())
        );
        
        TranslateConfig existingConfig = mongoTemplate.findOne(query, TranslateConfig.class);
        if (existingConfig == null) {
            log.error("Config not found for update - BusinessId: {}, Provider: {}", businessId, config.getProvider());
            String errorMessage = "Config not found for businessId: " + businessId + " and provider: " + config.getProvider();
            throw new IllegalStateException(errorMessage);
        }
        
        Update update = buildUpdateObject(scopeLevel, config);
        TranslateConfig updatedConfig = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), TranslateConfig.class);
        
        log.info("Translation config updated successfully - BusinessId: {}, Provider: {}, ConfigId: {}", 
                businessId, config.getProvider(), updatedConfig.getConfigId());
        return updatedConfig;
    }

    /**
     * Builds the MongoDB Update object with all config fields.
     * Reduces cyclomatic complexity by extracting logic.
     */
    private Update buildUpdateObject(String scopeLevel, Config config) {
        Update update = new Update()
                .set("scopeLevel", scopeLevel)
                .set("config.apiBaseUrl", config.getApiBaseUrl());
        
        setIfNotNull(update, "config.modelPipelineEndpoint", config.getModelPipelineEndpoint());
        setIfNotNull(update, "config.callbackUrl", config.getCallbackUrl());
        setIfNotNull(update, "config.userId", config.getUserId());
        setIfNotNull(update, "config.apiKey", config.getApiKey());
        setIfNotNull(update, "config.pipelineId", config.getPipelineId());
        setIfNotNull(update, "config.subscriptionKey", config.getSubscriptionKey());
        setIfNotNull(update, "config.region", config.getRegion());
        setIfNotNull(update, "config.endpoint", config.getEndpoint());
        
        return update;
    }

    /**
     * Helper method to set update field only if value is not null.
     */
    private void setIfNotNull(Update update, String field, String value) {
        if (value != null) {
            update.set(field, value);
        }
    }

    @Override
    public long getCount(String tenantId) {
        log.debug("Getting translation config count - TenantId: {}", tenantId);
        
        MongoTemplate mongoTemplate = getMongoTemplate(tenantId);
        Query query = new Query(Criteria.where(Constants.MONGO_TENANT_ID_FIELD).is(tenantId));
        long count = mongoTemplate.count(query, TranslateConfig.class);
        
        log.debug("Translation config count - TenantId: {}, Count: {}", tenantId, count);
        return count;
    }

}
