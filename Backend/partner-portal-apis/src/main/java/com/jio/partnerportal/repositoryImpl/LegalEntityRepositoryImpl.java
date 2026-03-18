package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.LegalEntity;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.LegalEntityRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class LegalEntityRepositoryImpl implements LegalEntityRepository {

    private TenantMongoTemplateProvider mongoTemplateProvider;

    @Autowired
    public LegalEntityRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    public LegalEntity findLegalEntityByLegalEntityId(String legalEntityId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("legalEntityId").is(legalEntityId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, LegalEntity.class);
    }

    @Override
    public void updateLegalEntity(LegalEntity legalEntity) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        mongoTemplate.save(legalEntity);

    }

    @Override
    public List<LegalEntity> findLegalEntitiesByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, LegalEntity.class);

    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), LegalEntity.class);
    }
}
