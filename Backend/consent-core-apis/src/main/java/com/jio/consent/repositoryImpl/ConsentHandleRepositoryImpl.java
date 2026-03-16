package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.ConsentHandle;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.ConsentHandleRepository;
import org.apache.logging.log4j.ThreadContext;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ConsentHandleRepositoryImpl implements ConsentHandleRepository {

    TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public ConsentHandleRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }


    @Override
    public ConsentHandle save(ConsentHandle consentHandle) {
        return this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER)).save(consentHandle);
    }

    @Override
    public ConsentHandle getByConsentHandleId(String consentHandleId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("consentHandleId").is(consentHandleId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ConsentHandle.class);
    }

    @Override
    public List<ConsentHandle> findConsentHandleByParams(Map<String, Object> searchParams) {
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

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, ConsentHandle.class);
    }

    @Override
    public List<ConsentHandle> findConsentHandleByParamsWithIds(Map<String, Object> searchParams) {
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

        Query query = new Query(criteria);
        return mongoTemplate.find(query, ConsentHandle.class);
    }

    @Override
    public List<ConsentHandle> findConsentHandleByParamsWithPagination(Map<String, Object> searchParams, Pageable pageable) {
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

        Query query = new Query(criteria);
        query.with(pageable);
        return mongoTemplate.find(query, ConsentHandle.class);
    }

    @Override
    public void deleteById(ObjectId id) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, ConsentHandle.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), ConsentHandle.class);
    }

}
