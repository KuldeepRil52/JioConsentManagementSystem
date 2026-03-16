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
import com.jio.partnerportal.dto.request.DataTypeRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.DataType;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.DataTypeRepository;
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
public class DataTypeService {

    private DataTypeRepository dataTypeRepository;
    private Utils utils;
    private AuditManager auditManager;

    public DataTypeService(DataTypeRepository dataTypeRepository, Utils utils, AuditManager auditManager) {
        this.dataTypeRepository = dataTypeRepository;
        this.utils = utils;
        this.auditManager = auditManager;
    }

    @Value("${dataType.search.parameters}")
    List<String> dataTypeSearchParams;

    public DataType createDataType(DataTypeRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Create Data type";

        // Check if data type with the same name already exists
        if (this.dataTypeRepository.existsByDataTypeName(request.getDataTypeName())) {
            throw new PartnerPortalException(ErrorCodes.JCMP3064);
        }

        String dataTypeId = UUID.randomUUID().toString();
        DataType dataType = DataType.builder()
                .dataTypeId(dataTypeId)
                .dataTypeName(request.getDataTypeName())
                .dataItems(request.getDataItems())
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .scopeType(headers.get(Constants.SCOPE_LEVEL_HEADER))
                .status(Status.ACTIVE.toString())
                .build();

        this.logDataTypeAudit(dataType, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create data type successfully");
        return this.dataTypeRepository.save(dataType);

    }

    public DataType updateDataType(DataTypeRequest request, String dataTypeId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Update Data Type";


        DataType dataType = this.dataTypeRepository.findByDataTypeId(dataTypeId);
        if(ObjectUtils.isEmpty(dataType)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Check if another data type with the same name already exists (excluding current data type)
        if (this.dataTypeRepository.existsByDataTypeNameExcludingDataTypeId(request.getDataTypeName(), dataTypeId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3065);
        }

        dataType.setDataTypeName(request.getDataTypeName());
        dataType.setDataItems(request.getDataItems());
        this.logDataTypeAudit(dataType, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update Data Type successfully");
        return this.dataTypeRepository.save(dataType);
    }

    public SearchResponse<DataType> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search Data Type";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, dataTypeSearchParams);
        List<DataType> mongoResponse = this.dataTypeRepository.findDataTypeByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search Data Type successfully");
        return SearchResponse.<DataType>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.dataTypeRepository.count();
    }

    /**
     * Modular function to log data type audit events
     * Can be used in both create and update data type flows
     *
     * @param dataType The data type entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logDataTypeAudit(DataType dataType, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.DATA_TYPE_ID_CONSTANT)
                    .id(dataType.getDataTypeId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add data type POJO in the extra field under the "data" key
            extra.put(Constants.DATA, dataType);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(dataType.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.DATA_TYPE)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for data type id: {}, action: {}, error: {}", 
                    dataType.getDataTypeId(), actionType, e.getMessage(), e);
        }
    }
}
