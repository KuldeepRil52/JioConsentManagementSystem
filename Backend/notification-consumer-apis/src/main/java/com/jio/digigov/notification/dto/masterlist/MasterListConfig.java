package com.jio.digigov.notification.dto.masterlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Transfer Object representing the complete master list configuration.
 *
 * This configuration defines how template arguments (master labels) should be resolved
 * to their actual values. The configuration can be loaded from:
 * 1. Static file: src/main/resources/master-list-config.json
 * 2. Database override: master_list_config collection in tenant database
 *
 * The database configuration takes precedence over the static file configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterListConfig {

    /**
     * Map of master labels to their configuration entries.
     * Key: Master label (e.g., "MASTER_LABEL_USER_NAME")
     * Value: Configuration entry defining how to resolve the value
     */
    @Builder.Default
    private Map<String, MasterListEntry> entries = new HashMap<>();

    /**
     * Metadata fields for database-stored configurations
     */
    private String businessId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String source; // "FILE" or "DATABASE"

    /**
     * Gets the master list entry for a given master label.
     *
     * @param masterLabel the master label to lookup
     * @return the corresponding entry, or null if not found
     */
    public MasterListEntry getEntry(String masterLabel) {
        return entries.get(masterLabel);
    }

    /**
     * Checks if a master label exists in the configuration.
     *
     * @param masterLabel the master label to check
     * @return true if the label exists, false otherwise
     */
    public boolean hasEntry(String masterLabel) {
        return entries.containsKey(masterLabel);
    }

    /**
     * Adds or updates a master list entry.
     *
     * @param masterLabel the master label
     * @param entry the configuration entry
     */
    public void putEntry(String masterLabel, MasterListEntry entry) {
        entries.put(masterLabel, entry);
    }

    /**
     * Gets the total number of configured master labels.
     *
     * @return the number of entries
     */
    public int size() {
        return entries.size();
    }

    /**
     * Validates that all entries in this configuration are valid.
     *
     * @return true if all entries are valid, false otherwise
     */
    public boolean isValid() {
        return entries.values().stream().allMatch(MasterListEntry::isValid);
    }

    /**
     * Creates a summary of the configuration for logging purposes.
     *
     * @return configuration summary
     */
    public ConfigurationSummary getSummary() {
        ConfigurationSummary summary = new ConfigurationSummary();

        for (MasterListEntry entry : entries.values()) {
            switch (entry.getDataSource()) {
                case PAYLOAD:
                    summary.payloadCount++;
                    break;
                case TOKEN:
                    summary.tokenCount++;
                    break;
                case DB:
                    summary.dbCount++;
                    break;
                case GENERATE:
                    summary.generateCount++;
                    break;
            }
        }

        summary.totalCount = entries.size();
        summary.source = this.source;
        summary.businessId = this.businessId;

        return summary;
    }

    /**
     * Inner class representing a summary of the master list configuration.
     */
    @Data
    public static class ConfigurationSummary {
        private int totalCount;
        private int payloadCount;
        private int tokenCount;
        private int dbCount;
        private int generateCount;
        private String source;
        private String businessId;
    }
}