package com.jio.consent.service;

import com.jio.consent.constant.Constants;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.*;
import com.jio.consent.dto.Request.BulkConsentItemRequest;
import com.jio.consent.dto.Request.BulkConsentRequest;
import com.jio.consent.dto.Request.CreateConsentRequest;
import com.jio.consent.dto.Request.CreateHandleRequest;
import com.jio.consent.dto.Response.BulkConsentResponse;
import com.jio.consent.dto.Response.BulkConsentStatusResponse;
import com.jio.consent.dto.Response.ConsentCreateResponse;
import com.jio.consent.entity.BulkConsentUpload;
import com.jio.consent.entity.BulkUploadRequest;
import com.jio.consent.entity.ConsentHandle;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.repository.BulkUploadRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BulkConsentService {

    private final BulkUploadRequestRepository bulkUploadRequestRepository;
    private final ConsentHandleService consentHandleService;
    private final ConsentService consentService;

    @Autowired
    public BulkConsentService(BulkUploadRequestRepository bulkUploadRequestRepository,
                              ConsentHandleService consentHandleService,
                              ConsentService consentService) {
        this.bulkUploadRequestRepository = bulkUploadRequestRepository;
        this.consentHandleService = consentHandleService;
        this.consentService = consentService;
    }

    public BulkConsentResponse processBulkConsent(BulkConsentRequest request, Map<String, String> headers) throws ConsentException {
        String transactionId = UUID.randomUUID().toString();
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);

        log.info("Processing bulk consent request with transactionId: {}, tenantId: {}, totalItems: {}", 
                transactionId, tenantId, request.getConsents().size());

        List<BulkConsentUpload> consentUploads = new ArrayList<>();
        List<String> duplicateTxnIds = new ArrayList<>();

        for (BulkConsentItemRequest item : request.getConsents()) {
            // Check idempotency - if txnId already exists, skip this item
            if (checkTxnIdExists(item.getTxnId())) {
                log.warn("Duplicate txnId found: {}, skipping this consent item", item.getTxnId());
                duplicateTxnIds.add(item.getTxnId());
                continue;
            }

            BulkConsentUpload consentUpload = BulkConsentUpload.builder()
                    .txnId(item.getTxnId())
                    .templateId(item.getTemplateId())
                    .templateVersion(item.getTemplateVersion())
                    .identityType(item.getIdentityType())
                    .identityValue(item.getIdentityValue())
                    .requestTimestamp(item.getTimeStamp() != null ? item.getTimeStamp() : LocalDateTime.now())
                    .preferences(convertPreferences(item.getPreferences()))
                    .status(BulkConsentUploadStatus.PENDING)
                    .build();

            consentUploads.add(consentUpload);
        }

        if (consentUploads.isEmpty()) {
            log.warn("All consent items are duplicates for transactionId: {}", transactionId);
            return BulkConsentResponse.builder()
                    .transactionId(transactionId)
                    .status("ACCEPTED")
                    .message("All consent items are duplicates. No new consents to process.")
                    .build();
        }

        BulkUploadRequest bulkUploadRequest = BulkUploadRequest.builder()
                .transactionId(transactionId)
                .tenantId(tenantId)
                .totalCount(consentUploads.size())
                .successCount(0)
                .failedCount(0)
                .status(BulkConsentUploadStatus.ACCEPTED)
                .consents(consentUploads)
                .build();

        bulkUploadRequestRepository.save(bulkUploadRequest);
        log.info("Saved bulk upload request with transactionId: {}, status: ACCEPTED, items: {}, duplicatesSkipped: {}", 
                transactionId, consentUploads.size(), duplicateTxnIds.size());

        processConsentsAsync(transactionId, tenantId, headers);

        return BulkConsentResponse.builder()
                .transactionId(transactionId)
                .status("ACCEPTED")
                .message("Bulk consent request accepted for processing")
                .build();
    }

    private boolean checkTxnIdExists(String txnId) {
        List<BulkUploadRequest> existingRequests = bulkUploadRequestRepository.findByConsentTxnId(txnId);
        return existingRequests != null && !existingRequests.isEmpty();
    }

    @Async
    public void processConsentsAsync(String transactionId, String tenantId, Map<String, String> headers) {
        log.info("Starting async processing for transactionId: {}", transactionId);

        try {
            ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);

            BulkUploadRequest bulkRequest = bulkUploadRequestRepository.getByTransactionId(transactionId);
            if (bulkRequest == null) {
                log.error("Bulk upload request not found for transactionId: {}", transactionId);
                return;
            }

            bulkUploadRequestRepository.updateStatus(transactionId, BulkConsentUploadStatus.PROCESSING, 0, 0);

            int successCount = 0;
            int failedCount = 0;

            List<BulkConsentUpload> consents = bulkRequest.getConsents();
            for (int i = 0; i < consents.size(); i++) {
                BulkConsentUpload consent = consents.get(i);
                try {
                    processSingleConsent(consent, headers);
                    consent.setStatus(BulkConsentUploadStatus.SUCCESS);
                    successCount++;
                    log.info("Successfully processed consent {}/{} for transactionId: {}", 
                            i + 1, consents.size(), transactionId);
                } catch (Exception e) {
                    consent.setStatus(BulkConsentUploadStatus.FAILED);
                    consent.setErrorMessage(e.getMessage());
                    failedCount++;
                    log.error("Failed to process consent {}/{} for transactionId: {}, error: {}", 
                            i + 1, consents.size(), transactionId, e.getMessage());
                }
            }

            bulkRequest.setConsents(consents);
            bulkRequest.setSuccessCount(successCount);
            bulkRequest.setFailedCount(failedCount);
            bulkRequest.setStatus(BulkConsentUploadStatus.COMPLETED);
            bulkUploadRequestRepository.save(bulkRequest);

            log.info("Completed async processing for transactionId: {}. Success: {}, Failed: {}", 
                    transactionId, successCount, failedCount);

        } catch (Exception e) {
            log.error("Error during async processing for transactionId: {}, error: {}", 
                    transactionId, e.getMessage(), e);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void processSingleConsent(BulkConsentUpload upload, Map<String, String> headers) 
            throws Exception {
        
        Integer templateVersion = null;
        try {
            templateVersion = Integer.parseInt(upload.getTemplateVersion());
        } catch (NumberFormatException e) {
            log.warn("Could not parse template version '{}' as integer, will use latest version", 
                    upload.getTemplateVersion());
        }

        CustomerIdentifiers customerIdentifiers = CustomerIdentifiers.builder()
                .type(upload.getIdentityType())
                .value(upload.getIdentityValue())
                .build();

        CreateHandleRequest handleRequest = CreateHandleRequest.builder()
                .templateId(upload.getTemplateId())
                .templateVersion(templateVersion)
                .customerIdentifiers(customerIdentifiers)
                .remarks(ConsentHandleRemarks.DATA_FIDUCIARY)
                .build();

        Map<String, String> handleHeaders = new HashMap<>(headers);
        handleHeaders.put(Constants.TXN_ID, upload.getTxnId());

        log.info("Creating consent handle for txnId: {}", upload.getTxnId());
        ConsentHandle consentHandle = consentHandleService.createConsentHandle(handleRequest, handleHeaders);
        upload.setConsentHandleId(consentHandle.getConsentHandleId());
        log.info("Created consent handle: {} for txnId: {}", 
                consentHandle.getConsentHandleId(), upload.getTxnId());

        Map<String, PreferenceStatus> preferencesStatus = new HashMap<>();
        for (BulkConsentUpload.BulkConsentPreference pref : upload.getPreferences()) {
            PreferenceStatus status = pref.getAction() == BulkConsentPreferenceAction.ACCEPT 
                    ? PreferenceStatus.ACCEPTED 
                    : PreferenceStatus.NOTACCEPTED;
            preferencesStatus.put(pref.getPreferenceId(), status);
        }

        CreateConsentRequest consentRequest = CreateConsentRequest.builder()
                .consentHandleId(consentHandle.getConsentHandleId())
                .preferencesStatus(preferencesStatus)
                .languagePreference(LANGUAGE.ENGLISH)
                .isParentalConsent(false)
                .build();

        log.info("Creating consent for txnId: {}, handle: {}", 
                upload.getTxnId(), consentHandle.getConsentHandleId());
        ConsentCreateResponse consentResponse = consentService.createConsentByConsentHandleId(consentRequest);
        upload.setConsentId(consentResponse.getConsentId());
        log.info("Created consent: {} for txnId: {}", 
                consentResponse.getConsentId(), upload.getTxnId());
    }

    public BulkConsentStatusResponse getBulkConsentStatus(String transactionId) throws ConsentException {
        BulkUploadRequest bulkRequest = bulkUploadRequestRepository.getByTransactionId(transactionId);

        if (ObjectUtils.isEmpty(bulkRequest)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        List<BulkConsentStatusResponse.BulkConsentItemStatus> itemStatuses = bulkRequest.getConsents().stream()
                .map(consent -> BulkConsentStatusResponse.BulkConsentItemStatus.builder()
                        .txnId(consent.getTxnId())
                        .status(consent.getStatus())
                        .consentHandleId(consent.getConsentHandleId())
                        .consentId(consent.getConsentId())
                        .errorMessage(consent.getErrorMessage())
                        .build())
                .collect(Collectors.toList());

        return BulkConsentStatusResponse.builder()
                .transactionId(transactionId)
                .status(bulkRequest.getStatus())
                .totalCount(bulkRequest.getTotalCount())
                .successCount(bulkRequest.getSuccessCount())
                .failedCount(bulkRequest.getFailedCount())
                .consents(itemStatuses)
                .build();
    }

    private List<BulkConsentUpload.BulkConsentPreference> convertPreferences(
            List<com.jio.consent.dto.Request.BulkConsentPreferenceRequest> preferences) {
        if (preferences == null) {
            return new ArrayList<>();
        }
        return preferences.stream()
                .map(pref -> BulkConsentUpload.BulkConsentPreference.builder()
                        .preferenceId(pref.getPreferenceId())
                        .action(pref.getAction())
                        .build())
                .collect(Collectors.toList());
    }
}
