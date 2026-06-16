package com.jio.digigov.grievance.service;

import com.jio.digigov.grievance.dto.request.UserDetailUpdateRequest;
import com.jio.digigov.grievance.entity.UserDetail;
import com.jio.digigov.grievance.enumeration.ScopeLevel;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing UserDetail with multi-tenant support.
 */
public interface UserDetailService {

    UserDetail create(UserDetail u, String tenantId, String businessId, ScopeLevel scopeLevel);

    List<UserDetail> list(String tenantId, String businessId, ScopeLevel scopeLevel);

    Optional<UserDetail> getById(String userDetailId, String tenantId, String businessId);

    Optional<UserDetail> update(String userDetailId, UserDetailUpdateRequest update, String tenantId, String businessId);

    boolean delete(String userDetailId, String tenantId, String businessId);

    long count(String tenantId, String businessId);
}
