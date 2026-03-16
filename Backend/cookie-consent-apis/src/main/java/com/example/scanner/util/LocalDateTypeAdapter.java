package com.example.scanner.util;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Override
    public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
                                 final JsonSerializationContext context) {
        // Format with exactly 3 decimal places (milliseconds) to match storage format
        // Truncate nanoseconds to milliseconds for consistent formatting
        LocalDateTime normalized = date.withNano((date.getNano() / 1_000_000) * 1_000_000);
        return new JsonPrimitive(normalized.format(FORMATTER));
    }

    @Override
    public LocalDateTime deserialize(final JsonElement json, final Type typeOfT,
                                     final JsonDeserializationContext context) throws JsonParseException {
        String dateString = json.getAsString();
        // Handle both formats: with nanoseconds (6 digits) and with milliseconds (3 digits)
        if (dateString.contains(".")) {
            int dotIndex = dateString.indexOf('.');
            int tIndex = dateString.indexOf('T');
            if (dotIndex > tIndex) {
                String beforeDot = dateString.substring(0, dotIndex + 1);
                String afterDot = dateString.substring(dotIndex + 1);
                // Normalize to 3 digits (milliseconds)
                if (afterDot.length() > 3) {
                    // Truncate if more than 3 digits (e.g., nanoseconds to milliseconds)
                    afterDot = afterDot.substring(0, 3);
                } else if (afterDot.length() < 3) {
                    // Pad with zeros if less than 3 digits
                    while (afterDot.length() < 3) {
                        afterDot += "0";
                    }
                }
                dateString = beforeDot + afterDot;
            }
        }
        return LocalDateTime.parse(dateString, FORMATTER);
    }

}