package com.jio.vault.service;

import com.jio.vault.constants.CollectionConstants;
import com.jio.vault.constants.ErrorCode;
import com.jio.vault.documents.ClientPublicCert;
import com.jio.vault.dto.OnboardCertResponse;
import com.jio.vault.exception.CustomException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.bson.Document;
import java.util.ArrayList;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
@Service
public class TenantValidationService {

    private final MongoClient mongoClient;

    public TenantValidationService(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }
    public void validateTenantAndBusiness(String tenantId, String businessId) {

        String dbName = "tenant_db_" + tenantId;
        boolean dbExists = mongoClient.listDatabaseNames()
                .into(new ArrayList<>())
                .contains(dbName);

        if (!dbExists) {
            throw new CustomException(ErrorCode.TENANT_ID_EMPTY,
                    "Tenant-ID does not exist: " + tenantId);
        }

        MongoDatabase db = mongoClient.getDatabase(dbName);
        Document business = db.getCollection(CollectionConstants.BUSINESS_APPS)
                .find(eq("businessId", businessId))
                .first();

        if (business == null) {
            throw new CustomException(ErrorCode.BUSINESS_ID_EMPTY,
                    "Business-ID does not exist in tenant: " + businessId);
        }
    }
    public OnboardCertResponse validateCert(String tenantId, String businessId) {
        String dbName = "tenant_db_" + tenantId;
        MongoDatabase db = mongoClient.getDatabase(dbName);

        boolean dbExists = mongoClient.listDatabaseNames()
                .into(new ArrayList<>())
                .contains(dbName);

        if (!dbExists) {
            throw new CustomException(ErrorCode.TENANT_ID_EMPTY,
                    "Tenant-ID does not exist: " + tenantId);
        }

        try {
            Document business = db.getCollection(CollectionConstants.BUSINESS_APPS)
                    .find(eq("businessId", businessId))
                    .first();

            if (business == null) {
                throw new CustomException(ErrorCode.BUSINESS_ID_EMPTY,
                        "Business-ID does not exist in tenant: " + businessId);
            }
            Document certDoc = db.getCollection(CollectionConstants.CLIENT_PUBLIC_CERT)
                    .find(eq("businessId", businessId))
                    .first();

            if (certDoc != null) {
                OnboardCertResponse existingCert = new OnboardCertResponse();
                existingCert.setTenantId(certDoc.getString("tenantId"));
                existingCert.setBusinessId(certDoc.getString("businessId"));
                existingCert.setPublicKeyPem(certDoc.getString("publicKeyPem"));
                existingCert.setCertType(certDoc.getString("certType"));
                existingCert.setMessage("The BusinessID has already been onboarded");
                return existingCert;
            }

            return null;

        } catch (MongoException e) {
            log.error("Mongo DB exception occured {}", e.getMessage());
            throw new CustomException(ErrorCode.TENANT_ID_EMPTY,
                    "Tenant-ID does not exist: " + tenantId);
        }
    }

}

