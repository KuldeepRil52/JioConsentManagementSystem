package com.jio.digigov.notification.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for providing tenant-specific MongoTemplate instances.
 *
 * This component manages the creation and caching of MongoTemplate instances
 * for different tenants in the multi-tenant notification system. It ensures
 * proper database isolation while maintaining performance through caching.
 *
 * Multi-Tenant Strategy:
 * - Database-per-tenant: Each tenant gets a separate MongoDB database
 * - Database naming: tenant_db_{tenantId} (e.g., tenant_db_tenant1)
 * - Connection pooling: Shared connection pool with tenant-specific databases
 * - Template caching: MongoTemplate instances cached for performance
 *
 * Features:
 * - Thread-safe template caching with ConcurrentHashMap
 * - Lazy initialization of tenant databases
 * - Automatic database creation if not exists
 * - Memory-efficient template reuse
 * - Proper resource cleanup capabilities
 *
 * Usage Example:
 * ```java
 * MongoTemplate tenantTemplate = mongoTemplateProvider.getTemplate("tenant123");
 * userRepository.save(user, tenantTemplate);
 * ```
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Component
@Slf4j
public class MongoTemplateProvider {

    private final MongoTemplate defaultMongoTemplate;
    private final MongoTemplate sharedMongoTemplate;
    private final Map<String, MongoTemplate> templateCache = new ConcurrentHashMap<>();

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    public MongoTemplateProvider(@Qualifier("tenantMongoTemplate") MongoTemplate defaultMongoTemplate,
                                 @Qualifier("sharedMongoTemplate") MongoTemplate sharedMongoTemplate) {
        this.defaultMongoTemplate = defaultMongoTemplate;
        this.sharedMongoTemplate = sharedMongoTemplate;
        log.info("MongoTemplateProvider initialized with defaultMongoTemplate: {}, sharedMongoTemplate: {}",
                 defaultMongoTemplate.getClass().getName(), sharedMongoTemplate.getClass().getName());
    }

    /**
     * Gets a MongoTemplate instance for the specified tenant.
     * Creates and caches the template if it doesn't exist.
     *
     * Special handling:
     * - If tenantId is "SYSTEM", routes to shared database (tenant_db_shared)
     * - Otherwise, routes to tenant-specific database (tenant_db_{tenantId})
     *
     * @param tenantId The tenant identifier
     * @return MongoTemplate instance for the tenant or shared database
     * @throws IllegalArgumentException if tenantId is null or empty
     */
    public MongoTemplate getTemplate(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }

        // Route system-wide notifications to shared database
        if ("SYSTEM".equalsIgnoreCase(tenantId)) {
            log.debug("Routing to shared database for system notification (tenantId: SYSTEM)");
            return sharedMongoTemplate;
        }

        // Regular tenant routing
        return templateCache.computeIfAbsent(tenantId, this::createTenantTemplate);
    }

    /**
     * Creates a new MongoTemplate instance for the specified tenant.
     *
     * @param tenantId The tenant identifier
     * @return New MongoTemplate instance configured for the tenant
     */
    private MongoTemplate createTenantTemplate(String tenantId) {
        try {
            log.debug("Creating MongoTemplate for tenant: {}", tenantId);

            // Generate tenant-specific database name
            String tenantDatabaseName = "tenant_db_" + tenantId;

            // Get the connection string from the default template
            String baseConnectionString = getConnectionString();

            // Construct tenant-specific connection string
            String tenantConnectionString;
            if (baseConnectionString.contains("?")) {
                // Insert database name before query parameters
                String[] parts = baseConnectionString.split("\\?", 2);
                tenantConnectionString = parts[0] + "/" + tenantDatabaseName + "?" + parts[1];
            } else {
                // Simple case: just append database name
                tenantConnectionString = baseConnectionString + "/" + tenantDatabaseName;
            }

            // Create tenant-specific database factory
            SimpleMongoClientDatabaseFactory databaseFactory = new SimpleMongoClientDatabaseFactory(
                    tenantConnectionString
            );

            // Create and configure MongoTemplate
            MongoTemplate tenantTemplate = new MongoTemplate(databaseFactory);

            log.info("Created MongoTemplate for tenant: {} with database: {}", tenantId, tenantDatabaseName);
            return tenantTemplate;

        } catch (Exception e) {
            log.error("Failed to create MongoTemplate for tenant: {}", tenantId);
            throw new RuntimeException("Failed to create MongoTemplate for tenant: " + tenantId, e);
        }
    }

    /**
     * Gets the MongoDB connection string from application configuration.
     * Extracts the base URI without database name for tenant-specific connections.
     */
    private String getConnectionString() {
        // Extract base connection string without database name
        if (mongoUri.contains("?")) {
            // Handle URI with query parameters
            String[] parts = mongoUri.split("\\?");
            String baseUri = parts[0];
            String queryParams = parts[1];

            // Remove database name from base URI if present
            if (baseUri.contains("/") && baseUri.lastIndexOf("/") > baseUri.lastIndexOf("@")) {
                baseUri = baseUri.substring(0, baseUri.lastIndexOf("/"));
            }

            return baseUri + "?" + queryParams;
        } else {
            // Handle simple URI without query parameters
            if (mongoUri.contains("/") && mongoUri.lastIndexOf("/") > mongoUri.lastIndexOf("@")) {
                return mongoUri.substring(0, mongoUri.lastIndexOf("/"));
            }
            return mongoUri;
        }
    }

    /**
     * Gets the default MongoTemplate (for system-wide operations).
     *
     * @return Default MongoTemplate instance
     */
    public MongoTemplate getDefaultTemplate() {
        return defaultMongoTemplate;
    }

    /**
     * Checks if a tenant template exists in the cache.
     *
     * @param tenantId The tenant identifier
     * @return true if template exists in cache
     */
    public boolean hasTemplate(String tenantId) {
        return templateCache.containsKey(tenantId);
    }

    /**
     * Removes a tenant template from the cache.
     * Useful for cleanup or when tenant configuration changes.
     *
     * @param tenantId The tenant identifier
     * @return true if template was removed, false if it didn't exist
     */
    public boolean hasRemovedTemplate(String tenantId) {
        MongoTemplate removed = templateCache.remove(tenantId);
        if (removed != null) {
            log.info("Removed MongoTemplate for tenant: {}", tenantId);
            return true;
        }
        return false;
    }

    /**
     * Clears all cached templates.
     * Useful for testing or system restart scenarios.
     */
    public void clearCache() {
        int count = templateCache.size();
        templateCache.clear();
        log.info("Cleared {} MongoTemplate instances from cache", count);
    }

    /**
     * Gets the number of cached templates.
     *
     * @return Number of cached templates
     */
    public int getCacheSize() {
        return templateCache.size();
    }

    /**
     * Gets a copy of all cached tenant IDs.
     *
     * @return Set of tenant IDs with cached templates
     */
    public Set<String> getCachedTenants() {
        return new HashSet<>(templateCache.keySet());
    }
}