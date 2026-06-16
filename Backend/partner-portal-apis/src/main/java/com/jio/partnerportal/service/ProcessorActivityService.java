package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.Status;
import com.jio.partnerportal.dto.request.ProcessorActivityRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.ProcessorActivity;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.ProcessorActivityRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ProcessorActivityService {

    private ProcessorActivityRepository processorActivityRepository;
    private Utils utils;
    AuditManager auditManager;

    public ProcessorActivityService(ProcessorActivityRepository processorActivityRepository, Utils utils, AuditManager auditManager) {
        this.processorActivityRepository = processorActivityRepository;
        this.utils = utils;
        this.auditManager = auditManager;
    }

    @Value("${processorActivity.search.parameters}")
    List<String> processorActivitySearchParams;

    public ProcessorActivity createProcessorActivity(ProcessorActivityRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Create processor activity ";

        // Check if processor activity with the same name already exists
        if (this.processorActivityRepository.existsByActivityName(request.getActivityName())) {
            throw new PartnerPortalException(ErrorCodes.JCMP3066);
        }

        String processorActivityId = UUID.randomUUID().toString();

        ProcessorActivity processorActivity = ProcessorActivity.builder()
                .processorActivityId(processorActivityId)
                .activityName(request.getActivityName())
                .processorId(request.getProcessorId())
                .processorName(request.getProcessorName())
                .dataTypesList(request.getDataTypeList())
                .details(request.getDetails())
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .scopeType(headers.get(Constants.SCOPE_LEVEL_HEADER))
                .status(Status.ACTIVE.toString())
                .version(1)
                .build();

        this.logProcessorActivityAudit(processorActivity, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create processor activity successfully");
        return this.processorActivityRepository.save(processorActivity);

    }

    public ProcessorActivity updateProcessorActivity(ProcessorActivityRequest request, String dataProcessorId, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Update processor activity";

        ProcessorActivity processorActivity = this.processorActivityRepository.findByProcessorActivityId(dataProcessorId);
        if (ObjectUtils.isEmpty(processorActivity)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Check if another processor activity with the same name already exists (excluding current processor activity)
        if (this.processorActivityRepository.existsByActivityNameExcludingProcessorActivityId(request.getActivityName(), dataProcessorId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3067);
        }

        processorActivity.setActivityName(request.getActivityName());
        processorActivity.setProcessorId(request.getProcessorId());
        processorActivity.setProcessorName(request.getProcessorName());
        processorActivity.setDetails(request.getDetails());
        processorActivity.setDataTypesList(request.getDataTypeList());
        processorActivity.setStatus(request.getStatus().toString());
        processorActivity.setVersion(processorActivity.getVersion() + 1);

        processorActivity.setId(null);

        this.logProcessorActivityAudit(processorActivity, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update processor activity successfully");
        return this.processorActivityRepository.save(processorActivity);
    }

    public SearchResponse<ProcessorActivity> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Search processor activity";

        LogUtil.logActivity(req, activity, "Success:Search processor activity successfully");
        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, processorActivitySearchParams);
        List<ProcessorActivity> mongoResponse = this.processorActivityRepository.findProcessorActivityByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        return SearchResponse.<ProcessorActivity>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.processorActivityRepository.count();
    }

    public ProcessorActivity getLatestProcessActivity(String processActivityId, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Get Latest processor activities by ID";
        ProcessorActivity processorActivity = this.processorActivityRepository.findLatestByProcessorActivityId(processActivityId);

        if (ObjectUtils.isEmpty(processorActivity)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Get Latest processor activities by ID successfully");
        return processorActivity;
    }

    /**
     * Modular function to log processorActivity audit events
     * Can be used in both create and update processorActivity flows
     *
     * @param processorActivity The processorActivity entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logProcessorActivityAudit(ProcessorActivity processorActivity, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.PROCESSOR_ACTIVITY_ID)
                    .id(processorActivity.getProcessorActivityId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add processorActivity POJO in the extra field under the "data" key
            extra.put(Constants.DATA, processorActivity);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(processorActivity.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.PROCESSOR_ACTIVITY)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for Processor Activity id: {}, action: {}, error: {}", 
                    processorActivity.getProcessorActivityId(), actionType, e.getMessage(), e);
        }
    }
}
