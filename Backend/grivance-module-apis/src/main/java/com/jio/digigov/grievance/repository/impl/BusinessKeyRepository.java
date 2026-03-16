package com.jio.digigov.grievance.repository.impl;
import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BusinessKeyRepository {

    private static final String COLLECTION_BUSINESS_KEYS = "business_keys";
    private static final String COLLECTION_SYSTEM_CONFIG = "system_configurations";
    private static final String BUSINESSID = "businessId";
    private static final String COLLECTION_DATA_PROCESSORS = "data_processors";

    @Autowired
    private MultiTenantMongoConfig tenantMongoTemplateFactory;

//    public BusinessKey findByBusinessId(String tenantId, String businessId) {
//        MongoTemplate template = tenantMongoTemplateFactory.getMongoTemplateForTenant(tenantId);
//        Query query = new Query(Criteria.where(BUSINESSID).is(businessId));
//        return template.findOne(query, BusinessKey.class, COLLECTION_BUSINESS_KEYS);
//    }

    public String findCertificateByBusinessId(String tenantId, String businessId) {
        MongoTemplate template = tenantMongoTemplateFactory.getMongoTemplateForTenant(tenantId);

        Query query = new Query(Criteria.where("businessId").is(businessId));
        query.fields().include("configurationJson.sslCertificate");

        Document doc = template.findOne(query, Document.class, COLLECTION_SYSTEM_CONFIG);
        if (doc == null) {
            log.warn("No configuration document found for businessId={} in tenant={}", businessId, tenantId);
            return null;
        }

        Object configObj = doc.get("configurationJson");
        if (!(configObj instanceof Document)) {
            log.warn("configurationJson not found or invalid for businessId={} in tenant={}", businessId, tenantId);
            return null;
        }

        Document configJson = (Document) configObj;
        String certificate = configJson.getString("sslCertificate");

        if (certificate == null || certificate.isBlank()) {
            log.warn("sslCertificate not found or empty for businessId={} in tenant={}", businessId, tenantId);
            return null;
        }

        return certificate;
    }

    public String findCertOfDataProcessor(String tenantId, String dataProcessorId) {
        MongoTemplate template = tenantMongoTemplateFactory.getMongoTemplateForTenant(tenantId);

        Query query = new Query(Criteria.where("dataProcessorId").is(dataProcessorId));
        query.fields().include("attachment");

        Document doc = template.findOne(query, Document.class, COLLECTION_DATA_PROCESSORS);
        if (doc == null) {
            log.warn("No data processor found for dataProcessorId={} in tenant={}", dataProcessorId, tenantId);
            return null;
        }

        String certificate = doc.getString("attachment");
        if (certificate == null || certificate.isBlank()) {
            log.warn("Attachment (certificate) not found or empty for dataProcessorId={} in tenant={}", dataProcessorId, tenantId);
            return null;
        }
        return certificate;
    }
}
