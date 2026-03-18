package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.ClientCredentials;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.ClientCredentialsRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ClientCredentialsRepositoryImpl implements ClientCredentialsRepository {

    private final TenantMongoTemplateProvider mongoTemplateProvider;

    public ClientCredentialsRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public ClientCredentials save(ClientCredentials clientCredentials) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(clientCredentials);
    }

    @Override
    public ClientCredentials findByBusinessId(String businessId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ClientCredentials.class);
    }

    @Override
    public ClientCredentials findByBusinessIdAndScopeLevel(String businessId, String scopeLevel) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessId").is(businessId);
        criteria.and("scopeLevel").is(scopeLevel);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ClientCredentials.class);
    }

    @Override
    public ClientCredentials findByBusinessUniqueId(String businessUniqueId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("businessUniqueId").is(businessUniqueId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ClientCredentials.class);
    }

    @Override
    public ClientCredentials findByConsumerKey(String consumerKey) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("consumerKey").is(consumerKey);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ClientCredentials.class);
    }

    @Override
    public List<ClientCredentials> findClientCredentialsByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, ClientCredentials.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), ClientCredentials.class);
    }
}

