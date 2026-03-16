package com.jio.multitranslator.repositoryimpl;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.entity.TranslateToken;
import com.jio.multitranslator.entity.TranslateTranscation;
import com.jio.multitranslator.multitenancy.TenantMongoTemplateProvider;
import com.jio.multitranslator.repository.TranslateRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository implementation for translation token and transaction operations.
 */
@Slf4j
@Repository
public class TranslateRepositoryImpl implements TranslateRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    public TranslateRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
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

    @Override
    @org.springframework.cache.annotation.Cacheable(
            value = "translateTokens",
            key = "T(String).valueOf(#businessId) + '_' + T(String).valueOf(#sourceLanguage) + '_' + T(String).valueOf(#targetLanguage)",
            unless = "#result == null"
    )
    public TranslateToken getTokenFromDB(String sourceLanguage, String targetLanguage, String businessId) {
        log.debug("Fetching token from DB - BusinessId: {}, Source: {}, Target: {}", 
                businessId, sourceLanguage, targetLanguage);
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        Query query = new Query(
                Criteria.where(Constants.MONGO_BUSINESS_ID_FIELD).is(businessId)
                        .and("targetLanguage").is(targetLanguage)
                        .and("sourceLanguage").is(sourceLanguage)
        );
        query.fields().exclude("_id");
        
        TranslateToken token = mongoTemplate.findOne(query, TranslateToken.class);
        if (token != null) {
            log.debug("Token found in DB - BusinessId: {}, Source: {}, Target: {}", 
                    businessId, sourceLanguage, targetLanguage);
        } else {
            log.debug("Token not found in DB - BusinessId: {}, Source: {}, Target: {}", 
                    businessId, sourceLanguage, targetLanguage);
        }
        return token;
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(
            value = "translateTokens",
            key = "T(String).valueOf(#storeToken.businessId) + '_' + T(String).valueOf(#storeToken.sourceLanguage) + '_' + T(String).valueOf(#storeToken.targetLanguage)"
    )
    public TranslateToken save(TranslateToken storeToken) {
        log.debug("Saving token to DB - BusinessId: {}, Source: {}, Target: {}", 
                storeToken.getBusinessId(), storeToken.getSourceLanguage(), storeToken.getTargetLanguage());
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        TranslateToken savedToken = mongoTemplate.save(storeToken);
        log.info("Token saved successfully - BusinessId: {}, Source: {}, Target: {}", 
                savedToken.getBusinessId(), savedToken.getSourceLanguage(), savedToken.getTargetLanguage());
        return savedToken;
    }

    @Override
    public void saveTranslate(TranslateTranscation saveTranslate) {
        log.debug("Saving translation transaction - TXN: {}, Source: {}, Target: {}", 
                saveTranslate.getTxn(), saveTranslate.getSourceLanguage(), saveTranslate.getTargetLanguage());
        
        MongoTemplate mongoTemplate = getMongoTemplate();
        mongoTemplate.save(saveTranslate);
        log.debug("Translation transaction saved - TXN: {}", saveTranslate.getTxn());
    }
}
