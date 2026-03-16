package com.jio.digigov.notification.service.masterlist;

import com.jio.digigov.notification.dto.masterlist.EventMappingConfig;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventMappingService {

    private final TenantMasterListConfigService configService;

    /**
     * Gets all labels valid for a specific event type.
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @return set of labels valid for the event
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if no active configuration found
     */
    public Set<String> getLabelsForEvent(String tenantId, EventType eventType) {
        Optional<TenantMasterListConfig> config = configService.getTenantConfig(tenantId);
        if (config.isEmpty()) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("No configuration found");
        }

        return config.get().getLabelsForEvent(eventType);
    }

    /**
     * Adds event mapping for a specific event type.
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @param labels the labels to associate with the event
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void addEventMapping(String tenantId, EventType eventType, Set<String> labels) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        // Validate all labels exist in master list
        for (String label : labels) {
            if (!config.containsLabel(label)) {
                throw new TenantMasterListConfigService.TenantMasterListConfigException("Invalid label: " + label);
            }
        }

        // Add mapping
        if (config.getEventMappings() == null) {
            config.setEventMappings(new EventMappingConfig());
        }

        config.getEventMappings().addEventMapping(eventType, labels);
        saveTenantConfig(tenantId, config);
    }

    /**
     * Removes all mappings for a specific event type.
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void removeEventMapping(String tenantId, EventType eventType) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        if (config.getEventMappings() != null) {
            config.getEventMappings().removeEventMapping(eventType);
            saveTenantConfig(tenantId, config);
        }
    }

    /**
     * Updates event mapping for a specific event type (replaces existing mapping).
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @param labels the new labels to associate with the event
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void updateEventMapping(String tenantId, EventType eventType, Set<String> labels) {
        // Remove existing mapping and add new one
        removeEventMapping(tenantId, eventType);
        addEventMapping(tenantId, eventType, labels);
    }

    /**
     * Validates if a set of labels are valid for a specific event type.
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @param labels the labels to validate
     * @return validation result with valid/invalid labels and suggestions
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if no active configuration found
     */
    public ValidationResult validateLabelsForEvent(String tenantId, EventType eventType, Set<String> labels) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);
        Set<String> allowedLabels = config.getLabelsForEvent(eventType);

        Set<String> validLabels = new HashSet<>();
        Set<String> invalidLabels = new HashSet<>();

        for (String label : labels) {
            if (allowedLabels.contains(label)) {
                validLabels.add(label);
            } else {
                invalidLabels.add(label);
            }
        }

        return ValidationResult.builder()
            .isValid(invalidLabels.isEmpty())
            .validLabels(validLabels)
            .invalidLabels(invalidLabels)
            .suggestions(generateSuggestions(config, invalidLabels))
            .build();
    }

    /**
     * Gets all configured event types for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return set of configured event types
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if no active configuration found
     */
    public Set<EventType> getAllConfiguredEvents(String tenantId) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        if (config.getEventMappings() == null) {
            return Collections.emptySet();
        }

        return config.getEventMappings().getAllConfiguredEvents();
    }

    /**
     * Gets all configured labels for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return set of configured labels
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if no active configuration found
     */
    public Set<String> getAllConfiguredLabels(String tenantId) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        if (config.getEventMappings() == null) {
            return config.getMasterListConfig().keySet();
        }

        return config.getEventMappings().getAllConfiguredLabels();
    }

    /**
     * Adds a label to a specific event.
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @param label the label to add
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void addLabelToEvent(String tenantId, EventType eventType, String label) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        // Validate label exists in master list
        if (!config.containsLabel(label)) {
            throw new TenantMasterListConfigService.TenantMasterListConfigException("Invalid label: " + label);
        }

        // Add label to event
        if (config.getEventMappings() == null) {
            config.setEventMappings(new EventMappingConfig());
        }

        config.getEventMappings().addLabelToEvent(eventType, label);
        saveTenantConfig(tenantId, config);
    }

    /**
     * Removes a label from a specific event.
     *
     * @param tenantId the tenant identifier
     * @param eventType the event type
     * @param label the label to remove
     * @throws TenantMasterListConfigService.TenantMasterListConfigException if operation fails
     */
    public void removeLabelFromEvent(String tenantId, EventType eventType, String label) {
        TenantMasterListConfig config = getActiveConfigOrThrow(tenantId);

        if (config.getEventMappings() != null) {
            config.getEventMappings().removeLabelFromEvent(eventType, label);
            saveTenantConfig(tenantId, config);
        }
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

    private Map<String, List<String>> generateSuggestions(TenantMasterListConfig config, Set<String> invalidLabels) {
        // Simple suggestion logic - can be enhanced with fuzzy matching
        Map<String, List<String>> suggestions = new HashMap<>();
        Set<String> availableLabels = config.getMasterListConfig().keySet();

        for (String invalidLabel : invalidLabels) {
            List<String> similar = availableLabels.stream()
                .filter(label -> calculateSimilarity(invalidLabel, label) > 0.6)
                .limit(3)
                .collect(Collectors.toList());

            if (!similar.isEmpty()) {
                suggestions.put(invalidLabel, similar);
            }
        }

        return suggestions;
    }

    private double calculateSimilarity(String s1, String s2) {
        // Simple Levenshtein distance-based similarity
        // Can be replaced with more sophisticated algorithms
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Validation result for label validation operations.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final Set<String> validLabels;
        private final Set<String> invalidLabels;
        private final Map<String, List<String>> suggestions;

        private ValidationResult(boolean isValid, Set<String> validLabels, Set<String> invalidLabels,
                               Map<String, List<String>> suggestions) {
            this.isValid = isValid;
            this.validLabels = validLabels;
            this.invalidLabels = invalidLabels;
            this.suggestions = suggestions;
        }

        public static ValidationResultBuilder builder() {
            return new ValidationResultBuilder();
        }

        public boolean isValid() { return isValid; }
        public Set<String> getValidLabels() { return validLabels; }
        public Set<String> getInvalidLabels() { return invalidLabels; }
        public Map<String, List<String>> getSuggestions() { return suggestions; }

        public static class ValidationResultBuilder {
            private boolean isValid;
            private Set<String> validLabels;
            private Set<String> invalidLabels;
            private Map<String, List<String>> suggestions;

            public ValidationResultBuilder isValid(boolean isValid) {
                this.isValid = isValid;
                return this;
            }

            public ValidationResultBuilder validLabels(Set<String> validLabels) {
                this.validLabels = validLabels;
                return this;
            }

            public ValidationResultBuilder invalidLabels(Set<String> invalidLabels) {
                this.invalidLabels = invalidLabels;
                return this;
            }

            public ValidationResultBuilder suggestions(Map<String, List<String>> suggestions) {
                this.suggestions = suggestions;
                return this;
            }

            public ValidationResult build() {
                return new ValidationResult(isValid, validLabels, invalidLabels, suggestions);
            }
        }
    }
}