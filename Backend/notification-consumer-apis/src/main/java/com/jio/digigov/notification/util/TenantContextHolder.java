package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for tenant context in multi-tenant application.
 *
 * THREAD MANAGEMENT JUSTIFICATION (J2EE Environment):
 * This class intentionally uses ThreadLocal to maintain tenant and business context
 * across the request lifecycle. In a J2EE/Spring Boot environment with request-scoped
 * processing, ThreadLocal is the appropriate pattern for:
 *
 * 1. Request Isolation: Each HTTP request is processed in a separate thread, ensuring
 *    tenant data is isolated between concurrent requests from different tenants.
 *
 * 2. Context Propagation: Allows tenant context to be transparently available throughout
 *    the request processing chain (controllers → services → repositories) without
 *    explicit parameter passing.
 *
 * 3. Multi-Tenant Database Routing: Enables dynamic MongoDB database selection based on
 *    the tenant context stored in this ThreadLocal.
 *
 * IMPORTANT: This ThreadLocal MUST be cleared after each request to prevent:
 * - Memory leaks in thread pools
 * - Context bleeding between requests
 * - Incorrect tenant data access
 *
 * The clear() method should be called in:
 * - Finally blocks of request handlers
 * - Spring interceptors/filters after request completion
 * - Kafka listeners after message processing
 *
 * This is a well-documented pattern for multi-tenant applications and is safe when
 * properly managed with cleanup after each request/operation.
 */
@Slf4j
public class TenantContextHolder {

    private TenantContextHolder() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * ThreadLocal for tenant ID - Intentional use for request-scoped context isolation
     */
    private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

    /**
     * ThreadLocal for business ID - Intentional use for request-scoped context isolation
     */
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