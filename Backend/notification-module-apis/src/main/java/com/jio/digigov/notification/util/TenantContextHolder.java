package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for tenant context.
 *
 * ThreadLocal usage is intentional and necessary for multi-tenant applications
 * running in servlet containers. This is the standard pattern for maintaining
 * tenant isolation across concurrent HTTP requests in a thread-per-request model.
 * The clear() method is called after each request to prevent memory leaks.
 */
@Slf4j
public class TenantContextHolder {

    private TenantContextHolder() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    // ThreadLocal usage is intentional for multi-tenant request isolation
    private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> BUSINESS_CONTEXT = new ThreadLocal<>();
    
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        log.debug("Setting tenant context: {}", tenantId);
        TENANT_CONTEXT.set(tenantId);
    }
    
    public static void setBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("Business ID cannot be null or empty");
        }
        log.debug("Setting business context: {}", businessId);
        BUSINESS_CONTEXT.set(businessId);
    }
    
    public static String getTenantId() {
        String tenant = TENANT_CONTEXT.get();
        if (tenant == null) {
            log.warn("No tenant context found in current thread");
            return "default_tenant";
        }
        return tenant;
    }
    
    public static String getBusinessId() {
        String business = BUSINESS_CONTEXT.get();
        if (business == null) {
            log.warn("No business context found in current thread");
            return "default_business";
        }
        return business;
    }
    
    // Legacy methods for backward compatibility
    public static void setTenant(String tenantId) {
        setTenantId(tenantId);
    }
    
    public static String getTenant() {
        return getTenantId();
    }
    
    public static String getTenantOrDefault(String defaultTenant) {
        String tenant = TENANT_CONTEXT.get();
        return tenant != null ? tenant : defaultTenant;
    }
    
    public static void clear() {
        log.debug("Clearing tenant and business context");
        TENANT_CONTEXT.remove();
        BUSINESS_CONTEXT.remove();
    }
    
    public static boolean hasTenant() {
        return TENANT_CONTEXT.get() != null;
    }
    
    public static boolean hasBusiness() {
        return BUSINESS_CONTEXT.get() != null;
    }
}