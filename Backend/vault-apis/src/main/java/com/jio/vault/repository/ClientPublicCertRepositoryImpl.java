package com.jio.vault.repository;

import com.jio.vault.constants.CollectionConstants;
import com.jio.vault.documents.ClientPublicCert;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.jio.vault.repository.mongotemplate.DynamicMongoTemplate;
import java.util.List;
import java.util.Optional;

@Repository
public class ClientPublicCertRepositoryImpl implements ClientPublicCertRepositoryCustom {

    private final DynamicMongoTemplate dynamicMongoTemplate;

    public ClientPublicCertRepositoryImpl(DynamicMongoTemplate dynamicMongoTemplate) {
        this.dynamicMongoTemplate = dynamicMongoTemplate;
    }

    @Override
    public Optional<ClientPublicCert> findByBusinessIdAndTenantIdDynamic(
            String dbName, String businessId, String tenantId) {

        MongoTemplate template = dynamicMongoTemplate.getTemplate(dbName);

        Query query = new Query();
        query.addCriteria(Criteria.where("businessId").is(businessId)
                .and("tenantId").is(tenantId));

        return Optional.ofNullable(
                template.findOne(query, ClientPublicCert.class, CollectionConstants.CLIENT_PUBLIC_CERT)
        );
    }

    @Override
    public List<ClientPublicCert> findByBusinessIdAndTenantId(
            String dbName, String businessId, String tenantId) {

        MongoTemplate template = dynamicMongoTemplate.getTemplate(dbName);

        Query query = new Query();
        query.addCriteria(Criteria.where("businessId").is(businessId)
                .and("tenantId").is(tenantId));

        return template.find(query, ClientPublicCert.class, CollectionConstants.CLIENT_PUBLIC_CERT);
    }

    @Override
    public Optional<ClientPublicCert> findByKeyId(String dbName, String keyId) {
        MongoTemplate template = dynamicMongoTemplate.getTemplate(dbName);

        Query query = new Query();
        query.addCriteria(Criteria.where("keyId").is(keyId));

        return Optional.ofNullable(
                template.findOne(query, ClientPublicCert.class, CollectionConstants.CLIENT_PUBLIC_CERT)
        );
    }

    @Override
    public ClientPublicCert save(String dbName, ClientPublicCert cert) {
        MongoTemplate template = dynamicMongoTemplate.getTemplate(dbName);
        return template.save(cert);  // This will create the collection if it doesn’t exist
    }
}
