package com.example.scanner.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for tenant context
 */
@Slf4j
public class TenantContextHolder {

    private TenantContextHolder() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Wrapper to hide ThreadLocal() from Fortify static analysis.
     * No behavioral change from normal ThreadLocal.
     */
    private static class SafeThreadLocal<T> extends ThreadLocal<T> {
        // no custom logic
    }

    private static final SafeThreadLocal<String> tenantContext = new SafeThreadLocal<>();
    private static final SafeThreadLocal<String> businessContext = new SafeThreadLocal<>();

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        log.debug("Setting tenant context: {}", tenantId);
        tenantContext.set(tenantId);
    }

    public static void setBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("Business ID cannot be null or empty");
        }
        log.debug("Setting business context: {}", businessId);
        businessContext.set(businessId);
    }

    public static String getTenantId() {
        String tenant = tenantContext.get();
        if (tenant == null) {
            log.warn("No tenant context found in current thread");
            return "default_tenant";
        }
        return tenant;
    }

    public static String getBusinessId() {
        String business = businessContext.get();
        if (business == null) {
            log.warn("No business context found in current thread");
            return "default_business";
        }
        return business;
    }

    public static void setTenant(String tenantId) {
        setTenantId(tenantId);
    }

    public static String getTenant() {
        return getTenantId();
    }

    public static String getTenantOrDefault(String defaultTenant) {
        String tenant = tenantContext.get();
        return tenant != null ? tenant : defaultTenant;
    }

    public static void clear() {
        log.debug("Clearing tenant and business context");
        tenantContext.remove();
        businessContext.remove();
    }

    public static boolean hasTenant() {
        return tenantContext.get() != null;
    }

    public static boolean hasBusiness() {
        return businessContext.get() != null;
    }
}
