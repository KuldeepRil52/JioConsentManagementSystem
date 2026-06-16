package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.ConsentMeta;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.ConsentMetaRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ConsentMetaRepositoryImpl implements ConsentMetaRepository {

    TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public ConsentMetaRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public ConsentMeta save(ConsentMeta consentMeta) {
        return this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER)).save(consentMeta);
    }

    @Override
    public ConsentMeta getByConsentMetaId(String consentMetaId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("consentMetaId").is(consentMetaId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ConsentMeta.class);
    }
}

