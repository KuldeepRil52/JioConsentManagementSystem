package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.DigilockerConfig;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.DigilockerConfigRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DigilockerConfigRepositoryImpl implements DigilockerConfigRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public DigilockerConfigRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public Optional<DigilockerConfig> findFirstByBusinessIdAndStatus(String businessId, String status) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        criteria.and("status").is(status);
        Query query = new Query(criteria);
        DigilockerConfig config = mongoTemplate.findOne(query, DigilockerConfig.class);
        return Optional.ofNullable(config);
    }
}

