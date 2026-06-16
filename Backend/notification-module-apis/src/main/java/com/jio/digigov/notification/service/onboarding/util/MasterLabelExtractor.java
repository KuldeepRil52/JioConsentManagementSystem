package com.jio.digigov.notification.service.onboarding.util;

import com.jio.digigov.notification.dto.onboarding.EventTemplateDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility for extracting master labels from template definitions.
 *
 * This utility analyzes template definitions and extracts all unique
 * master label identifiers (MASTER_LABEL_*) that are referenced in
 * SMS and Email templates.
 *
 * Used during onboarding to determine which master labels need to be
 * created automatically, regardless of the createMasterList flag.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
@Slf4j
public class MasterLabelExtractor {

    /**
     * Extracts all unique master labels from a list of template definitions.
     *
     * Scans SMS argumentsMap and Email argumentsSubjectMap/argumentsBodyMap
     * to find all MASTER_LABEL_* identifiers.
     *
     * @param templates List of event template definitions
     * @return Set of unique master label identifiers
     */
    public Set<String> extractMasterLabels(List<EventTemplateDefinition> templates) {
        log.debug("Extracting master labels from {} template definitions", templates.size());

        Set<String> masterLabels = new HashSet<>();

        for (EventTemplateDefinition template : templates) {
            // Extract from SMS template
            if (template.getSms() != null && template.getSms().getArgumentsMap() != null) {
                masterLabels.addAll(template.getSms().getArgumentsMap().values());
            }

            // Extract from Email template
            if (template.getEmail() != null) {
                if (template.getEmail().getArgumentsSubjectMap() != null) {
                    masterLabels.addAll(template.getEmail().getArgumentsSubjectMap().values());
                }

                if (template.getEmail().getArgumentsBodyMap() != null) {
                    masterLabels.addAll(template.getEmail().getArgumentsBodyMap().values());
                }
            }
        }

        log.info("Extracted {} unique master labels from {} templates",
                masterLabels.size(), templates.size());

        if (log.isDebugEnabled()) {
            masterLabels.forEach(label -> log.debug("  - {}", label));
        }

        return masterLabels;
    }

    /**
     * Extracts master labels for a specific event type.
     *
     * @param template Single event template definition
     * @return Set of master label identifiers for this template
     */
    public Set<String> extractMasterLabelsForEvent(EventTemplateDefinition template) {
        log.debug("Extracting master labels for event: {}", template.getEventType());

        Set<String> masterLabels = new HashSet<>();

        // Extract from SMS template
        if (template.getSms() != null && template.getSms().getArgumentsMap() != null) {
            masterLabels.addAll(template.getSms().getArgumentsMap().values());
        }

        // Extract from Email template
        if (template.getEmail() != null) {
            if (template.getEmail().getArgumentsSubjectMap() != null) {
                masterLabels.addAll(template.getEmail().getArgumentsSubjectMap().values());
            }

            if (template.getEmail().getArgumentsBodyMap() != null) {
                masterLabels.addAll(template.getEmail().getArgumentsBodyMap().values());
            }
        }

        log.debug("Extracted {} master labels for event {}: {}",
                masterLabels.size(), template.getEventType(), masterLabels);

        return masterLabels;
    }

    /**
     * Validates that all master labels start with the expected prefix.
     *
     * @param masterLabels Set of master label identifiers
     * @return true if all labels are valid, false otherwise
     */
    public boolean validateMasterLabels(Set<String> masterLabels) {
        final String EXPECTED_PREFIX = "MASTER_LABEL_";

        for (String label : masterLabels) {
            if (!label.startsWith(EXPECTED_PREFIX)) {
                log.warn("Invalid master label found (missing prefix): {}", label);
                return false;
            }
        }

        return true;
    }

    /**
     * Counts the total number of label references across all templates.
     *
     * This includes duplicates - useful for understanding template complexity.
     *
     * @param templates List of event template definitions
     * @return Total count of label references (including duplicates)
     */
    public int countLabelReferences(List<EventTemplateDefinition> templates) {
        int count = 0;

        for (EventTemplateDefinition template : templates) {
            // Count SMS arguments
            if (template.getSms() != null && template.getSms().getArgumentsMap() != null) {
                count += template.getSms().getArgumentsMap().size();
            }

            // Count Email subject arguments
            if (template.getEmail() != null && template.getEmail().getArgumentsSubjectMap() != null) {
                count += template.getEmail().getArgumentsSubjectMap().size();
            }

            // Count Email body arguments
            if (template.getEmail() != null && template.getEmail().getArgumentsBodyMap() != null) {
                count += template.getEmail().getArgumentsBodyMap().size();
            }
        }

        return count;
    }
}
