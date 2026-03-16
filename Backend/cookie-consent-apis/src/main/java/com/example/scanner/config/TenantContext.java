package com.example.scanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-local storage for current tenant context in multi-tenant application.
 * This class provides a way to store and retrieve the current tenant ID
 * for database routing and tenant isolation.
 */
public class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    /**
     * Set the current tenant ID for the current thread
     * @param tenantId The tenant identifier
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("Attempting to set null or empty tenant ID");
            return;
        }

        currentTenant.set(tenantId.trim());
        log.debug("Set current tenant to: {}", tenantId);
    }

    /**
     * Get the current tenant ID for the current thread
     * @return The current tenant ID, or null if not set
     */
    public static String getCurrentTenant() {
        String tenantId = currentTenant.get();
        log.debug("Retrieved current tenant: {}", tenantId);
        return tenantId;
    }

    /**
     * Clear the current tenant context for the current thread
     * This should always be called after tenant-specific operations
     * to prevent tenant leakage between requests
     */
    public static void clear() {
        String previousTenant = currentTenant.get();
        currentTenant.remove();
        log.debug("Cleared tenant context (was: {})", previousTenant);
    }

    /**
     * Check if a tenant is currently set
     * @return true if a tenant is set, false otherwise
     */
    public static boolean hasTenant() {
        return currentTenant.get() != null;
    }

    /**
     * Get the current tenant or return a default value
     * @param defaultTenant The default tenant to return if none is set
     * @return The current tenant or the default
     */
    public static String getCurrentTenantOrDefault(String defaultTenant) {
        String tenant = getCurrentTenant();
        return tenant != null ? tenant : defaultTenant;
    }
}