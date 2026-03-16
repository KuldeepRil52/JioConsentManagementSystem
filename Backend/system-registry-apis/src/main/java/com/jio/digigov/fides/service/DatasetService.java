package com.jio.digigov.fides.service;

import com.jio.digigov.fides.dto.request.DatasetRequest;
import com.jio.digigov.fides.dto.response.DatasetCountResponse;
import com.jio.digigov.fides.dto.response.DatasetListResponse;
import com.jio.digigov.fides.dto.response.DatasetResponse;
import com.jio.digigov.fides.dto.response.DatasetUpdateResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface DatasetService {

    DatasetResponse create(DatasetRequest request, String tenantId, String businessId, HttpServletRequest req);

    DatasetListResponse list(String tenantId, String businessId);

    DatasetResponse getById(String datasetId, String tenantId, String businessId);

    DatasetUpdateResponse update(String datasetId, DatasetRequest request,
                                 String tenantId, String businessId, HttpServletRequest req);

    void delete(String datasetId, String tenantId, String businessId, HttpServletRequest req);

    DatasetCountResponse getDatasetCounts(String tenantId, String businessId);
}