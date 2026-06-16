package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.dto.ConsentStatus;
import com.jio.consent.dto.CustomerIdentifiers;
import com.jio.consent.dto.StaleStatus;
import com.jio.consent.entity.Consent;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.ConsentRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ConsentRepositoryImpl implements ConsentRepository {

    TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public ConsentRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public Consent save(Consent consent) {
        return tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER)).save(consent);

    }

    @Override
    public Consent getByConsentId(String consentId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("consentId").is(consentId);
        criteria.and("staleStatus").ne(StaleStatus.STALE);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, Consent.class);
    }

    @Override
    public Consent existByTemplateIdAndTemplateVersionAndCustomerIdentifiers(String templateId, int templateVersion, CustomerIdentifiers customerIdentifiers) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("templateId").is(templateId);
        criteria.and("templateVersion").is(templateVersion);
        criteria.and("customerIdentifiers.type").is(customerIdentifiers.getType());
        criteria.and("customerIdentifiers.value").is(customerIdentifiers.getValue());
        criteria.and("staleStatus").ne(StaleStatus.STALE);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, Consent.class);
    }

    @Override
    public List<Consent> findConsentByParams(Map<String, Object> searchParams) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Handle type conversion for specific fields
            if ("templateVersion".equals(key) && value instanceof String) {
                try {
                    int intValue = Integer.parseInt((String) value);
                    criteria.and(key).is(intValue);
                } catch (NumberFormatException e) {
                    // If conversion fails, skip this parameter
                    continue;
                }
            } else {
                criteria.and(key).is(value);
            }
        }
        
        // Exclude STALE status consents
        criteria.and("staleStatus").ne(StaleStatus.STALE);

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, Consent.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), Consent.class);
    }

    @Override
    public long countByParams(Map<String, Object> searchParams) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        
        Criteria criteria = new Criteria();
        
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Handle type conversion for specific fields
            if ("templateVersion".equals(key) && value instanceof String) {
                try {
                    int intValue = Integer.parseInt((String) value);
                    criteria.and(key).is(intValue);
                } catch (NumberFormatException e) {
                    // If conversion fails, skip this parameter
                    continue;
                }
            } else {
                criteria.and(key).is(value);
            }
        }
        
        // Exclude STALE status consents
        criteria.and("staleStatus").ne(StaleStatus.STALE);
        
        Query query = new Query(criteria);
        return mongoTemplate.count(query, Consent.class);
    }

    @Override
    public Map<ConsentStatus, Long> countByStatus(Map<String, Object> searchParams) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        
        Criteria criteria = new Criteria();
        
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Handle type conversion for specific fields
            if ("templateVersion".equals(key) && value instanceof String) {
                try {
                    int intValue = Integer.parseInt((String) value);
                    criteria.and(key).is(intValue);
                } catch (NumberFormatException e) {
                    // If conversion fails, skip this parameter
                    continue;
                }
            } else {
                criteria.and(key).is(value);
            }
        }
        
        // Exclude STALE status consents
        criteria.and("staleStatus").ne(StaleStatus.STALE);
        
        // Build aggregation pipeline: match -> group by status -> count
        MatchOperation matchOperation = Aggregation.match(criteria);
        GroupOperation groupOperation = Aggregation.group("status").count().as("count");
        
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
        
        @SuppressWarnings("unchecked")
        AggregationResults<Map<String, Object>> results = (AggregationResults<Map<String, Object>>) (AggregationResults<?>) mongoTemplate.aggregate(aggregation, "consents", Map.class);
        
        Map<ConsentStatus, Long> statusCounts = new HashMap<>();
        
        // Initialize all status types with 0 count
        for (ConsentStatus status : ConsentStatus.values()) {
            statusCounts.put(status, 0L);
        }
        
        // Update counts from aggregation results
        for (Map<String, Object> result : results.getMappedResults()) {
            String statusStr = (String) result.get("_id");
            if (statusStr != null) {
                try {
                    ConsentStatus status = ConsentStatus.valueOf(statusStr);
                    Integer count = (Integer) result.get("count");
                    statusCounts.put(status, count != null ? count.longValue() : 0L);
                } catch (IllegalArgumentException e) {
                    // Skip unknown status values
                }
            }
        }
        
        return statusCounts;
    }

    @Override
    public Consent findLatestByCreatedAt() {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.limit(1);
        return mongoTemplate.findOne(query, Consent.class);
    }
    
    @Override
    public Consent findById(org.bson.types.ObjectId id) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.findById(id, Consent.class);
    }
    
    @Override
    public Consent getActiveByConsentId(String consentId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("consentId").is(consentId);
        criteria.and("status").is(ConsentStatus.ACTIVE);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, Consent.class);
    }
}
