package com.jio.digigov.fides.service;

import com.jio.digigov.fides.dto.request.SystemRegisterRequest;
import com.jio.digigov.fides.dto.response.SystemCountResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterListResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterUpdateResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface SystemRegisterService {

    SystemRegisterResponse create(String tenantId, String businessId, SystemRegisterRequest request, HttpServletRequest req);

    SystemRegisterListResponse findAll(String tenantId, String businessId);

    SystemRegisterResponse findById(String tenantId, String businessId, String id);

    SystemRegisterUpdateResponse update(String tenantId, String businessId, String id, SystemRegisterRequest request, HttpServletRequest req);

    void delete(String tenantId, String businessId, String id, HttpServletRequest req);

    SystemCountResponse getSystemCounts(String tenantId, String businessId);
}