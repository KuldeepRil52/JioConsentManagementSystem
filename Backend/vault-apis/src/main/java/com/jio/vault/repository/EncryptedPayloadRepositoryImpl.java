package com.jio.vault.repository;

import com.jio.vault.constants.CollectionConstants;
import com.jio.vault.documents.EncryptedPayload;
import com.jio.vault.repository.mongotemplate.DynamicMongoTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EncryptedPayloadRepositoryImpl implements EncryptedPayloadRepositoryCustom {

    private final DynamicMongoTemplate dynamicMongoTemplate;

    public EncryptedPayloadRepositoryImpl(DynamicMongoTemplate dynamicMongoTemplate) {
        this.dynamicMongoTemplate = dynamicMongoTemplate;
    }

    @Override
    public Optional<EncryptedPayload> findByUuid(String dbName, String uuid) {
        MongoTemplate template = dynamicMongoTemplate.getTemplate(dbName);

        Query query = new Query();
        query.addCriteria(Criteria.where("uuid").is(uuid));

        return Optional.ofNullable(
                template.findOne(query, EncryptedPayload.class, CollectionConstants.ENCRYPTED_PAYLOAD)
        );
    }

    @Override
    public EncryptedPayload save(String dbName, EncryptedPayload payload) {
        MongoTemplate template = dynamicMongoTemplate.getTemplate(dbName);
        return template.save(payload, CollectionConstants.ENCRYPTED_PAYLOAD);
    }
}
