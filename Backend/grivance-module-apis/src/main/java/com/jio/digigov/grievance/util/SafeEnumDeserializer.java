package com.jio.digigov.grievance.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Generic, safe deserializer for all enums.
 * - Case-insensitive
 * - Throws descriptive error messages
 * - Works with any Enum type
 */
public class SafeEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {

    private final Class<T> enumType;

    public SafeEnumDeserializer(Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText();

        if (value == null || value.isBlank()) {
            return null; // handle gracefully
        }

        try {
            // Case-insensitive match
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            String allowedValues = Arrays.stream(enumType.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(String.format(
                    "Invalid value '%s' for enum %s. Allowed values: [%s]",
                    value, enumType.getSimpleName(), allowedValues
            ));
        }
    }
}