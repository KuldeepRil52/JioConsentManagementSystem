package com.jio.vault.repository.mongotemplate;

import com.mongodb.client.MongoClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class DynamicMongoTemplate {

    private final MongoClient mongoClient;

    public DynamicMongoTemplate(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    /**
     * Returns a MongoTemplate bound to the given database name.
     */
    public MongoTemplate getTemplate(String dbName) {
        return new MongoTemplate(mongoClient, dbName);
    }
}
