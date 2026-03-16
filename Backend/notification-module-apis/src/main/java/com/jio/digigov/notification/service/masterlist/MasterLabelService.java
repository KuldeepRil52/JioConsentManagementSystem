package com.jio.digigov.notification.service.masterlist;

import com.jio.digigov.notification.dto.masterlist.EventMappingConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.response.masterlist.ValidationResponseDto;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.enums.EventType;
import com.jio.digigov.notification.service.masterlist.resolver.DatabaseResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterLabelService {

    private final TenantMasterListConfigService configService;
    private final DatabaseResolver databaseResolver;

    /**
     * Adds a new master label to the configuration.
     *
     * @param tenantId the tenant identifier
     * @param labelName the label name
     * @param entry the master list entry configuration
     * @param events the events to associate with this label (optional)
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void addMasterLabel(String tenantId, String labelName, MasterListEntry entry, Set<EventType> events) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        // Check if label already exists
        if (config.containsLabel(labelName)) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Label already exists: " + labelName);
        }

        // Validate the entry
        if (!entry.isValid()) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Invalid master list entry");
        }

        // Add label to master list
        config.getMasterListConfig().put(labelName, entry);

        // Add to event mappings
        if (events != null && !events.isEmpty()) {
            if (config.getEventMappings() == null) {
                config.setEventMappings(new EventMappingConfig());
            }

            for (EventType eventType : events) {
                config.getEventMappings().addLabelToEvent(eventType, labelName);
            }
        }

        saveTenantConfig(tenantId, config);
        log.info("Added master label '{}' for tenant {} with {} associated events",
                labelName, tenantId, events != null ? events.size() : 0);
    }

    /**
     * Updates an existing master label.
     *
     * @param tenantId the tenant identifier
     * @param labelName the label name
     * @param entry the updated master list entry configuration
     * @param events the events to associate with this label (null to keep existing)
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void updateMasterLabel(String tenantId, String labelName, MasterListEntry entry, Set<EventType> events) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        if (!config.containsLabel(labelName)) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Label not found: " + labelName);
        }

        // Validate the entry
        if (!entry.isValid()) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Invalid master list entry");
        }

        // Update entry
        config.getMasterListConfig().put(labelName, entry);

        // Update event mappings if provided
        if (events != null) {
            if (config.getEventMappings() == null) {
                config.setEventMappings(new EventMappingConfig());
            }

            // Remove from all events first
            config.getEventMappings().removeLabelFromAllEvents(labelName);

            // Add to specified events
            for (EventType eventType : events) {
                config.getEventMappings().addLabelToEvent(eventType, labelName);
            }
        }

        saveTenantConfig(tenantId, config);
        log.info("Updated master label '{}' for tenant {}", labelName, tenantId);
    }

    /**
     * Deletes a master label from the configuration.
     *
     * @param tenantId the tenant identifier
     * @param labelName the label name to delete
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void deleteMasterLabel(String tenantId, String labelName) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        if (!config.containsLabel(labelName)) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Label not found: " + labelName);
        }

        // Remove from master list
        config.getMasterListConfig().remove(labelName);

        // Remove from all event mappings
        if (config.getEventMappings() != null) {
            config.getEventMappings().removeLabelFromAllEvents(labelName);
        }

        saveTenantConfig(tenantId, config);
        log.info("Deleted master label '{}' for tenant {}", labelName, tenantId);
    }

    /**
     * Gets details about a specific master label.
     *
     * @param tenantId the tenant identifier
     * @param labelName the label name
     * @return master label details
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if label not found
     */
    public MasterLabelDetails getLabelDetails(String tenantId, String labelName, boolean includeEventMappings) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);
        MasterListEntry entry = config.getEntry(labelName);

        if (entry == null) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Label not found: " + labelName);
        }

        Set<EventType> associatedEvents = new HashSet<>();
        if (config.getEventMappings() != null) {
            associatedEvents = config.getEventMappings().getLabelToEvents()
                .getOrDefault(labelName, new HashSet<>());
        }

        return MasterLabelDetails.builder()
            .labelName(labelName)
            .entry(entry)
            .associatedEvents(associatedEvents)
            .description(entry.getDescription())
            .build();
    }

    /**
     * Tests resolution of a master label with sample data.
     *
     * @param tenantId the tenant identifier
     * @param labelName the label name
     * @param eventPayload sample event payload for resolution
     * @param eventType the event type (for getting event-specific overrides)
     * @return resolution test result
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public ResolutionTestResult testLabelResolution(String tenantId, String labelName,
                                                  Map<String, Object> eventPayload, EventType eventType) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);
        MasterListEntry entry = config.getEntryForEvent(labelName, eventType);

        if (entry == null) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException(
                "Label not found or not valid for event: " + labelName);
        }

        long startTime = System.currentTimeMillis();
        String resolvedValue;
        String status;
        Map<String, Object> queryInfo = new HashMap<>();

        try {
            if (entry.getDataSource() == com.jio.digigov.notification.enums.MasterListDataSource.DB) {
                resolvedValue = databaseResolver.resolveDynamicQuery(entry, eventPayload, tenantId);

                // Add query information
                queryInfo.put("collection", entry.getCollection());
                queryInfo.put("path", entry.getPath());
                if (entry.hasDynamicQuery()) {
                    queryInfo.put("dynamicQuery", entry.getQuery());
                    queryInfo.put("resolvedQuery", buildResolvedQuery(entry, eventPayload));
                }
            } else {
                // Handle other data sources
                resolvedValue = "Test resolution not supported for " + entry.getDataSource();
            }
            status = "SUCCESS";

        } catch (Exception e) {
            resolvedValue = null;
            status = "ERROR: " + e.getMessage();
            log.warn("Test resolution failed for label '{}': {}", labelName, e.getMessage());
        }

        long resolutionTime = System.currentTimeMillis() - startTime;

        return ResolutionTestResult.builder()
            .labelName(labelName)
            .resolvedValue(resolvedValue)
            .resolutionTime(resolutionTime)
            .status(status)
            .query(queryInfo)
            .build();
    }

    /**
     * Gets all master labels for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return map of label names to their configurations
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if no active configuration found
     */
    public Map<String, MasterListEntry> getAllMasterLabels(String tenantId) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);
        return new HashMap<>(config.getMasterListConfig());
    }

    /**
     * Validates labels for a specific event type.
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @param masterLabels the set of labels to validate
     * @return validation response
     */
    public ValidationResponseDto validateLabelsForEvent(String tenantId, EventType eventType, Set<String> masterLabels) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        // Implementation can be added based on business requirements
        // For now, returning a basic validation response
        return ValidationResponseDto.builder()
                .isValid(true)
                .validLabels(masterLabels)
                .build();
    }

    private TenantMasterListConfig getActiveConfigOrThrow(String tenantId) {
        return configService.getTenantConfig(tenantId)
            .orElseThrow(() -> new TenantMasterListConfigService.TenantMasterListConfigException("No configuration found"));
    }

    private void saveTenantConfig(String tenantId, TenantMasterListConfig config) {
        try {
            configService.updateTenantConfig(tenantId, config);
        } catch (Exception e) {
            log.error("Failed to save tenant config for tenantId {}: {}", tenantId, e.getMessage());
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Failed to save configuration", e);
        }
    }

    private Map<String, Object> buildResolvedQuery(MasterListEntry entry, Map<String, Object> eventPayload) {
        Map<String, Object> resolvedQuery = new HashMap<>();

        if (entry.getQuery() != null) {
            for (Map.Entry<String, String> param : entry.getQuery().entrySet()) {
                String resolvedValue = resolveFromPayload(param.getValue(), eventPayload);
                resolvedQuery.put(param.getKey(), resolvedValue);
            }
        }

        return resolvedQuery;
    }

    private String resolveFromPayload(String template, Map<String, Object> payload) {
        // Simple template resolution - can be enhanced
        if (template.startsWith("{{") && template.endsWith("}}")) {
            String path = template.substring(2, template.length() - 2);
            return extractValueFromPayload(payload, path);
        }
        return template;
    }

    private String extractValueFromPayload(Map<String, Object> payload, String path) {
        String[] parts = path.split("\\.");
        Object current = payload;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current != null ? current.toString() : null;
    }

    /**
     * Master label details response.
     */
    public static class MasterLabelDetails {
        private final String labelName;
        private final MasterListEntry entry;
        private final Set<EventType> associatedEvents;
        private final String description;

        private MasterLabelDetails(String labelName, MasterListEntry entry,
                                 Set<EventType> associatedEvents, String description) {
            this.labelName = labelName;
            this.entry = entry;
            this.associatedEvents = associatedEvents;
            this.description = description;
        }

        public static MasterLabelDetailsBuilder builder() {
            return new MasterLabelDetailsBuilder();
        }

        public String getLabelName() { return labelName; }
        public MasterListEntry getEntry() { return entry; }
        public Set<EventType> getAssociatedEvents() { return associatedEvents; }
        public String getDescription() { return description; }

        public static class MasterLabelDetailsBuilder {
            private String labelName;
            private MasterListEntry entry;
            private Set<EventType> associatedEvents;
            private String description;

            public MasterLabelDetailsBuilder labelName(String labelName) {
                this.labelName = labelName;
                return this;
            }

            public MasterLabelDetailsBuilder entry(MasterListEntry entry) {
                this.entry = entry;
                return this;
            }

            public MasterLabelDetailsBuilder associatedEvents(Set<EventType> associatedEvents) {
                this.associatedEvents = associatedEvents;
                return this;
            }

            public MasterLabelDetailsBuilder description(String description) {
                this.description = description;
                return this;
            }

            public MasterLabelDetails build() {
                return new MasterLabelDetails(labelName, entry, associatedEvents, description);
            }
        }
    }

    /**
     * Resolution test result.
     */
    public static class ResolutionTestResult {
        private final String labelName;
        private final String resolvedValue;
        private final long resolutionTime;
        private final String status;
        private final Map<String, Object> query;

        private ResolutionTestResult(String labelName, String resolvedValue, long resolutionTime,
                                   String status, Map<String, Object> query) {
            this.labelName = labelName;
            this.resolvedValue = resolvedValue;
            this.resolutionTime = resolutionTime;
            this.status = status;
            this.query = query;
        }

        public static ResolutionTestResultBuilder builder() {
            return new ResolutionTestResultBuilder();
        }

        public String getLabelName() { return labelName; }
        public String getResolvedValue() { return resolvedValue; }
        public long getResolutionTime() { return resolutionTime; }
        public String getStatus() { return status; }
        public Map<String, Object> getQuery() { return query; }

        public static class ResolutionTestResultBuilder {
            private String labelName;
            private String resolvedValue;
            private long resolutionTime;
            private String status;
            private Map<String, Object> query;

            public ResolutionTestResultBuilder labelName(String labelName) {
                this.labelName = labelName;
                return this;
            }

            public ResolutionTestResultBuilder resolvedValue(String resolvedValue) {
                this.resolvedValue = resolvedValue;
                return this;
            }

            public ResolutionTestResultBuilder resolutionTime(long resolutionTime) {
                this.resolutionTime = resolutionTime;
                return this;
            }

            public ResolutionTestResultBuilder status(String status) {
                this.status = status;
                return this;
            }

            public ResolutionTestResultBuilder query(Map<String, Object> query) {
                this.query = query;
                return this;
            }

            public ResolutionTestResult build() {
                return new ResolutionTestResult(labelName, resolvedValue, resolutionTime, status, query);
            }
        }
    }
}