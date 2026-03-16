package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.ProcessorActivity;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.ProcessorActivityRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ProcessorActivityRepositoryImpl implements ProcessorActivityRepository {

    private final TenantMongoTemplateProvider mongoTemplateProvider;

    public ProcessorActivityRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider){
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public ProcessorActivity save(ProcessorActivity processorActivity) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(processorActivity);    }

    @Override
    public ProcessorActivity findByProcessorActivityId(String processorActivityId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("processorActivityId").is(processorActivityId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ProcessorActivity.class);
    }

    @Override
    public List<ProcessorActivity> findByProcessorActivityIds(List<String> processorActivityIds) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("processorActivityId").in(processorActivityIds);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, ProcessorActivity.class);
    }

    @Override
    public List<ProcessorActivity> findProcessorActivityByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, ProcessorActivity.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), ProcessorActivity.class);
    }

    @Override
    public ProcessorActivity findLatestByProcessorActivityId(String processorActivityId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("processorActivityId").is(processorActivityId);
        Query query = new Query();
        query.addCriteria(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "version"));
        return mongoTemplate.findOne(query, ProcessorActivity.class);
    }

    @Override
    public boolean existsByActivityName(String activityName) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("activityName").is(activityName);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, ProcessorActivity.class);
    }

    @Override
    public boolean existsByActivityNameExcludingProcessorActivityId(String activityName, String excludeProcessorActivityId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("activityName").is(activityName);
        criteria.and("processorActivityId").ne(excludeProcessorActivityId);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, ProcessorActivity.class);
    }

}
