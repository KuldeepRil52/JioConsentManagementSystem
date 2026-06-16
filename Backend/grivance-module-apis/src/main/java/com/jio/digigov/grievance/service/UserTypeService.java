package com.jio.digigov.grievance.service;

import com.jio.digigov.grievance.dto.request.UserTypeUpdateRequest;
import com.jio.digigov.grievance.entity.UserType;
import com.jio.digigov.grievance.enumeration.ScopeLevel;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing UserType with multi-tenant support.
 */
public interface UserTypeService {

    /**
     * Create a new UserType for a given tenant and business.
     */
    UserType create(UserType u, String tenantId, String businessId, ScopeLevel scopeLevel);

    /**
     * List all UserTypes for a given tenant, business and Scope.
     */
    List<UserType> list(String tenantId, String businessId, ScopeLevel scopeLevel);

    /**
     * Get a UserType by its ID for a given tenant and business.
     */
    Optional<UserType> getById(String userTypeId, String tenantId, String businessId, ScopeLevel scopeLevel);

    /**
     * Update an existing UserType for a given tenant and business.
     */
    Optional<UserType> update(String userTypeId, UserTypeUpdateRequest update, String tenantId, String businessId);

    /**
     * Delete a UserType by its ID for a given tenant and business.
     */
    boolean delete(String userTypeId, String tenantId, String businessId);

    /**
     * Count total UserTypes for a given tenant and business.
     */
    long count(String tenantId, String businessId);
}
