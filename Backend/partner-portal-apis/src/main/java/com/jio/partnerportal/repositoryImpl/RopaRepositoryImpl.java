package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.RopaRecord;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.RopaRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class RopaRepositoryImpl implements RopaRepository {

    private TenantMongoTemplateProvider mongoTemplateProvider;

    @Autowired
    public RopaRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public RopaRecord save(RopaRecord ropaRecord) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(ropaRecord);
    }

    @Override
    public RopaRecord findByRopaId(String ropaId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("ropaId").is(ropaId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, RopaRecord.class);
    }

    @Override
    public List<RopaRecord> findByBusinessId(String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, RopaRecord.class);
    }


    @Override
    public List<RopaRecord> findRopaByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, RopaRecord.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), RopaRecord.class);
    }

    @Override
    public boolean existsByRopaId(String ropaId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = Query.query(Criteria.where("ropaId").is(ropaId));
        return mongoTemplate.exists(query, RopaRecord.class);
    }

    @Override
    public void deleteByRopaId(String ropaId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("ropaId").is(ropaId);
        Query query = new Query();
        query.addCriteria(criteria);
        mongoTemplate.remove(query, RopaRecord.class);
    }

    @Override
    public List<RopaRecord> findAllRopa() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.findAll(RopaRecord.class);
    }
}
