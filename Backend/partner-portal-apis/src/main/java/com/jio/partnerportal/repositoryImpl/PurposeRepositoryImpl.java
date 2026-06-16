package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;

import com.jio.partnerportal.entity.Purpose;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.PurposeRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class PurposeRepositoryImpl implements PurposeRepository {

    private final TenantMongoTemplateProvider mongoTemplateProvider;

    public PurposeRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider){
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public Purpose save(Purpose purpose) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(purpose);    }

    @Override
    public Purpose findByPurposeId(String purposeId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("purposeId").is(purposeId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, Purpose.class);
    }

    @Override
    public Purpose findByPurposeCode(String purposeCode) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("purposeCode").is(purposeCode);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, Purpose.class);
    }

    @Override
    public List<Purpose> findByPurposeIds(List<String> purposeIds) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("purposeId").in(purposeIds);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, Purpose.class);
    }

    @Override
    public List<Purpose> findPurposeByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, Purpose.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), Purpose.class);
    }

    @Override
    public boolean existsByPurposeName(String purposeName) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("purposeName").is(purposeName);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, Purpose.class);
    }

    @Override
    public boolean existsByPurposeNameExcludingPurposeId(String purposeName, String excludePurposeId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("purposeName").is(purposeName);
        criteria.and("purposeId").ne(excludePurposeId);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, Purpose.class);
    }
}
