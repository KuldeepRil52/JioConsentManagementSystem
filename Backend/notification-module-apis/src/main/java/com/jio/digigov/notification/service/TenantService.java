package com.jio.digigov.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TenantService {

    public void validateTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        log.debug("Validating tenant: {}", tenantId);
    }

    public void validateBusiness(String businessId, String tenantId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("Business ID cannot be null or empty");
        }
        
        log.debug("Validating business: {} for tenant: {}", businessId, tenantId);
    }

    public String getDatabaseName(String tenantId) {
        return "tenant_db_" + tenantId;
    }
}