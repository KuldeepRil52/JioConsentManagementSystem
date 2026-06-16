package com.jio.schedular.repositoryImpl;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.enums.ConsentHandleStatus;
import com.jio.schedular.entity.ConsentHandle;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.ConsentHandleRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    public ConsentHandle save(ConsentHandle consentHandle, String tenantId) {
        return this.tenantMongoTemplateProvider.getMongoTemplate(tenantId).save(consentHandle);
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
            criteria.and(entry.getKey()).is(entry.getValue());
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
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        return mongoTemplate.find(query, ConsentHandle.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), ConsentHandle.class);
    }

    @Override
    public List<ConsentHandle> findPendingConsentHandlesOlderThan(LocalDateTime expiryDate, Pageable pageable) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        
        Criteria criteria = new Criteria();
        criteria.and("status").is(ConsentHandleStatus.PENDING);
        criteria.and("createdAt").lt(expiryDate);
        
        Query query = new Query(criteria);
        query.with(pageable);
        
        return mongoTemplate.find(query, ConsentHandle.class);
    }

}
