package com.jio.digigov.notification.entity;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.masterlist.EventMappingConfig;
import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

/**
 * Entity representing tenant-specific master list configuration.
 *
 * This entity stores custom master list configurations that override
 * the default static configuration file. It's stored in tenant-specific
 * databases and applies to all businesses within that tenant.
 *
 * The fallback strategy is:
 * 1. Check tenant-specific database configuration (one record per tenant)
 * 2. Fall back to static file configuration
 *
 * Note: This entity is stored in tenant-specific databases, so tenantId
 * is not needed as a field. Only one record should exist per tenant.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "master_list_config")
public class TenantMasterListConfig extends BaseEntity {

    @Field("masterListConfig")
    @NotNull(message = "Master list configuration is required")
    private Map<String, MasterListEntry> masterListConfig;

    // NEW: Event mappings for event-based master lists
    @Field("eventMappings")
    private EventMappingConfig eventMappings;

    @Field("description")
    private String description;

    @Field("version")
    private Integer version = 1;

    @Field("isActive")
    @Indexed
    private Boolean isActive = true;

    /**
     * Validates that the master list configuration is valid.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValidConfiguration() {
        if (masterListConfig == null || masterListConfig.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, MasterListEntry> entry : masterListConfig.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                log.warn("Invalid master list entry: null or empty key");
                return false;
            }

            if (entry.getValue() == null || !entry.getValue().isValid()) {
                log.warn("Invalid master list entry for key '{}': {}",
                        entry.getKey(), entry.getValue());
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the count of configured master list entries.
     *
     * @return number of entries
     */
    public int getEntryCount() {
        return masterListConfig != null ? masterListConfig.size() : 0;
    }

    /**
     * Checks if this configuration contains a specific master label.
     *
     * @param masterLabel the master label to check
     * @return true if the label exists, false otherwise
     */
    public boolean containsLabel(String masterLabel) {
        return masterListConfig != null && masterListConfig.containsKey(masterLabel);
    }

    /**
     * Gets a specific master list entry by label.
     *
     * @param masterLabel the master label
     * @return the master list entry, or null if not found
     */
    public MasterListEntry getEntry(String masterLabel) {
        return masterListConfig != null ? masterListConfig.get(masterLabel) : null;
    }

    // NEW: Event-specific validation methods
    /**
     * Checks if a label is valid for a specific event type.
     *
     * @param label the master label to check
     * @param eventType the event type
     * @return true if the label is valid for the event, false otherwise
     */
    public boolean isLabelValidForEvent(String label, EventType eventType) {
        if (eventMappings == null) {
            // If no event mappings configured, all labels are valid for all events
            return masterListConfig.containsKey(label);
        }
        return eventMappings.isLabelValidForEvent(label, eventType);
    }

    /**
     * Gets all labels valid for a specific event type.
     *
     * @param eventType the event type
     * @return set of labels valid for the event
     */
    public Set<String> getLabelsForEvent(EventType eventType) {
        if (eventMappings == null) {
            // If no event mappings, return all labels
            return masterListConfig.keySet();
        }
        return eventMappings.getLabelsForEvent(eventType);
    }

    /**
     * Gets master list entry for a specific event, checking for event-specific overrides.
     *
     * @param label the master label
     * @param eventType the event type
     * @return the master list entry (override or default), or null if not found
     */
    public MasterListEntry getEntryForEvent(String label, EventType eventType) {
        // Check for event-specific override first
        if (eventMappings != null) {
            MasterListEntry override = eventMappings.getEventSpecificOverride(eventType, label);
            if (override != null) {
                return override;
            }
        }

        // Return default entry
        return masterListConfig.get(label);
    }
}