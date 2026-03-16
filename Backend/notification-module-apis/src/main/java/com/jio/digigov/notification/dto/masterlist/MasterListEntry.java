package com.jio.digigov.notification.dto.masterlist;

import com.jio.digigov.notification.enums.MasterListDataSource;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Data Transfer Object representing a single entry in the master list configuration.
 *
 * Each master list entry defines how to resolve a specific master label to its actual value.
 * The resolution strategy depends on the configured data source (PAYLOAD, TOKEN, DB, GENERATE).
 *
 * Examples:
 * - PAYLOAD resolution: Extract "customerName" from eventPayload
 * - TOKEN resolution: Decrypt JWT and extract "consentId" from claims
 * - DB resolution: Query "ng_configurations" collection for "businessName" field
 * - GENERATE resolution: Generate OTP with specified length and format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterListEntry {

    /**
     * The data source type for this master list entry.
     * Determines which resolver will be used to extract the value.
     */
    private MasterListDataSource dataSource;

    /**
     * The database collection name for DB data source.
     * Only used when dataSource is DB.
     * Examples: "data_principals", "ng_configurations", "user_profiles"
     */
    private String collection;

    /**
     * The field path to extract the value from.
     * Supports dot notation for nested objects and header extraction.
     *
     * Examples:
     * - PAYLOAD: "customerIdentifiers.value", "eventPayload.customerName", "header.X-Tenant-ID", "header.X-Business-ID"
     * - TOKEN: "claims.consentId", "claims.user.profile.name"
     * - DB: "businessName", "user.profile.personal.name"
     * - GENERATE: Not used for generators
     */
    private String path;

    /**
     * The generator type for GENERATE data source.
     * Only used when dataSource is GENERATE.
     * Supported values: "OTP", "UUID", "TIMESTAMP", "RANDOM_STRING"
     */
    private String generator;

    /**
     * Dynamic query parameters for DB data source.
     * Only used when dataSource is DB.
     * Supports template variables like {{eventPayload.consentId}}
     *
     * Examples:
     * - {"consentId": "{{eventPayload.consentId}}"}
     * - {"userId": "{{eventPayload.user.id}}", "status": "{{eventPayload.user.status}}"}
     */
    private Map<String, String> query;

    /**
     * Configuration options for generators.
     * Only used when dataSource is GENERATE.
     *
     * Examples:
     * - OTP: {"length": 6, "numeric": true}
     * - UUID: {"version": "v4"}
     * - TIMESTAMP: {"format": "ISO_8601"}
     * - RANDOM_STRING: {"length": 10, "charset": "ALPHANUMERIC"}
     */
    private Map<String, Object> config;

    /**
     * Default value to use when resolution fails.
     * This value will be used instead of throwing an exception when:
     * 1. The master-list resolution configuration has use-default-values enabled
     * 2. The primary resolution mechanism fails (e.g., payload path not found, DB query returns no results)
     *
     * If defaultValue is null or empty string, an empty string will be used as the default.
     *
     * Examples:
     * - "Valued Customer" for customerName when not found in payload
     * - "Our Service" for businessName when not found in database
     * - "000000" for OTP when generation fails
     * - "" for optional fields that may not be present
     */
    private String defaultValue;

    /**
     * Validates that this master list entry has all required fields for its data source.
     *
     * @return true if the entry is valid, false otherwise
     */
    public boolean isValid() {
        if (dataSource == null) {
            return false;
        }

        switch (dataSource) {
            case PAYLOAD:
            case TOKEN:
                return path != null && !path.trim().isEmpty();
            case DB:
                return collection != null && !collection.trim().isEmpty()
                    && path != null && !path.trim().isEmpty();
            case GENERATE:
                return generator != null && !generator.trim().isEmpty();
            default:
                return false;
        }
    }

    /**
     * Check if this entry supports dynamic queries.
     *
     * @return true if entry has dynamic query parameters, false otherwise
     */
    public boolean hasDynamicQuery() {
        return query != null && !query.isEmpty();
    }

    /**
     * Gets the default value for this entry, ensuring it's never null.
     *
     * @return the default value, or empty string if defaultValue is null
     */
    public String getDefaultValue() {
        return defaultValue != null ? defaultValue : "";
    }

    /**
     * Checks if this entry has a meaningful default value.
     *
     * @return true if entry has a non-null, non-empty default value
     */
    public boolean hasDefaultValue() {
        return defaultValue != null && !defaultValue.trim().isEmpty();
    }

    /**
     * Gets a human-readable description of this master list entry.
     *
     * @return description string
     */
    public String getDescription() {
        switch (dataSource) {
            case PAYLOAD:
                if (path != null && path.startsWith("header.")) {
                    return String.format("Extract '%s' from request headers", path.substring(7));
                } else {
                    return String.format("Extract '%s' from request payload", path);
                }
            case TOKEN:
                return String.format("Extract '%s' from JWT token claims", path);
            case DB:
                String desc = String.format("Query '%s' collection for field '%s'", collection, path);
                if (hasDynamicQuery()) {
                    desc += " with dynamic query: " + query;
                }
                return desc;
            case GENERATE:
                return String.format("Generate value using '%s' generator", generator);
            default:
                return "Unknown data source";
        }
    }
}