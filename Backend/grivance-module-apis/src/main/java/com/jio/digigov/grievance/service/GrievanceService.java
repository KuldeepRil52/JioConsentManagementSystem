package com.jio.digigov.grievance.service;

import com.jio.digigov.grievance.dto.request.GrievanceCreateRequest;
import com.jio.digigov.grievance.dto.request.GrievanceUpdateRequest;
import com.jio.digigov.grievance.dto.response.GrievanceListResponse;
import com.jio.digigov.grievance.dto.response.GrievanceResponse;
import com.jio.digigov.grievance.dto.response.PagedResponse;
import com.jio.digigov.grievance.entity.Grievance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing Grievances with multi-tenant
 * and business-level validation. All operations are restricted
 * to BUSINESS scope only.
 */
public interface GrievanceService {

    /**
     * Creates a new grievance for the given tenant and business.
     *
     * @param grievance   grievance request object
     * @param tenantId    tenant identifier (from X-Tenant-Id header)
     * @param businessId  business identifier (from X-Business-Id header)
     * @return persisted grievance entity
     */
    GrievanceResponse create(GrievanceCreateRequest grievance, String tenantId, String businessId, String grievanceTemplateId, String transactionId, HttpServletRequest servletRequest);

    /**
     * Retrieves a paginated list of grievances with optional filters.
     *
     * @param page        page number (1-based)
     * @param size        number of records per page
     * @param tenantId    tenant identifier
     * @param businessId  business identifier
     * @return map containing grievance list and pagination metadata
     */
    PagedResponse<GrievanceListResponse> list(Integer page, Integer size,
                                              String tenantId, String businessId);

    /**
     * Retrieves a grievance by its ID.
     *
     * @param grievanceId grievance identifier
     * @param tenantId    tenant identifier
     * @param businessId  business identifier
     * @return optional containing grievance if found
     */
    Optional<Grievance> getById(String grievanceId, String tenantId, String businessId);

    /**
     * Updates an existing grievance with the given fields.
     *
     * @param grievanceId grievance identifier
     * @param updates     GrievanceUpdateRequest
     * @param tenantId    tenant identifier
     * @param businessId  business identifier
     * @return optional containing updated grievance if found
     */
    Grievance update(String grievanceId, GrievanceUpdateRequest updates,
                               String tenantId, String businessId, String transactionId, HttpServletRequest servletRequest);

    /**
     * Deletes a grievance by its ID.
     *
     * @param grievanceId grievance identifier
     * @param tenantId    tenant identifier
     * @param businessId  business identifier
     * @return true if deletion was successful, false otherwise
     */
    boolean delete(String grievanceId, String tenantId, String businessId);

    /**
     * Counts the number of grievances for a given tenant and business.
     *
     * @param tenantId    tenant identifier
     * @param businessId  business identifier
     * @return total grievance count
     */
//    long count(String tenantId, String businessId);

    long countByFilters(Map<String, String> params, String tenantId, String businessId);


    PagedResponse<GrievanceListResponse> search(Map<String, String> filters, String tenantId, String businessId,
                                                Integer page, Integer size);

    Grievance updateFeedback(String grievanceId, @Min(1) @Max(5) int feedback, @NotBlank String tenantId, @NotBlank String businessId);
}
