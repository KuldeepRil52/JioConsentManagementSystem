package com.jio.digigov.grievance.service;

import com.jio.digigov.grievance.dto.request.GrievanceTemplateRequest;
import com.jio.digigov.grievance.entity.GrievanceTemplate;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GrievanceTemplateService {

    GrievanceTemplate create(GrievanceTemplate req, String tenantId, String businessId, String transactionId, HttpServletRequest httpRequest);

    List<GrievanceTemplate> listByBusinessId(String tenantId, String businessId, Integer page, Integer size);

    Optional<GrievanceTemplate> getById(String templateId, String tenantId, String businessId);

    boolean delete(String templateId, String tenantId, String businessId);

    long count(String tenantId, String businessId);

    int getLatestVersion(String businessId, String grievanceTemplateName);

    /**
     * Update existing grievance template.
     */
    GrievanceTemplate update(String templateId, GrievanceTemplateRequest request, String tenantId, String businessId, String transactionId, HttpServletRequest httpRequest);

    /**
     * Search templates by filter params.
     */
    List<GrievanceTemplate> search(String tenantId, String businessId, Map<String, String> filters,
                                   Integer page, Integer size);
}
