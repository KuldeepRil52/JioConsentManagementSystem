package com.jio.consent.repository;

import com.jio.consent.dto.BulkConsentUploadStatus;
import com.jio.consent.entity.BulkUploadRequest;

import java.util.List;

public interface BulkUploadRequestRepository {

    BulkUploadRequest save(BulkUploadRequest bulkUploadRequest);

    BulkUploadRequest getByTransactionId(String transactionId);

    BulkUploadRequest updateStatus(String transactionId, BulkConsentUploadStatus status, int successCount, int failedCount);

    List<BulkUploadRequest> findByConsentTxnId(String txnId);
}
