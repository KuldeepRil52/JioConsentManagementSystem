package com.jio.consent.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class for serializing objects to JSON strings for signature operations.
 * DTOs should use @JsonPropertyOrder(alphabetic = true) annotation to ensure
 * consistent JSON representation regardless of object field order.
 */
@Slf4j
@Component
public class SignatureJsonSerializer {

    private final ObjectMapper objectMapper;

    public SignatureJsonSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Serializes an object to a JSON string.
     * The JSON will be sorted alphabetically if the DTO uses @JsonPropertyOrder(alphabetic = true).
     *
     * @param object the object to serialize
     * @return JSON string representation
     */
    public String serializeToSortedJson(Object object) {
        try {
            if (object == null) {
                return "null";
            }

            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize object to JSON: " + e.getMessage(), e);
        }
    }
}

