package com.jio.digigov.notification.dto.masterlist;

import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMappingConfig {

    // Primary lookup: event -> allowed labels
    @Builder.Default
    private Map<EventType, Set<String>> eventToLabels = new HashMap<>();

    // Reverse lookup: label -> applicable events
    @Builder.Default
    private Map<String, Set<EventType>> labelToEvents = new HashMap<>();

    // Event-specific overrides (optional)
    @Builder.Default
    private Map<EventType, Map<String, MasterListEntry>> eventOverrides = new HashMap<>();

    // Utility methods for efficient operations
    public Set<String> getLabelsForEvent(EventType eventType) {
        return eventToLabels.getOrDefault(eventType, Collections.emptySet());
    }

    public boolean isLabelValidForEvent(String label, EventType eventType) {
        Set<EventType> events = labelToEvents.get(label);
        return events != null && events.contains(eventType);
    }

    public void addEventMapping(EventType eventType, Set<String> labels) {
        // Add to event -> labels mapping
        eventToLabels.put(eventType, new HashSet<>(labels));

        // Update reverse mapping
        for (String label : labels) {
            labelToEvents.computeIfAbsent(label, k -> new HashSet<>()).add(eventType);
        }
    }

    public void removeEventMapping(EventType eventType) {
        Set<String> labels = eventToLabels.remove(eventType);
        if (labels != null) {
            // Remove from reverse mapping
            for (String label : labels) {
                Set<EventType> events = labelToEvents.get(label);
                if (events != null) {
                    events.remove(eventType);
                    if (events.isEmpty()) {
                        labelToEvents.remove(label);
                    }
                }
            }
        }
        // Remove any event-specific overrides
        eventOverrides.remove(eventType);
    }

    public void addLabelToEvent(EventType eventType, String label) {
        eventToLabels.computeIfAbsent(eventType, k -> new HashSet<>()).add(label);
        labelToEvents.computeIfAbsent(label, k -> new HashSet<>()).add(eventType);
    }

    public void removeLabelFromEvent(EventType eventType, String label) {
        Set<String> labels = eventToLabels.get(eventType);
        if (labels != null) {
            labels.remove(label);
            if (labels.isEmpty()) {
                eventToLabels.remove(eventType);
            }
        }

        Set<EventType> events = labelToEvents.get(label);
        if (events != null) {
            events.remove(eventType);
            if (events.isEmpty()) {
                labelToEvents.remove(label);
            }
        }
    }

    public void removeLabelFromAllEvents(String label) {
        Set<EventType> events = labelToEvents.remove(label);
        if (events != null) {
            for (EventType eventType : events) {
                Set<String> labels = eventToLabels.get(eventType);
                if (labels != null) {
                    labels.remove(label);
                    if (labels.isEmpty()) {
                        eventToLabels.remove(eventType);
                    }
                }
            }
        }
    }

    public Set<EventType> getAllConfiguredEvents() {
        return new HashSet<>(eventToLabels.keySet());
    }

    public Set<String> getAllConfiguredLabels() {
        return new HashSet<>(labelToEvents.keySet());
    }

    // Get event-specific override for a label
    public MasterListEntry getEventSpecificOverride(EventType eventType, String label) {
        Map<String, MasterListEntry> overrides = eventOverrides.get(eventType);
        return overrides != null ? overrides.get(label) : null;
    }

    // Add event-specific override
    public void addEventSpecificOverride(EventType eventType, String label, MasterListEntry override) {
        eventOverrides.computeIfAbsent(eventType, k -> new HashMap<>()).put(label, override);
    }
}