package com.jio.digigov.notification.service.masterlist.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.enums.CacheType;
import com.jio.digigov.notification.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolver for extracting values from tenant-specific database collections.
 *
 * This resolver queries MongoDB collections in tenant-specific databases using
 * businessId as the primary query field. It supports:
 * - Batch queries per collection for improved performance
 * - Nested field extraction using dot notation
 * - Multiple master list entries resolved in a single database operation
 *
 * Database Query Strategy:
 * - Always queries tenant-specific database: cms_db_{tenantId}
 * - Always uses businessId as the query field: {"businessId": "business123"}
 * - Supports batch resolution of multiple arguments from the same collection
 * - Extracts field values using dot notation: "user.profile.name"
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseResolver {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;
    private final CacheService<String> cacheService;

    /**
     * Batch resolves multiple database entries for improved performance.
     * Groups entries by collection and executes one query per collection.
     * Implements caching to avoid repeated database queries for same values.
     *
     * @param entries map of argument keys to master list entries (all must be DB type)
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return map of argument keys to resolved values
     * @throws DatabaseResolutionException if resolution fails
     */
    public Map<String, String> batchResolve(Map<String, MasterListEntry> entries,
                                           String tenantId, String businessId) {
        if (entries == null || entries.isEmpty()) {
            return new HashMap<>();
        }

        log.debug("Batch resolving {} database entries for tenantId={}, businessId={}",
                 entries.size(), tenantId, businessId);

        try {
            return processBatchResolutionWithCache(entries, tenantId, businessId);
        } catch (Exception e) {
            log.error("Error during cached batch database resolution for tenantId={}, businessId={}: {}",
                     tenantId, businessId, e.getMessage(), e);

            log.warn("Falling back to direct database resolution for tenantId={}, businessId={}",
                    tenantId, businessId);

            return resolveBatchFromDatabase(entries, tenantId, businessId);
        }
    }

    /**
     * Resolves database entries directly from database without caching.
     *
     * @param entries map of argument keys to master list entries
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return map of argument keys to resolved values
     * @throws DatabaseResolutionException if resolution fails
     */
    private Map<String, String> resolveBatchFromDatabase(Map<String, MasterListEntry> entries,
                                                        String tenantId, String businessId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Map<String, String> results = new HashMap<>();

            // Group entries by collection for batch processing
            Map<String, List<ArgumentEntry>> entriesByCollection = groupEntriesByCollection(entries);

            for (Map.Entry<String, List<ArgumentEntry>> collectionGroup : entriesByCollection.entrySet()) {
                String collection = collectionGroup.getKey();
                List<ArgumentEntry> argumentEntries = collectionGroup.getValue();

                log.debug("Querying collection '{}' for {} arguments", collection, argumentEntries.size());

                try {
                    // Execute single query for this collection
                    Map<String, String> collectionResults = queryCollection(
                        mongoTemplate, collection, argumentEntries, businessId);
                    results.putAll(collectionResults);

                } catch (Exception e) {
                    log.error("Failed to query collection '{}' for tenantId={}, businessId={}: {}",
                             collection, tenantId, businessId, e.getMessage(), e);

                    // Mark all arguments from this collection as failed
                    for (ArgumentEntry argEntry : argumentEntries) {
                        results.put(argEntry.argumentKey, null);
                    }
                }
            }

            return results;

        } catch (Exception e) {
            log.error("Database resolution failed for tenantId={}, businessId={}: {}",
                     tenantId, businessId, e.getMessage(), e);
            throw new DatabaseResolutionException("Database resolution failed", e);
        }
    }

    /**
     * Resolves a single database entry.
     *
     * @param entry the master list entry (must be DB type)
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return the resolved value, or null if not found
     * @throws DatabaseResolutionException if resolution fails
     */
    public String resolve(MasterListEntry entry, String tenantId, String businessId) {
        Map<String, MasterListEntry> singleEntry = new HashMap<>();
        singleEntry.put("single", entry);

        Map<String, String> results = batchResolve(singleEntry, tenantId, businessId);
        return results.get("single");
    }

    /**
     * Groups master list entries by their collection name for batch processing.
     *
     * @param entries the master list entries
     * @return map of collection names to argument entries
     */
    private Map<String, List<ArgumentEntry>> groupEntriesByCollection(Map<String, MasterListEntry> entries) {
        Map<String, List<ArgumentEntry>> grouped = new HashMap<>();

        for (Map.Entry<String, MasterListEntry> entry : entries.entrySet()) {
            String argumentKey = entry.getKey();
            MasterListEntry listEntry = entry.getValue();

            if (listEntry.getCollection() == null || listEntry.getCollection().trim().isEmpty()) {
                log.warn("Invalid DB entry - missing collection: {}", argumentKey);
                continue;
            }

            ArgumentEntry argEntry = new ArgumentEntry(argumentKey, listEntry);
            grouped.computeIfAbsent(listEntry.getCollection(), k -> new ArrayList<>()).add(argEntry);
        }

        return grouped;
    }

    /**
     * Queries a single collection and extracts values for multiple arguments.
     *
     * @param mongoTemplate the MongoDB template
     * @param collection the collection name
     * @param argumentEntries the arguments to resolve from this collection
     * @param businessId the business identifier
     * @return map of argument keys to resolved values
     */
    private Map<String, String> queryCollection(MongoTemplate mongoTemplate, String collection,
                                               List<ArgumentEntry> argumentEntries, String businessId) {
        Map<String, String> results = new HashMap<>();

        try {
            // Build query for businessId
            Query query = new Query().addCriteria(Criteria.where("businessId").is(businessId));

            // Execute query
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) (List<?>) mongoTemplate.find(query, Map.class, collection);

            if (documents.isEmpty()) {
                log.debug("No documents found in collection '{}' for businessId '{}'", collection, businessId);

                // Mark all arguments as not found
                for (ArgumentEntry argEntry : argumentEntries) {
                    results.put(argEntry.argumentKey, null);
                }
                return results;
            }

            if (documents.size() > 1) {
                log.warn("Multiple documents found in collection '{}' for businessId '{}', using first one",
                        collection, businessId);
            }

            // Use the first document found
            Map<String, Object> document = documents.get(0);

            // Extract values for each argument
            for (ArgumentEntry argEntry : argumentEntries) {
                String value = extractFieldValue(document, argEntry.entry.getPath());
                results.put(argEntry.argumentKey, value);

                log.debug("Extracted value for '{}' from collection '{}': '{}'",
                         argEntry.argumentKey, collection, value);
            }

        } catch (Exception e) {
            log.error("Error querying collection '{}' with businessId '{}': {}",
                     collection, businessId, e.getMessage(), e);
            throw e;
        }

        return results;
    }

    /**
     * Extracts a field value from a document using dot notation.
     *
     * @param document the MongoDB document
     * @param path the dot-notation path to the field
     * @return the field value as a string, or null if not found
     */
    private String extractFieldValue(Map<String, Object> document, String path) {
        if (document == null || path == null || path.trim().isEmpty()) {
            return null;
        }

        try {
            Object value = navigateDocumentPath(document, path);

            if (value == null) {
                log.debug("Field not found at path: {}", path);
                return null;
            }

            return convertToString(value);

        } catch (Exception e) {
            log.debug("Error extracting field value at path '{}': {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Navigates through a document structure using dot notation.
     *
     * @param document the root document
     * @param path the dot-notation path
     * @return the value at the path, or null if not found
     */
    private Object navigateDocumentPath(Map<String, Object> document, String path) {
        String[] pathSegments = path.split("\\.");
        Object current = document;

        for (String segment : pathSegments) {
            if (current == null) {
                return null;
            }

            current = navigateSegment(current, segment);
        }

        return current;
    }

    /**
     * Navigates a single path segment in a document structure.
     *
     * @param obj the current object
     * @param segment the path segment
     * @return the value at the segment, or null if not found
     */
    private Object navigateSegment(Object obj, String segment) {
        if (obj == null) {
            return null;
        }

        // Handle Map objects (most common in MongoDB documents)
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return map.get(segment);
        }

        // Handle List objects (for array access)
        if (obj instanceof List && segment.matches("\\d+")) {
            try {
                int index = Integer.parseInt(segment);
                List<?> list = (List<?>) obj;
                if (index >= 0 && index < list.size()) {
                    return list.get(index);
                }
            } catch (Exception e) {
                log.debug("Failed to access list index '{}': {}", segment, e.getMessage());
            }
        }

        // For other object types, try JSON navigation
        try {
            JsonNode jsonNode = objectMapper.valueToTree(obj);
            JsonNode childNode = jsonNode.get(segment);
            if (childNode != null && !childNode.isNull()) {
                return objectMapper.treeToValue(childNode, Object.class);
            }
        } catch (Exception e) {
            log.debug("Failed to navigate segment '{}' using JSON: {}", segment, e.getMessage());
        }

        return null;
    }

    /**
     * Converts an object to its string representation.
     *
     * @param value the value to convert
     * @return the string representation
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        }

        // For complex objects, convert to JSON string
        if (isComplexObject(value)) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                log.debug("Failed to convert object to JSON, using toString(): {}", e.getMessage());
                return value.toString();
            }
        }

        return value.toString();
    }

    /**
     * Checks if an object needs JSON serialization.
     *
     * @param value the value to check
     * @return true if the object is complex, false otherwise
     */
    private boolean isComplexObject(Object value) {
        if (value == null) {
            return false;
        }

        Class<?> clazz = value.getClass();

        return !clazz.isPrimitive()
            && !Number.class.isAssignableFrom(clazz)
            && !Boolean.class.isAssignableFrom(clazz)
            && !Character.class.isAssignableFrom(clazz)
            && !(value instanceof String)
            && !clazz.isEnum();
    }

    /**
     * Validates a collection name.
     *
     * @param collection the collection name to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCollection(String collection) {
        return collection != null
            && !collection.trim().isEmpty()
            && collection.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }

    /**
     * Validates a field path.
     *
     * @param path the path to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        return !path.contains("..")
            && !path.startsWith(".")
            && !path.endsWith(".")
            && path.matches("^[a-zA-Z][a-zA-Z0-9_.\\[\\]]*$");
    }

    /**
     * Inner class to hold argument key and master list entry pairs.
     */
    private static class ArgumentEntry {
        final String argumentKey;
        final MasterListEntry entry;

        ArgumentEntry(String argumentKey, MasterListEntry entry) {
            this.argumentKey = argumentKey;
            this.entry = entry;
        }
    }

    /**
     * Resolves a database entry using dynamic queries with event payload.
     *
     * @param entry the master list entry with dynamic query configuration
     * @param eventPayload the event payload for template variable resolution
     * @param tenantId the tenant identifier
     * @return the resolved value, or null if not found
     * @throws DatabaseResolutionException if resolution fails
     */
    public String resolveDynamicQuery(MasterListEntry entry, Map<String, Object> eventPayload, String tenantId) {
        if (entry == null || entry.getCollection() == null) {
            throw new DatabaseResolutionException("Invalid entry or missing collection");
        }

        log.debug("Resolving dynamic query for collection '{}' with payload keys: {}",
                 entry.getCollection(), eventPayload != null ? eventPayload.keySet() : "null");

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Build dynamic query
            Query query = new Query();

            // Process dynamic query parameters if present
            if (entry.hasDynamicQuery()) {
                for (Map.Entry<String, String> param : entry.getQuery().entrySet()) {
                    String key = param.getKey();
                    String template = param.getValue();

                    String resolvedValue = resolveTemplateValue(template, eventPayload);
                    if (resolvedValue != null) {
                        query.addCriteria(Criteria.where(key).is(resolvedValue));
                        log.debug("Added dynamic criteria: {} = {}", key, resolvedValue);
                    } else {
                        log.warn("Could not resolve template value '{}' from payload", template);
                        // Still add the parameter but it might not match anything
                        query.addCriteria(Criteria.where(key).is(template));
                    }
                }
            }

            // Execute query
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) (List<?>)
                mongoTemplate.find(query, Map.class, entry.getCollection());

            if (documents.isEmpty()) {
                log.debug("No documents found for dynamic query in collection '{}'", entry.getCollection());
                return null;
            }

            if (documents.size() > 1) {
                log.warn("Multiple documents found for dynamic query in collection '{}', using first one",
                        entry.getCollection());
            }

            // Extract field value from the first document
            Map<String, Object> document = documents.get(0);
            String value = extractFieldValue(document, entry.getPath());

            log.debug("Resolved dynamic query value: '{}'", value);
            return value;

        } catch (Exception e) {
            log.error("Dynamic query resolution failed for collection '{}': {}",
                     entry.getCollection(), e.getMessage(), e);
            throw new DatabaseResolutionException("Dynamic query resolution failed", e);
        }
    }

    /**
     * Resolves template values from event payload.
     *
     * @param template the template string (e.g., "{{eventPayload.consentId}}")
     * @param eventPayload the event payload
     * @return the resolved value, or null if not found
     */
    private String resolveTemplateValue(String template, Map<String, Object> eventPayload) {
        if (template == null || eventPayload == null) {
            return null;
        }

        // Handle template syntax: {{eventPayload.consentId}}
        if (template.startsWith("{{") && template.endsWith("}}")) {
            String path = template.substring(2, template.length() - 2).trim();
            return extractValueFromEventPayload(eventPayload, path);
        }

        // Return as-is if not a template
        return template;
    }

    /**
     * Extracts a value from event payload using dot notation path.
     *
     * @param payload the event payload
     * @param path the dot notation path
     * @return the extracted value as string, or null if not found
     */
    private String extractValueFromEventPayload(Map<String, Object> payload, String path) {
        String[] pathSegments = path.split("\\.");
        Object current = payload;

        for (String segment : pathSegments) {
            if (current == null) {
                return null;
            }

            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(segment);
            } else {
                // Try to access as object property using reflection or JSON
                current = navigateSegment(current, segment);
            }
        }

        return current != null ? current.toString() : null;
    }

    /**
     * Builds cache key for database query results.
     * Format: db-queries:{collection}:{businessId}:{path}
     *
     * @param collection the collection name
     * @param path the field path
     * @param businessId the business identifier
     * @return the cache key
     */
    private String buildDatabaseCacheKey(String collection, String path, String businessId) {
        return String.format("db-queries:%s:%s:%s", collection, businessId, path);
    }

    /**
     * Evicts cached database values for a specific business and tenant.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    public void evictBusinessCache(String tenantId, String businessId) {
        cacheService.evictByType(CacheType.MASTER_LIST_DB_VALUE, tenantId, businessId);

        log.info("Evicted all cached database values for tenant: {}, business: {}",
                tenantId, businessId);
    }

    /**
     * Evicts cached database values for a specific collection and business.
     *
     * @param collection the collection name
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    public void evictCollectionCache(String collection, String tenantId, String businessId) {
        // Note: Current cache service doesn't support pattern-based eviction
        // This would require iterating through cache keys or implementing pattern matching
        log.warn("Collection-specific cache eviction not implemented yet for collection: {}", collection);

        // For now, evict all database cache entries for this tenant/business
        evictBusinessCache(tenantId, businessId);
    }

    /**
     * Evicts a specific cached database value.
     *
     * @param collection the collection name
     * @param path the field path
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    public void evictSpecificCache(String collection, String path, String tenantId, String businessId) {
        String cacheKey = buildDatabaseCacheKey(collection, path, businessId);
        cacheService.evict(cacheKey, tenantId, businessId);

        log.debug("Evicted cached database value for collection: {}, path: {}, tenant: {}, business: {}",
                 collection, path, tenantId, businessId);
    }

    /**
     * Checks if a database query result is cached.
     *
     * @param collection the collection name
     * @param path the field path
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return true if cached value exists
     */
    public boolean isValueCached(String collection, String path, String tenantId, String businessId) {
        String cacheKey = buildDatabaseCacheKey(collection, path, businessId);
        return cacheService.exists(cacheKey, tenantId, businessId);
    }

    /**
     * Preloads database values into cache for given master list entries.
     *
     * @param entries the master list entries to preload
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return true if all values were successfully preloaded
     */
    public boolean preloadValues(Map<String, MasterListEntry> entries, String tenantId, String businessId) {
        try {
            // This will load values and cache them
            Map<String, String> results = batchResolve(entries, tenantId, businessId);

            long successCount = results.values().stream().mapToLong(v -> v != null ? 1 : 0).sum();
            log.debug("Successfully preloaded {}/{} database values for tenant: {}, business: {}",
                     successCount, entries.size(), tenantId, businessId);

            return successCount == entries.size();
        } catch (Exception e) {
            log.warn("Failed to preload database values for tenant: {}, business: {}: {}",
                    tenantId, businessId, e.getMessage());
            return false;
        }
    }

    /**
     * Gets cache statistics for database query result caching.
     *
     * @return cache statistics
     */
    public Object getDatabaseCacheStats() {
        return cacheService.getStats(CacheType.MASTER_LIST_DB_VALUE);
    }

    private Map<String, String> processBatchResolutionWithCache(Map<String, MasterListEntry> entries,
                                                               String tenantId, String businessId) {
        Map<String, String> results = new HashMap<>();
        Map<String, MasterListEntry> uncachedEntries = new HashMap<>();

        // Step 1: Check cache for existing values
        checkCacheForEntries(entries, results, uncachedEntries, tenantId, businessId);

        // Step 2: Resolve uncached entries from database
        if (!uncachedEntries.isEmpty()) {
            resolveDatabaseAndCache(uncachedEntries, results, tenantId, businessId);
        }

        logBatchResolutionResults(entries, uncachedEntries, results);
        return results;
    }

    private void checkCacheForEntries(Map<String, MasterListEntry> entries,
                                    Map<String, String> results,
                                    Map<String, MasterListEntry> uncachedEntries,
                                    String tenantId, String businessId) {
        for (Map.Entry<String, MasterListEntry> entry : entries.entrySet()) {
            String argumentKey = entry.getKey();
            MasterListEntry listEntry = entry.getValue();

            String cacheKey = buildDatabaseCacheKey(listEntry.getCollection(), listEntry.getPath(), businessId);
            Optional<String> cachedValue = cacheService.get(cacheKey, tenantId, businessId);

            if (cachedValue.isPresent()) {
                results.put(argumentKey, cachedValue.get());
                log.debug("Cache hit for DB value: argumentKey={}, collection={}, path={}",
                         argumentKey, listEntry.getCollection(), listEntry.getPath());
            } else {
                uncachedEntries.put(argumentKey, listEntry);
                log.debug("Cache miss for DB value: argumentKey={}, collection={}, path={}",
                         argumentKey, listEntry.getCollection(), listEntry.getPath());
            }
        }
    }

    private void resolveDatabaseAndCache(Map<String, MasterListEntry> uncachedEntries,
                                       Map<String, String> results,
                                       String tenantId, String businessId) {
        log.debug("Resolving {} uncached database entries", uncachedEntries.size());
        Map<String, String> dbResults = resolveBatchFromDatabase(uncachedEntries, tenantId, businessId);

        for (Map.Entry<String, String> dbResult : dbResults.entrySet()) {
            String argumentKey = dbResult.getKey();
            String resolvedValue = dbResult.getValue();
            MasterListEntry listEntry = uncachedEntries.get(argumentKey);

            if (listEntry != null) {
                String cacheKey = buildDatabaseCacheKey(listEntry.getCollection(), listEntry.getPath(), businessId);
                cacheService.put(cacheKey, resolvedValue, tenantId, businessId, CacheType.MASTER_LIST_DB_VALUE);

                log.debug("Cached DB value: argumentKey={}, collection={}, path={}, value={}",
                         argumentKey, listEntry.getCollection(), listEntry.getPath(), resolvedValue);
            }

            results.put(argumentKey, resolvedValue);
        }
    }

    private void logBatchResolutionResults(Map<String, MasterListEntry> entries,
                                         Map<String, MasterListEntry> uncachedEntries,
                                         Map<String, String> results) {
        long cachedCount = entries.size() - uncachedEntries.size();
        long resolvedCount = results.values().stream().mapToLong(v -> v != null ? 1 : 0).sum();

        log.debug("Batch database resolution completed. Cached: {}, Resolved: {}/{} arguments successfully",
                 cachedCount, resolvedCount, entries.size());
    }

    /**
     * Custom exception for database resolution errors.
     */
    public static class DatabaseResolutionException extends RuntimeException {
        public DatabaseResolutionException(String message) {
            super(message);
        }

        public DatabaseResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}