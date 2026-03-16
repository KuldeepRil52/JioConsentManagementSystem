package com.jio.digigov.notification.service.masterlist.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Resolver for extracting values from TriggerEventRequestDto payload.
 *
 * This resolver supports extracting values from any field in the TriggerEventRequestDto
 * using dot notation for nested object navigation. It can access:
 * - Direct fields: "eventType", "resource", "source", "language"
 * - Customer identifiers: "customerIdentifiers.type", "customerIdentifiers.value"
 * - Event payload: "eventPayload.customerName", "eventPayload.amount"
 * - Request headers: "header.X-Tenant-ID", "header.X-Business-ID", "header.X-Transaction-ID"
 * - Data processor IDs: "dataProcessorIds[0]" (for array access)
 *
 * Examples:
 * - path: "eventType" -> returns request.getEventType()
 * - path: "customerIdentifiers.value" -> returns request.getCustomerIdentifiers().getValue()
 * - path: "eventPayload.customerName" -> returns request.getEventPayload().get("customerName")
 * - path: "header.X-Tenant-ID" -> returns the X-Tenant-ID header value
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayloadResolver {

    private final ObjectMapper objectMapper;

    /**
     * Extracts a value from the TriggerEventRequestDto using the specified path.
     *
     * @param request the trigger event request
     * @param path the dot-notation path to the value (e.g., "eventPayload.customerName")
     * @return the extracted value as a string, or null if not found
     * @throws PayloadResolutionException if extraction fails
     */
    public String extractValue(TriggerEventRequestDto request, String path) {
        return extractValue(request, path, null);
    }

    /**
     * Extracts a value from the TriggerEventRequestDto using the specified path, with support for headers.
     *
     * @param request the trigger event request
     * @param path the dot-notation path to the value (e.g., "eventPayload.customerName", "header.X-Tenant-ID")
     * @param headers the request headers map (can be null if not needed)
     * @return the extracted value as a string, or null if not found
     * @throws PayloadResolutionException if extraction fails
     */
    public String extractValue(TriggerEventRequestDto request, String path, Map<String, String> headers) {
        if (request == null) {
            throw new PayloadResolutionException("TriggerEventRequestDto is null");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new PayloadResolutionException("Path is null or empty");
        }

        try {
            log.debug("Extracting value from payload path: {}", path);

            // Handle header extraction
            if (path.startsWith("header.")) {
                return extractFromHeaders(path, headers);
            }

            Object value = navigatePath(request, path);

            if (value == null) {
                log.debug("Value not found at path: {}", path);
                return null;
            }

            String stringValue = convertToString(value);
            log.debug("Successfully extracted value from path '{}': '{}'", path, stringValue);
            return stringValue;

        } catch (Exception e) {
            log.error("Failed to extract value from path '{}': {}", path, e.getMessage());
            throw new PayloadResolutionException("Failed to extract value from path: " + path, e);
        }
    }

    /**
     * Extracts a value from request headers.
     *
     * @param path the header path (e.g., "header.X-Tenant-ID")
     * @param headers the request headers map
     * @return the header value, or null if not found
     */
    private String extractFromHeaders(String path, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            log.debug("Headers map is null or empty, cannot extract header: {}", path);
            return null;
        }

        // Remove "header." prefix to get the actual header name
        String headerName = path.substring(7); // "header.".length() = 7

        // Try exact match first
        String value = headers.get(headerName);
        if (value != null) {
            log.debug("Found header '{}' with value: '{}'", headerName, value);
            return value;
        }

        // Try case-insensitive lookup for headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                log.debug("Found header '{}' (case-insensitive) with value: '{}'", headerName, entry.getValue());
                return entry.getValue();
            }
        }

        log.debug("Header '{}' not found in request headers", headerName);
        return null;
    }

    /**
     * Navigates through the object graph using the dot-notation path.
     *
     * @param root the root object to start navigation from
     * @param path the dot-notation path
     * @return the value at the specified path, or null if not found
     */
    private Object navigatePath(Object root, String path) {
        String[] pathSegments = path.split("\\.");
        Object current = root;

        for (String segment : pathSegments) {
            if (current == null) {
                return null;
            }

            current = navigateSegment(current, segment);
        }

        return current;
    }

    /**
     * Navigates a single path segment (field access or map key lookup).
     *
     * @param obj the current object
     * @param segment the path segment
     * @return the value for the segment, or null if not found
     */
    private Object navigateSegment(Object obj, String segment) {
        if (obj == null) {
            return null;
        }

        try {
            // Handle Map objects (like eventPayload)
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                return map.get(segment);
            }

            // Handle regular object field access using reflection
            return getFieldValue(obj, segment);

        } catch (Exception e) {
            log.debug("Failed to navigate segment '{}' on object of type {}: {}",
                     segment, obj.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets the value of a field from an object using reflection and getter methods.
     *
     * @param obj the object
     * @param fieldName the field name
     * @return the field value, or null if not accessible
     */
    private Object getFieldValue(Object obj, String fieldName) {
        Class<?> clazz = obj.getClass();

        // Try standard getter method patterns
        String[] getterPrefixes = {"get", "is"};
        String capitalizedFieldName = capitalize(fieldName);

        for (String prefix : getterPrefixes) {
            String methodName = prefix + capitalizedFieldName;

            try {
                Method method = clazz.getMethod(methodName);
                return method.invoke(obj);
            } catch (Exception e) {
                // Continue to next getter pattern
            }
        }

        log.debug("No getter method found for field '{}' in class {}", fieldName, clazz.getSimpleName());
        return null;
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the input string
     * @return the capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Converts an object value to its string representation.
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
                log.debug("Failed to convert complex object to JSON, using toString(): {}", e.getMessage());
                return value.toString();
            }
        }

        return value.toString();
    }

    /**
     * Checks if an object is a complex type that should be JSON-serialized.
     *
     * @param value the value to check
     * @return true if the object is complex, false otherwise
     */
    private boolean isComplexObject(Object value) {
        if (value == null) {
            return false;
        }

        Class<?> clazz = value.getClass();

        // Simple types don't need JSON serialization
        return !clazz.isPrimitive()
            && !Number.class.isAssignableFrom(clazz)
            && !Boolean.class.isAssignableFrom(clazz)
            && !Character.class.isAssignableFrom(clazz)
            && !(value instanceof String)
            && !clazz.isEnum();
    }

    /**
     * Validates that a path can be extracted from a request structure.
     *
     * @param path the path to validate
     * @return true if the path appears valid, false otherwise
     */
    public boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        // Special validation for header paths
        if (path.startsWith("header.")) {
            String headerName = path.substring(7);
            return !headerName.isEmpty() && headerName.matches("^[a-zA-Z][a-zA-Z0-9-_]*$");
        }

        // Basic validation: no consecutive dots, no leading/trailing dots
        return !path.contains("..")
            && !path.startsWith(".")
            && !path.endsWith(".")
            && path.matches("^[a-zA-Z][a-zA-Z0-9_.]*$");
    }

    /**
     * Custom exception for payload resolution errors.
     */
    public static class PayloadResolutionException extends RuntimeException {
        public PayloadResolutionException(String message) {
            super(message);
        }

        public PayloadResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}