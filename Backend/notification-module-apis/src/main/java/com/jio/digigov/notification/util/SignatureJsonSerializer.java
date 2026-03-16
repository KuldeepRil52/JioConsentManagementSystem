package com.jio.digigov.notification.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jio.digigov.notification.exception.SignatureGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for serializing DTOs to alphabetically sorted JSON for signature generation.
 *
 * <p>This serializer ensures consistent JSON representation regardless of field declaration order
 * in classes. It's critical for signature verification where payload must be byte-for-byte
 * identical between sender and receiver.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Alphabetical property ordering (case-sensitive)</li>
 *   <li>ISO-8601 date/time format for LocalDateTime fields</li>
 *   <li>Null value exclusion for cleaner payloads</li>
 *   <li>Compact JSON (no pretty printing) for consistency</li>
 *   <li>Recursive sorting for nested objects and maps</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * MissedNotificationResponseDto response = ...;
 * String sortedJson = signatureJsonSerializer.serializeToSortedJson(response);
 * // Result: {"businessId":"b123","notificationId":"n456","status":"RETRIEVED",...}
 * </pre>
 *
 * @since 2.0.0
 */
@Slf4j
@Component
public class SignatureJsonSerializer {

    private final ObjectMapper sortedObjectMapper;

    /**
     * Constructor that initializes a specially configured ObjectMapper for signature purposes.
     */
    public SignatureJsonSerializer() {
        this.sortedObjectMapper = createSortedObjectMapper();
    }

    /**
     * Creates and configures an ObjectMapper with alphabetical property ordering.
     *
     * @return configured ObjectMapper instance
     */
    private ObjectMapper createSortedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Enable alphabetical property ordering (CRITICAL for signature consistency)
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        // Date/Time handling - use ISO-8601 format
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Exclude null values to match typical API behavior
        // mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);  // Can be enabled if needed

        // Disable pretty printing for compact, consistent output
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        // Handle empty beans gracefully
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        log.debug("SignatureJsonSerializer initialized with alphabetical property ordering");
        return mapper;
    }

    /**
     * Serializes any object to alphabetically sorted JSON string.
     *
     * <p>This method converts DTOs, Maps, or any Java object to a JSON string with
     * all properties sorted alphabetically. This is essential for signature generation
     * as it ensures consistent JSON representation.</p>
     *
     * @param object the object to serialize (DTO, Map, List, etc.)
     * @return alphabetically sorted JSON string
     * @throws SignatureGenerationException if serialization fails
     */
    public String serializeToSortedJson(Object object) {
        if (object == null) {
            log.warn("Attempted to serialize null object for signature");
            return "{}";
        }

        try {
            // First, convert to JSON string
            String json = sortedObjectMapper.writeValueAsString(object);

            log.debug("Serialized {} to sorted JSON (length: {})",
                    object.getClass().getSimpleName(), json.length());

            return json;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object of type {} to JSON: {}",
                    object.getClass().getName(), e.getMessage(), e);

            throw SignatureGenerationException.jsonSerializationFailed(
                    object.getClass().getSimpleName(), e);
        }
    }

    /**
     * Serializes a Map to alphabetically sorted JSON string.
     *
     * <p>This method is optimized for Map objects and ensures keys are sorted.
     * It first converts the input map to a TreeMap for alphabetical ordering,
     * then serializes it to JSON.</p>
     *
     * @param map the map to serialize
     * @return alphabetically sorted JSON string
     * @throws SignatureGenerationException if serialization fails
     */
    public String serializeMapToSortedJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            log.warn("Attempted to serialize null or empty map for signature");
            return "{}";
        }

        try {
            // Convert to TreeMap for guaranteed alphabetical ordering
            Map<String, Object> sortedMap = new TreeMap<>(map);

            // Recursively sort nested maps
            sortNestedMaps(sortedMap);

            String json = sortedObjectMapper.writeValueAsString(sortedMap);

            log.debug("Serialized map with {} keys to sorted JSON (length: {})",
                    map.size(), json.length());

            return json;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize map to JSON: {}", e.getMessage());

            throw SignatureGenerationException.jsonSerializationFailed("Map", e);
        }
    }

    /**
     * Recursively sorts nested maps within a map.
     *
     * <p>This ensures that nested Map objects are also alphabetically sorted,
     * providing complete consistency for signature generation.</p>
     *
     * @param map the map to process
     */
    @SuppressWarnings("unchecked")
    private void sortNestedMaps(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Map) {
                // Convert nested map to TreeMap
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                Map<String, Object> sortedNestedMap = new TreeMap<>(nestedMap);

                // Recursively sort deeper levels
                sortNestedMaps(sortedNestedMap);

                // Replace with sorted version
                entry.setValue(sortedNestedMap);
            }
        }
    }

    /**
     * Deserializes JSON string to a Map for verification purposes.
     *
     * <p>This is useful when receiving signed payloads that need to be
     * converted back to Map objects for processing.</p>
     *
     * @param json the JSON string to deserialize
     * @return Map representation of the JSON
     * @throws SignatureGenerationException if deserialization fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserializeJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            log.warn("Attempted to deserialize null or empty JSON string");
            return Map.of();
        }

        try {
            Map<String, Object> map = sortedObjectMapper.readValue(json, Map.class);

            log.debug("Deserialized JSON to map with {} keys", map.size());

            return new TreeMap<>(map);  // Return as TreeMap for consistent ordering

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to map: {}", e.getMessage());

            throw SignatureGenerationException.jsonSerializationFailed("JSON string", e);
        }
    }

    /**
     * Validates if a string is valid JSON.
     *
     * @param json the string to validate
     * @return true if valid JSON, false otherwise
     */
    public boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        try {
            sortedObjectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            log.debug("Invalid JSON detected: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Pretty prints a JSON string for debugging purposes.
     *
     * <p><b>WARNING:</b> Do NOT use pretty-printed JSON for signature generation!
     * This method is for logging and debugging only.</p>
     *
     * @param json the JSON string to pretty print
     * @return formatted JSON string
     */
    public String prettyPrintJson(String json) {
        try {
            Object jsonObject = sortedObjectMapper.readValue(json, Object.class);
            return sortedObjectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            log.warn("Failed to pretty print JSON: {}", e.getMessage());
            return json;  // Return original if pretty printing fails
        }
    }

    /**
     * Gets the configured ObjectMapper for advanced use cases.
     *
     * <p>Use this only if you need direct access to the mapper for custom serialization logic.</p>
     *
     * @return the configured ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return sortedObjectMapper;
    }
}
