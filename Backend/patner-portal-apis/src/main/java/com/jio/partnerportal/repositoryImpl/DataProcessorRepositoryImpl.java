package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.DataProcessor;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.DataProcessorRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DataProcessorRepositoryImpl implements DataProcessorRepository {

    private TenantMongoTemplateProvider mongoTemplateProvider;

    public DataProcessorRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public DataProcessor save(DataProcessor dataProcessor) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(dataProcessor);
    }

    @Override
    public DataProcessor findByDataProcessorId(String dataProcessorId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("dataProcessorId").is(dataProcessorId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, DataProcessor.class);
    }

    @Override
    public List<DataProcessor> findDataProcessorByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, DataProcessor.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), DataProcessor.class);
    }

    @Override
    public boolean existsByDataProcessorName(String dataProcessorName) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("dataProcessorName").is(dataProcessorName);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, DataProcessor.class);
    }

    @Override
    public boolean existsByDataProcessorNameExcludingDataProcessorId(String dataProcessorName, String excludeDataProcessorId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("dataProcessorName").is(dataProcessorName);
        criteria.and("dataProcessorId").ne(excludeDataProcessorId);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, DataProcessor.class);
    }
}
