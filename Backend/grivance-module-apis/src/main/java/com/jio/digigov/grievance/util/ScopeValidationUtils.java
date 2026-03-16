package com.jio.digigov.grievance.util;

import com.jio.digigov.grievance.enumeration.ScopeLevel;

public class ScopeValidationUtils {

    /**
     * Validates the logical rule for scopeLevel based on tenantId and businessId.
     * Throws IllegalArgumentException if invalid.
     */
    public static void validateScope(String tenantId, String businessId, ScopeLevel scopeLevel) {

        if (tenantId.equals(businessId)) {
            // tenant-level case → must be TENANT
            if (scopeLevel != ScopeLevel.TENANT) {
                throw new IllegalArgumentException(
                        "Invalid Scope: When tenantId equals businessId, scopeLevel must be TENANT."
                );
            }
        } else {
            // business-level case → must be BUSINESS
            if (scopeLevel != ScopeLevel.BUSINESS) {
                throw new IllegalArgumentException(
                        "Invalid Scope: When tenantId differs from businessId, scopeLevel must be BUSINESS."
                );
            }
        }
    }
}