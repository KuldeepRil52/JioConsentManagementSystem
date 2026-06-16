package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.client.notification.NotificationManager;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import com.jio.partnerportal.client.wso2.Wso2Manager;
import com.jio.partnerportal.client.wso2.request.OnboardDataProcessorRequest;
import com.jio.partnerportal.client.wso2.response.OnboardDataProcessorResponse;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.Status;
import com.jio.partnerportal.dto.request.DataProcessorRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.DataProcessor;
import com.jio.partnerportal.entity.Document;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.DataProcessorRepository;
import com.jio.partnerportal.repository.DocumentRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.RestUtility;
import com.jio.partnerportal.util.Utils;
import com.jio.partnerportal.util.DocumentValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class DataProcessorService {
    private DataProcessorRepository dataProcessorRepository;
    private Utils utils;
    private DocumentRepository documentRepository;
    private DocumentValidator documentValidator;
    private Wso2Manager wso2Manager;
    private RestUtility restUtility;
    private Environment environment;
    private AuditManager auditManager;
    private NotificationManager notificationManager;

    @Autowired
    public DataProcessorService(DataProcessorRepository dataProcessorRepository, Utils utils,
                               DocumentRepository documentRepository, DocumentValidator documentValidator, Wso2Manager wso2Manager,
                               RestUtility restUtility,
                               Environment environment, AuditManager auditManager,
                               NotificationManager notificationManager) {
        this.dataProcessorRepository = dataProcessorRepository;
        this.utils = utils;
        this.documentRepository = documentRepository;
        this.documentValidator = documentValidator;
        this.wso2Manager = wso2Manager;
        this.restUtility = restUtility;
        this.environment = environment;
        this.auditManager = auditManager;
        this.notificationManager = notificationManager;
    }

    @Value("${dataProcessor.search.parameters}")
    List<String> dataProcessorSearchParams;

    public DataProcessor createDataProcessor(DataProcessorRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Create Data Processor";

        // Check if data processor with the same name already exists
        if (this.dataProcessorRepository.existsByDataProcessorName(request.getDataProcessorName())) {
            throw new PartnerPortalException(ErrorCodes.JCMP3062);
        }
//
//        if (request.getAttachment() != null && request.getAttachmentMeta() != null) {
//            // Validate attachment content before saving
//            this.documentValidator.validateDocument(request.getAttachment(), request.getAttachmentMeta().getName(), request.getAttachmentMeta().getContentType(), request.getAttachmentMeta().getSize());
//        }
//
//        if (request.getVendorRiskDocument() != null && request.getVendorRiskDocumentMeta() != null) {
//            // Validate vendor risk document before saving
//            this.documentValidator.validateDocument(request.getVendorRiskDocument(), request.getVendorRiskDocumentMeta().getName(), request.getVendorRiskDocumentMeta().getContentType(), request.getVendorRiskDocumentMeta().getSize());
//        }
//
//        if (request.getCertificate() != null && request.getCertificateMeta() != null) {
//            // Validate certificate content before saving
//            this.documentValidator.validateDocument(request.getCertificate(), request.getCertificateMeta().getName(), request.getCertificateMeta().getContentType(), request.getCertificateMeta().getSize());
//        }

        String dataProcessorId = UUID.randomUUID().toString();
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        String attachmentId = UUID.randomUUID().toString();
        String vendorRiskDocumentId = UUID.randomUUID().toString();

        // Call WSO2 to onboard data processor
        OnboardDataProcessorRequest onboardDataProcessorRequest = OnboardDataProcessorRequest.builder()
                .tenantId(tenantId)
                .dataProcessorId(dataProcessorId)
                .dataProcessorName(request.getDataProcessorName())
                .build();

        OnboardDataProcessorResponse onboardDataProcessorResponse = wso2Manager.onboardDataProcessor(onboardDataProcessorRequest);

        if (onboardDataProcessorResponse == null || !onboardDataProcessorResponse.isSuccess() || onboardDataProcessorResponse.getData() == null) {
            throw new PartnerPortalException(ErrorCodes.JCMP3033);
        }

        Document attachment = Document.builder()
                .documentId(attachmentId)
                .documentName(request.getAttachmentMeta().getName())
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .contentType(request.getAttachmentMeta().getContentType())
                .status(Status.ACTIVE.toString())
                .documentSize(request.getAttachmentMeta().getSize())
                .tag(request.getAttachmentMeta().getTag())
                .isBase64Document(true)
                .data(request.getAttachment())
                .version(1)
                .build();

        Document vendorRiskDocument = Document.builder()
                .documentId(vendorRiskDocumentId)
                .documentName(request.getVendorRiskDocumentMeta().getName())
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .contentType(request.getVendorRiskDocumentMeta().getContentType())
                .status(Status.ACTIVE.toString())
                .documentSize(request.getVendorRiskDocumentMeta().getSize())
                .tag(request.getVendorRiskDocumentMeta().getTag())
                .isBase64Document(true)
                .data(request.getVendorRiskDocument())
                .version(1)
                .build();


        documentRepository.saveDocument(attachment);
        documentRepository.saveDocument(vendorRiskDocument);

        request.getAttachmentMeta().setDocumentId(attachmentId);
        request.getVendorRiskDocumentMeta().setDocumentId(vendorRiskDocumentId);
        
        // Save Certificate to document collection if provided
        if (request.getCertificate() != null && request.getCertificateMeta() != null) {
            String certificateDocumentId = UUID.randomUUID().toString();
            Document certificateDocument = Document.builder()
                    .documentId(certificateDocumentId)
                    .documentName(request.getCertificateMeta().getName())
                    .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                    .contentType(request.getCertificateMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(request.getCertificateMeta().getSize())
                    .tag(request.getCertificateMeta().getTag())
                    .isBase64Document(true)
                    .data(request.getCertificate())
                    .version(1)
                    .build();
            
            documentRepository.saveDocument(certificateDocument);
            request.getCertificateMeta().setDocumentId(certificateDocumentId);
            log.info("Certificate saved to document collection with ID: {}", certificateDocumentId);
        }
        DataProcessor dataProcessor = DataProcessor.builder()
                .dataProcessorId(dataProcessorId)
                .dataProcessorName(request.getDataProcessorName())
                .attachment(request.getAttachment())
                .attachmentMeta(request.getAttachmentMeta())
                .callbackUrl(request.getCallbackUrl())
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .scopeType(headers.get(Constants.SCOPE_LEVEL_HEADER))
                .vendorRiskDocument(request.getVendorRiskDocument())
                .vendorRiskDocumentMeta(request.getVendorRiskDocumentMeta())
                .details(request.getDetails())
                .status(Status.ACTIVE.toString())
                .spoc(request.getSpoc())
                .identityType(request.getIdentityType())
                .dataProcessorUniqueId(onboardDataProcessorResponse.getData().getDataProcessorUniqueId())
                .consumerKey(onboardDataProcessorResponse.getData().getConsumerKey())
                .consumerSecret(onboardDataProcessorResponse.getData().getConsumerSecret())
                .isCrossBordered(request.getIsCrossBordered())
                .certificate(request.getCertificate())
                .certificateMeta(request.getCertificateMeta())
                .build();

        DataProcessor savedDataProcessor = this.dataProcessorRepository.save(dataProcessor);

        // Send notification asynchronously
        try {
            String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
            String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
            String consumerKey = onboardDataProcessorResponse.getData().getConsumerKey();
            String consumerSecret = onboardDataProcessorResponse.getData().getConsumerSecret();
            initiateDataProcessorOnboardNotification(request, tenantId, businessId, scopeLevel, dataProcessorId, consumerKey, consumerSecret);
        } catch (Exception e) {
            log.error("Error initiating data processor onboard notification: {}", e.getMessage(), e);
        }

        this.logDataProcessorAudit(savedDataProcessor, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create Data Processor successfully");
        return savedDataProcessor;

    }

    public DataProcessor updateDataProcessor(DataProcessorRequest request, String dataProcessorId, boolean isCertificateModified, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Update Data Processor";

        DataProcessor dataProcessor = this.dataProcessorRepository.findByDataProcessorId(dataProcessorId);
        if(ObjectUtils.isEmpty(dataProcessor)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Check if another data processor with the same name already exists (excluding current data processor)
        if (this.dataProcessorRepository.existsByDataProcessorNameExcludingDataProcessorId(request.getDataProcessorName(), dataProcessorId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3063);
        }

        String businessId = dataProcessor.getBusinessId();
        
        // Handle Certificate modification
        if (isCertificateModified && 
            request.getCertificate() != null && 
            request.getCertificateMeta() != null) {
            
            String certificateDocumentId = UUID.randomUUID().toString();
            // Validate updated certificate
//            this.documentValidator.validateDocument(request.getCertificate(), request.getCertificateMeta().getName(), request.getCertificateMeta().getContentType(), request.getCertificateMeta().getSize());
            Document certificateDocument = Document.builder()
                    .documentId(certificateDocumentId)
                    .documentName(request.getCertificateMeta().getName())
                    .businessId(businessId)
                    .contentType(request.getCertificateMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(request.getCertificateMeta().getSize())
                    .tag(request.getCertificateMeta().getTag())
                    .isBase64Document(true)
                    .data(request.getCertificate())
                    .version(1)
                    .build();
            
            documentRepository.saveDocument(certificateDocument);
            request.getCertificateMeta().setDocumentId(certificateDocumentId);
            log.info("Updated Certificate saved to document collection with new ID: {}", certificateDocumentId);
        }

        dataProcessor.setDataProcessorName(request.getDataProcessorName());
        dataProcessor.setCallbackUrl(request.getCallbackUrl());
        dataProcessor.setDetails(request.getDetails());
        dataProcessor.setSpoc(request.getSpoc());
        dataProcessor.setIdentityType(request.getIdentityType());
        dataProcessor.setIsCrossBordered(request.getIsCrossBordered());
        dataProcessor.setCertificate(request.getCertificate());
        dataProcessor.setCertificateMeta(request.getCertificateMeta());

        this.logDataProcessorAudit(dataProcessor, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update Data Processor successfully");
        return this.dataProcessorRepository.save(dataProcessor);
    }

    private void initiateDataProcessorOnboardNotification(DataProcessorRequest request, String tenantId, String businessId, String scopeLevel, String dataProcessorId, String consumerKey, String consumerSecret) {
        try {
            // Validate SPOC and identity type
            if (request.getSpoc() == null) {
                log.warn("SPOC details are missing for data processor: {}", dataProcessorId);
                return;
            }

            // Determine customer identifier based on identity type
            IdentityType identityType = request.getIdentityType();
            String customerIdentifierValue = null;

            if (identityType == IdentityType.EMAIL && request.getSpoc().getEmail() != null) {
                customerIdentifierValue = request.getSpoc().getEmail();
            } else {
                log.warn("Unknown or missing identity type/customer identifier for data processor: {}. Only EMAIL is supported for data processor SPOC.", dataProcessorId);
                return;
            }

            // Build customer identifiers
            TriggerEventRequest.CustomerIdentifiers customerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                    .type(identityType)
                    .value(customerIdentifierValue)
                    .build();

            // Build event payload for PROCESSOR_ONBOARDING
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("processorSpocName", request.getSpoc().getName());
            eventPayload.put("processorName", request.getDataProcessorName());
            eventPayload.put("processorId", dataProcessorId);
            eventPayload.put("clientId", consumerKey);
            eventPayload.put("clientSecret", consumerSecret);

            // Build trigger event request
            TriggerEventRequest triggerEventRequest = TriggerEventRequest.builder()
                    .eventType("PROCESSOR_ONBOARDING")
                    .customerIdentifiers(customerIdentifiers)
                    .eventPayload(eventPayload)
                    .build();

            // Trigger notification event using NotificationManager
            notificationManager.triggerEvent(tenantId, businessId, scopeLevel, triggerEventRequest);

            log.info("PROCESSOR_ONBOARDING notification event triggered successfully for tenantId: {}, businessId: {}, dataProcessorId: {}", 
                    tenantId, businessId, dataProcessorId);

        } catch (Exception e) {
            log.error("Error triggering PROCESSOR_ONBOARDING notification event for dataProcessorId: {}. Error: {}",
                    dataProcessorId, e.getMessage(), e);
        }
    }

    public SearchResponse<DataProcessor> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, dataProcessorSearchParams);
        List<DataProcessor> mongoResponse = this.dataProcessorRepository.findDataProcessorByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        return SearchResponse.<DataProcessor>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.dataProcessorRepository.count();
    }

    /**
     * Modular function to log data processor audit events
     * Can be used in both create and update data processor flows
     *
     * @param dataProcessor The data processor entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logDataProcessorAudit(DataProcessor dataProcessor, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.DATA_PROCESSOR_ID)
                    .id(dataProcessor.getDataProcessorId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add data processor POJO in the extra field under the "data" key
            extra.put(Constants.DATA, dataProcessor);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(dataProcessor.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.DATA_PROCESSOR)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for data processor id: {}, action: {}, error: {}", 
                    dataProcessor.getDataProcessorId(), actionType, e.getMessage(), e);
        }
    }
}
