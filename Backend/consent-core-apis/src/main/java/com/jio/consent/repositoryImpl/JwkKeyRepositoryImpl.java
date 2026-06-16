package com.jio.consent.repositoryImpl;

import com.jio.consent.entity.JwkKey;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.signature.JwkKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementation of JwkKeyRepository for tenant-specific queries.
 */
@Slf4j
@Repository
public class JwkKeyRepositoryImpl implements JwkKeyRepository {

    @Autowired
    private TenantMongoTemplateProvider mongoTemplateProvider;

    /**
     * Finds the first JWK key by tenant ID, key type, and use.
     */
    public JwkKey findFirstByTenantIdAndKtyAndUse(String tenantId, String kty, String use) {
        MongoTemplate template = mongoTemplateProvider.getMongoTemplate(tenantId);
        Query query = new Query(Criteria.where("tenantId").is(tenantId)
                .and("kty").is(kty)
                .and("use").is(use));
        query.limit(1);
        return template.findOne(query, JwkKey.class);
    }

    /**
     * Finds the first JWK key by tenant ID and use.
     */
    public JwkKey findFirstByTenantIdAndUse(String tenantId, String use) {
        MongoTemplate template = mongoTemplateProvider.getMongoTemplate(tenantId);
        Query query = new Query(Criteria.where("tenantId").is(tenantId)
                .and("use").is(use));
        query.limit(1);
        return template.findOne(query, JwkKey.class);
    }

    /**
     * Finds the first JWK key by tenant ID, ordered by kid ascending.
     */
    public JwkKey findFirstByTenantIdOrderByKidAsc(String tenantId) {
        MongoTemplate template = mongoTemplateProvider.getMongoTemplate(tenantId);
        Query query = new Query(Criteria.where("tenantId").is(tenantId));
        query.with(Sort.by(Sort.Direction.ASC, "kid"));
        query.limit(1);
        return template.findOne(query, JwkKey.class);
    }

    /**
     * Finds all JWK keys by tenant ID.
     */
    public List<JwkKey> findByTenantId(String tenantId) {
        MongoTemplate template = mongoTemplateProvider.getMongoTemplate(tenantId);
        Query query = new Query(Criteria.where("tenantId").is(tenantId));
        return template.find(query, JwkKey.class);
    }
}

