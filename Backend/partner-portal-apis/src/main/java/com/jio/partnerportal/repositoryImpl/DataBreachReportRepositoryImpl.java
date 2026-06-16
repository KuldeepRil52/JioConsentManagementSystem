package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.DataBreachReport;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.DataBreachReportRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DataBreachReportRepositoryImpl implements DataBreachReportRepository {

    private final TenantMongoTemplateProvider mongoTemplateProvider;

    @Autowired
    public DataBreachReportRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public DataBreachReport save(DataBreachReport report) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(report);
    }

    @Override
    public DataBreachReport findById(String id) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = new Query(Criteria.where("incidentId").is(id));
        return mongoTemplate.findOne(query, DataBreachReport.class);
    }

    @Override
    public List<DataBreachReport> findAllByTenantId(String tenantId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = new Query(Criteria.where("tenantId").is(tenantId));
        return mongoTemplate.find(query, DataBreachReport.class);
    }

    @Override
    public DataBreachReport findLatestByTenantId(String tenantId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Query query = new Query(Criteria.where("tenantId").is(tenantId));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.limit(1);
        return mongoTemplate.findOne(query, DataBreachReport.class);
    }
}
