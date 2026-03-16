package com.jio.digigov.grievance.service;

import com.jio.digigov.grievance.dto.request.GrievanceTypeUpdateRequest;
import com.jio.digigov.grievance.entity.GrievanceType;
import com.jio.digigov.grievance.enumeration.ScopeLevel;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing Grievance Types in a multi-tenant environment.
 * All operations respect tenantId and businessId context passed from headers.
 */
public interface GrievanceTypeService {

    /**
     * Create a new GrievanceType for a tenant/business.
     *
     * @param g GrievanceType entity to create
     * @param tenantId Tenant context
     * @param businessId Business context
     * @return created GrievanceType with audit fields
     */
    GrievanceType create(GrievanceType g, String tenantId, String businessId, ScopeLevel scopeLevel);

    /**
     * List all GrievanceTypes for the current tenant/business.
     *
     * @param tenantId Tenant context
     * @param businessId Business context
     * @return list of GrievanceTypes
     */
    List<GrievanceType> list(String tenantId, String businessId, ScopeLevel scopeLevel);

    /**
     * Retrieve a GrievanceType by its ID in a tenant/business.
     *
     * @param grievanceTypeId unique ID
     * @param tenantId Tenant context
     * @param businessId Business context
     * @return optional GrievanceType
     */
    Optional<GrievanceType> getById(String grievanceTypeId, String tenantId, String businessId);

    /**
     * Update a GrievanceType in a tenant/business.
     *
     * @param grievanceTypeId ID of GrievanceType
     * @param updateRequest update payload
     * @param tenantId Tenant context
     * @param businessId Business context
     * @return optional updated GrievanceType
     */
    Optional<GrievanceType> update(String grievanceTypeId, GrievanceTypeUpdateRequest updateRequest,
                                   String tenantId, String businessId);

    /**
     * Delete a GrievanceType in a tenant/business.
     *
     * @param grievanceTypeId ID to delete
     * @param tenantId Tenant context
     * @param businessId Business context
     * @return true if deleted, false if not found
     */
    boolean delete(String grievanceTypeId, String tenantId, String businessId);

    /**
     * Count total GrievanceTypes in tenant/business.
     *
     * @param tenantId Tenant context
     * @param businessId Business context
     * @return total count
     */
    long count(String tenantId, String businessId);
}
