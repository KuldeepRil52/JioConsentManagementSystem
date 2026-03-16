package com.jio.digigov.fides.service;

import com.jio.digigov.fides.dto.request.DbIntegrationCreateRequest;
import com.jio.digigov.fides.dto.request.DbIntegrationTestRequest;
import com.jio.digigov.fides.dto.request.DbIntegrationUpdateRequest;
import com.jio.digigov.fides.dto.response.DbIntegrationResponse;
import com.jio.digigov.fides.entity.DbIntegration;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface DbIntegrationService {

    Map<String, Object> getSupportedDbTypes();

    DbIntegrationResponse create(String tenantId, String businessId, DbIntegrationCreateRequest request, HttpServletRequest req);

    List<DbIntegration> list(String tenantId, String businessId, String systemId);

    DbIntegration getByIntegrationId(String tenantId, String businessId, String integrationId);

    DbIntegrationResponse update(String tenantId, String businessId, String integrationId, DbIntegrationUpdateRequest request, HttpServletRequest req);

    void delete(String tenantId, String businessId, String integrationId, HttpServletRequest req);

    DbIntegration mapDataset(String tenantId, String businessId, String integrationId, String datasetId, HttpServletRequest req);

    Map<String, Object> testConnection(DbIntegrationTestRequest request);

    Map<String, Object> count(String tenantId, String businessId);
}