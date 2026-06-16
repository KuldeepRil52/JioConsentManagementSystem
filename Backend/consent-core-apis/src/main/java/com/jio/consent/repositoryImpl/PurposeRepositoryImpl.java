package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.Purpose;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.PurposeRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PurposeRepositoryImpl implements PurposeRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public PurposeRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public List<Purpose> findByPurposeIds(List<String> purposeIds) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = Criteria.where("purposeId").in(purposeIds);
        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, Purpose.class);
    }
}


