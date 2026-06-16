package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.ConfigType;
import com.jio.partnerportal.dto.Operation;
import com.jio.partnerportal.dto.request.RopaRequest;
import com.jio.partnerportal.dto.response.RopaDetailResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.BusinessApplication;
import com.jio.partnerportal.entity.LegalEntity;
import com.jio.partnerportal.entity.ProcessorActivity;
import com.jio.partnerportal.entity.Purpose;
import com.jio.partnerportal.entity.RopaRecord;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.BusinessApplicationRepository;
import com.jio.partnerportal.repository.LegalEntityRepository;
import com.jio.partnerportal.repository.ProcessorActivityRepository;
import com.jio.partnerportal.repository.PurposeRepository;
import com.jio.partnerportal.repository.RopaRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RopaService {
    
    RopaRepository ropaRepository;
    BusinessApplicationRepository businessApplicationRepository;
    LegalEntityRepository legalEntityRepository;
    ProcessorActivityRepository processorActivityRepository;
    PurposeRepository purposeRepository;
    Utils utils;
    ConfigHistoryService configHistoryService;
    AuditManager auditManager;

    @Autowired
    RopaService(RopaRepository ropaRepository, BusinessApplicationRepository businessApplicationRepository, 
                LegalEntityRepository legalEntityRepository, ProcessorActivityRepository processorActivityRepository, 
                PurposeRepository purposeRepository, Utils utils, ConfigHistoryService configHistoryService, AuditManager auditManager) {
        this.ropaRepository = ropaRepository;
        this.businessApplicationRepository = businessApplicationRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.processorActivityRepository = processorActivityRepository;
        this.purposeRepository = purposeRepository;
        this.utils = utils;
        this.configHistoryService = configHistoryService;
        this.auditManager = auditManager;
    }

    @Value("${ropa.search.parameters}")
    List<String> ropaSearchParams;

    public RopaRecord createRopa(Map<String, String> headers, RopaRequest request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Create Ropa records";

        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);

        RopaRecord ropaRecord = RopaRecord.builder()
                .ropaId(UUID.randomUUID().toString())
                .businessId(businessId)
                .processingActivityId(request.getProcessingActivityId())
                .purposeForProcessingId(request.getPurposeForProcessingId())
                .categoriesOfSpecialNature(request.getCategoriesOfSpecialNature())
                .sourceOfPersonalData(request.getSourceOfPersonalData())
                .categoryOfIndividual(request.getCategoryOfIndividual())
                .activityReason(request.getActivityReason())
                .additionalCondition(request.getAdditionalCondition())
                .caseOrPurposeForExemption(request.getCaseOrPurposeForExemption())
                .dpiaReference(request.getDpiaReference())
                .linkOrDocumentRef(request.getLinkOrDocumentRef())
                .businessFunctionsSharedWith(request.getBusinessFunctionsSharedWith())
                .geographicalLocations(request.getGeographicalLocations())
                .thirdPartiesSharedWith(request.getThirdPartiesSharedWith())
                .contractReferences(request.getContractReferences())
                .crossBorderFlow(request.isCrossBorderFlow())
                .restrictedTransferSafeguards(request.getRestrictedTransferSafeguards())
                .administrativePrecautions(request.getAdministrativePrecautions())
                .financialPrecautions(request.getFinancialPrecautions())
                .technicalPrecautions(request.getTechnicalPrecautions())
                .retentionPeriod(request.getRetentionPeriod())
                .storageLocation(request.getStorageLocation())
                .breachDocumentation(request.getBreachDocumentation())
                .lastBreachDate(request.getLastBreachDate())
                .breachSummary(request.getBreachSummary())
                .build();

        RopaRecord savedRopaRecord = this.ropaRepository.save(ropaRecord);
        this.configHistoryService.createConfigHistoryEntry(savedRopaRecord, businessId, ConfigType.ROPA, Operation.CREATE);
        this.logRopaAudit(savedRopaRecord, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create Ropa records successfully");
        return savedRopaRecord;
    }

    public RopaRecord updateRopa(String ropaId, RopaRequest request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {

        String activity = "Update Ropa records";

        RopaRecord ropaRecord = this.ropaRepository.findByRopaId(ropaId);
        if (ObjectUtils.isEmpty(ropaRecord)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        ropaRecord.setProcessingActivityId(request.getProcessingActivityId());
        ropaRecord.setPurposeForProcessingId(request.getPurposeForProcessingId());
        ropaRecord.setCategoriesOfSpecialNature(request.getCategoriesOfSpecialNature());
        ropaRecord.setSourceOfPersonalData(request.getSourceOfPersonalData());
        ropaRecord.setCategoryOfIndividual(request.getCategoryOfIndividual());
        ropaRecord.setActivityReason(request.getActivityReason());
        ropaRecord.setAdditionalCondition(request.getAdditionalCondition());
        ropaRecord.setCaseOrPurposeForExemption(request.getCaseOrPurposeForExemption());
        ropaRecord.setDpiaReference(request.getDpiaReference());
        ropaRecord.setLinkOrDocumentRef(request.getLinkOrDocumentRef());
        ropaRecord.setBusinessFunctionsSharedWith(request.getBusinessFunctionsSharedWith());
        ropaRecord.setGeographicalLocations(request.getGeographicalLocations());
        ropaRecord.setThirdPartiesSharedWith(request.getThirdPartiesSharedWith());
        ropaRecord.setContractReferences(request.getContractReferences());
        ropaRecord.setCrossBorderFlow(request.isCrossBorderFlow());
        ropaRecord.setRestrictedTransferSafeguards(request.getRestrictedTransferSafeguards());
        ropaRecord.setAdministrativePrecautions(request.getAdministrativePrecautions());
        ropaRecord.setFinancialPrecautions(request.getFinancialPrecautions());
        ropaRecord.setTechnicalPrecautions(request.getTechnicalPrecautions());
        ropaRecord.setRetentionPeriod(request.getRetentionPeriod());
        ropaRecord.setStorageLocation(request.getStorageLocation());
        ropaRecord.setBreachDocumentation(request.getBreachDocumentation());
        ropaRecord.setLastBreachDate(request.getLastBreachDate());
        ropaRecord.setBreachSummary(request.getBreachSummary());
        
        RopaRecord updatedRopaRecord = this.ropaRepository.save(ropaRecord);
        this.configHistoryService.createConfigHistoryEntry(updatedRopaRecord, ropaRecord.getBusinessId(), ConfigType.ROPA, Operation.UPDATE);
        this.logRopaAudit(updatedRopaRecord, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update Ropa records successfully");
        return updatedRopaRecord;
    }

    public SearchResponse<RopaRecord> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search Ropa records";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, ropaSearchParams);
        List<RopaRecord> mongoResponse = this.ropaRepository.findRopaByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search Ropa records successfully");
        return SearchResponse.<RopaRecord>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.ropaRepository.count();
    }

    public RopaRecord findByRopaId(String ropaId) throws PartnerPortalException {
        RopaRecord ropaRecord = this.ropaRepository.findByRopaId(ropaId);
        if (ObjectUtils.isEmpty(ropaRecord)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        return ropaRecord;
    }

    public void deleteRopa(String ropaId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Delete ROPA record";

        RopaRecord ropaRecord = this.ropaRepository.findByRopaId(ropaId);
        if (ObjectUtils.isEmpty(ropaRecord)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Log audit before deletion
        this.logRopaAudit(ropaRecord, ActionType.DELETE);
        
        // Delete the ROPA record
        this.ropaRepository.deleteByRopaId(ropaId);
        
        LogUtil.logActivity(req, activity, "Success: Delete ROPA record successfully");
    }

    public RopaDetailResponse getRopaDetails(String ropaId, String tenantId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Get Ropa details";

        RopaRecord ropaRecord = this.ropaRepository.findByRopaId(ropaId);
        if (ObjectUtils.isEmpty(ropaRecord)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Fetch business application details
        BusinessApplication businessApplication = this.businessApplicationRepository.findByBusinessId(ropaRecord.getBusinessId());
        
        // Fetch processor activity details
        ProcessorActivity processorActivity = this.processorActivityRepository.findByProcessorActivityId(ropaRecord.getProcessingActivityId());
        
        // Fetch purpose details
        Purpose purpose = this.purposeRepository.findByPurposeId(ropaRecord.getPurposeForProcessingId());

        // Fetch LegalEntity to get tenant SPOC details (legalEntityId equals tenantId)
        LegalEntity legalEntity = this.legalEntityRepository.findLegalEntityByLegalEntityId(tenantId);
        
        // Build process owner from tenant SPOC
        RopaDetailResponse.ProcessOwner processOwner;
        if (legalEntity != null && legalEntity.getSpoc() != null) {
            processOwner = RopaDetailResponse.ProcessOwner.builder()
                    .name(legalEntity.getSpoc().getName() != null ? legalEntity.getSpoc().getName() : "N/A")
                    .mobile(legalEntity.getSpoc().getMobile() != null ? legalEntity.getSpoc().getMobile() : "N/A")
                    .email(legalEntity.getSpoc().getEmail() != null ? legalEntity.getSpoc().getEmail() : "N/A")
                    .build();
        } else {
            processOwner = RopaDetailResponse.ProcessOwner.builder()
                    .name("N/A")
                    .mobile("N/A")
                    .email("N/A")
                    .build();
        }

        // Build process overview
        RopaDetailResponse.ProcessOverview processOverview = RopaDetailResponse.ProcessOverview.builder()
                .businessFunction(businessApplication != null ? businessApplication.getName() : "N/A")
                .department(businessApplication != null ? businessApplication.getName() : "N/A")
                .processOwner(processOwner)
                .processingActivityName(processorActivity != null ? processorActivity.getActivityName() : "N/A")
                .purposeForProcessing(purpose != null ? purpose.getPurposeDescription() : "N/A")
                .build();


        LogUtil.logActivity(req, activity, "Success: Get Ropa details successfully");
        // Build the complete response
        return RopaDetailResponse.builder()
                .id(ropaRecord.getId().toString())
                .ropaId(ropaRecord.getRopaId())
                .businessId(ropaRecord.getBusinessId())
                .processOverview(processOverview)
                .categoriesOfSpecialNature(ropaRecord.getCategoriesOfSpecialNature())
                .sourceOfPersonalData(ropaRecord.getSourceOfPersonalData())
                .categoryOfIndividual(ropaRecord.getCategoryOfIndividual())
                .activityReason(ropaRecord.getActivityReason())
                .additionalCondition(ropaRecord.getAdditionalCondition())
                .caseOrPurposeForExemption(ropaRecord.getCaseOrPurposeForExemption())
                .dpiaReference(ropaRecord.getDpiaReference())
                .linkOrDocumentRef(ropaRecord.getLinkOrDocumentRef())
                .businessFunctionsSharedWith(ropaRecord.getBusinessFunctionsSharedWith())
                .geographicalLocations(ropaRecord.getGeographicalLocations())
                .thirdPartiesSharedWith(ropaRecord.getThirdPartiesSharedWith())
                .contractReferences(ropaRecord.getContractReferences())
                .crossBorderFlow(ropaRecord.isCrossBorderFlow())
                .restrictedTransferSafeguards(ropaRecord.getRestrictedTransferSafeguards())
                .administrativePrecautions(ropaRecord.getAdministrativePrecautions())
                .financialPrecautions(ropaRecord.getFinancialPrecautions())
                .technicalPrecautions(ropaRecord.getTechnicalPrecautions())
                .retentionPeriod(ropaRecord.getRetentionPeriod())
                .storageLocation(ropaRecord.getStorageLocation())
                .breachDocumentation(ropaRecord.getBreachDocumentation())
                .lastBreachDate(ropaRecord.getLastBreachDate())
                .breachSummary(ropaRecord.getBreachSummary())
                .createdAt(ropaRecord.getCreatedAt())
                .updatedAt(ropaRecord.getUpdatedAt())
                .build();
    }

    public List<RopaDetailResponse> getAllRopaDetails(String tenantId, HttpServletRequest req) throws PartnerPortalException {

        String act = "Get All Ropa Details";
        // Fetch all ROPA records for the tenant (DB-scoped by tenant-id)
        List<RopaRecord> ropaRecords = this.ropaRepository.findAllRopa();
        
        if (ObjectUtils.isEmpty(ropaRecords)) {
            return new ArrayList<>();
        }

        // Collect unique IDs for bulk queries
        Set<String> businessIds = new HashSet<>();
        Set<String> processorActivityIds = new HashSet<>();
        Set<String> purposeIds = new HashSet<>();
        
        for (RopaRecord record : ropaRecords) {
            if (record.getBusinessId() != null) {
                businessIds.add(record.getBusinessId());
            }
            if (record.getProcessingActivityId() != null) {
                processorActivityIds.add(record.getProcessingActivityId());
            }
            if (record.getPurposeForProcessingId() != null) {
                purposeIds.add(record.getPurposeForProcessingId());
            }
        }

        // Bulk fetch related entities
        List<BusinessApplication> businessApplications = businessIds.isEmpty() ? new ArrayList<>() 
                : this.businessApplicationRepository.findByBusinessIds(new ArrayList<>(businessIds));
        List<ProcessorActivity> processorActivities = processorActivityIds.isEmpty() ? new ArrayList<>() 
                : this.processorActivityRepository.findByProcessorActivityIds(new ArrayList<>(processorActivityIds));
        List<Purpose> purposes = purposeIds.isEmpty() ? new ArrayList<>() 
                : this.purposeRepository.findByPurposeIds(new ArrayList<>(purposeIds));

        // Fetch LegalEntity for tenant SPOC (single query since same tenantId)
        LegalEntity legalEntity = this.legalEntityRepository.findLegalEntityByLegalEntityId(tenantId);

        // Build lookup maps for O(1) access
        Map<String, BusinessApplication> businessMap = businessApplications.stream()
                .collect(Collectors.toMap(BusinessApplication::getBusinessId, business -> business, (existing, replacement) -> existing));
        Map<String, ProcessorActivity> processorActivityMap = processorActivities.stream()
                .collect(Collectors.toMap(ProcessorActivity::getProcessorActivityId, activity -> activity, (existing, replacement) -> existing));
        Map<String, Purpose> purposeMap = purposes.stream()
                .collect(Collectors.toMap(Purpose::getPurposeId, purpose -> purpose, (existing, replacement) -> existing));

        // Build process owner from tenant SPOC
        RopaDetailResponse.ProcessOwner processOwner;
        if (legalEntity != null && legalEntity.getSpoc() != null) {
            processOwner = RopaDetailResponse.ProcessOwner.builder()
                    .name(legalEntity.getSpoc().getName() != null ? legalEntity.getSpoc().getName() : "N/A")
                    .mobile(legalEntity.getSpoc().getMobile() != null ? legalEntity.getSpoc().getMobile() : "N/A")
                    .email(legalEntity.getSpoc().getEmail() != null ? legalEntity.getSpoc().getEmail() : "N/A")
                    .build();
        } else {
            processOwner = RopaDetailResponse.ProcessOwner.builder()
                    .name("N/A")
                    .mobile("N/A")
                    .email("N/A")
                    .build();
        }

        // Build response list
        List<RopaDetailResponse> responses = new ArrayList<>();
        for (RopaRecord ropaRecord : ropaRecords) {
            BusinessApplication businessApplication = ropaRecord.getBusinessId() != null 
                    ? businessMap.get(ropaRecord.getBusinessId()) : null;
            ProcessorActivity processorActivity = ropaRecord.getProcessingActivityId() != null 
                    ? processorActivityMap.get(ropaRecord.getProcessingActivityId()) : null;
            Purpose purpose = ropaRecord.getPurposeForProcessingId() != null 
                    ? purposeMap.get(ropaRecord.getPurposeForProcessingId()) : null;

            // Build process overview
            RopaDetailResponse.ProcessOverview processOverview = RopaDetailResponse.ProcessOverview.builder()
                    .businessFunction(businessApplication != null ? businessApplication.getName() : "N/A")
                    .department(businessApplication != null ? businessApplication.getName() : "N/A")
                    .processOwner(processOwner)
                    .processingActivityName(processorActivity != null ? processorActivity.getActivityName() : "N/A")
                    .purposeForProcessing(purpose != null ? purpose.getPurposeDescription() : "N/A")
                    .build();

            // Build complete response
            RopaDetailResponse response = RopaDetailResponse.builder()
                    .id(ropaRecord.getId().toString())
                    .ropaId(ropaRecord.getRopaId())
                    .businessId(ropaRecord.getBusinessId())
                    .processOverview(processOverview)
                    .categoriesOfSpecialNature(ropaRecord.getCategoriesOfSpecialNature())
                    .sourceOfPersonalData(ropaRecord.getSourceOfPersonalData())
                    .categoryOfIndividual(ropaRecord.getCategoryOfIndividual())
                    .activityReason(ropaRecord.getActivityReason())
                    .additionalCondition(ropaRecord.getAdditionalCondition())
                    .caseOrPurposeForExemption(ropaRecord.getCaseOrPurposeForExemption())
                    .dpiaReference(ropaRecord.getDpiaReference())
                    .linkOrDocumentRef(ropaRecord.getLinkOrDocumentRef())
                    .businessFunctionsSharedWith(ropaRecord.getBusinessFunctionsSharedWith())
                    .geographicalLocations(ropaRecord.getGeographicalLocations())
                    .thirdPartiesSharedWith(ropaRecord.getThirdPartiesSharedWith())
                    .contractReferences(ropaRecord.getContractReferences())
                    .crossBorderFlow(ropaRecord.isCrossBorderFlow())
                    .restrictedTransferSafeguards(ropaRecord.getRestrictedTransferSafeguards())
                    .administrativePrecautions(ropaRecord.getAdministrativePrecautions())
                    .financialPrecautions(ropaRecord.getFinancialPrecautions())
                    .technicalPrecautions(ropaRecord.getTechnicalPrecautions())
                    .retentionPeriod(ropaRecord.getRetentionPeriod())
                    .storageLocation(ropaRecord.getStorageLocation())
                    .breachDocumentation(ropaRecord.getBreachDocumentation())
                    .lastBreachDate(ropaRecord.getLastBreachDate())
                    .breachSummary(ropaRecord.getBreachSummary())
                    .createdAt(ropaRecord.getCreatedAt())
                    .updatedAt(ropaRecord.getUpdatedAt())
                    .build();

            responses.add(response);
        }

        LogUtil.logActivity(req, act, "Success: Get All Ropa Details successfully");
        return responses;
    }

    /**
     * Modular function to log ropa audit events
     * Can be used in both create and update ropa flows
     *
     * @param ropaRecord The ropaRecord entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logRopaAudit(RopaRecord ropaRecord, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.ROPA_ID)
                    .id(ropaRecord.getRopaId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add ropaRecord POJO in the extra field under the "data" key
            extra.put(Constants.DATA, ropaRecord);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(ropaRecord.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.ROPA)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for notification config id: {}, action: {}, error: {}", 
                    ropaRecord.getRopaId(), actionType, e.getMessage(), e);
        }
    }
}
