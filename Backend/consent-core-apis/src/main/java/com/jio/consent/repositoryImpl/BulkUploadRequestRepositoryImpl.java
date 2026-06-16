package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.dto.BulkConsentUploadStatus;
import com.jio.consent.entity.BulkUploadRequest;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.BulkUploadRequestRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BulkUploadRequestRepositoryImpl implements BulkUploadRequestRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public BulkUploadRequestRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    private MongoTemplate getMongoTemplate() {
        return this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
    }

    @Override
    public BulkUploadRequest save(BulkUploadRequest bulkUploadRequest) {
        return getMongoTemplate().save(bulkUploadRequest);
    }

    @Override
    public BulkUploadRequest getByTransactionId(String transactionId) {
        Query query = new Query(Criteria.where("transactionId").is(transactionId));
        return getMongoTemplate().findOne(query, BulkUploadRequest.class);
    }

    @Override
    public BulkUploadRequest updateStatus(String transactionId, BulkConsentUploadStatus status, 
                                          int successCount, int failedCount) {
        Query query = new Query(Criteria.where("transactionId").is(transactionId));
        Update update = new Update()
                .set("status", status)
                .set("successCount", successCount)
                .set("failedCount", failedCount)
                .set("updatedAt", LocalDateTime.now());

        getMongoTemplate().updateFirst(query, update, BulkUploadRequest.class);
        return getByTransactionId(transactionId);
    }

    @Override
    public List<BulkUploadRequest> findByConsentTxnId(String txnId) {
        Query query = new Query(Criteria.where("consents.txnId").is(txnId));
        return getMongoTemplate().find(query, BulkUploadRequest.class);
    }
}
