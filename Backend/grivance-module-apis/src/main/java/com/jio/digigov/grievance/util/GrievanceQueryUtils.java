package com.jio.digigov.grievance.util;

import com.jio.digigov.grievance.exception.InvalidBusinessIdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GrievanceQueryUtils {

    public static void validateBusinessId(String businessId) {
        if (businessId == null || businessId.isBlank()) {
            log.error("[GrievanceQueryUtils] Invalid or missing BusinessId: {}", businessId);
            throw new InvalidBusinessIdException("Invalid or missing BusinessId");
        }
        log.debug("[GrievanceQueryUtils] Validated BusinessId: {}", businessId);
    }

    /**
     * Build dynamic MongoDB query based on params.
     * Dynamically maps userDetails.* partial keys from MongoDB schema.
     */
    public static Query buildQuery(Map<String, String> params) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (params == null || params.isEmpty()) {
            log.debug("[GrievanceQueryUtils] No search parameters provided, returning empty query.");
            return query;
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null || value.isBlank()) {
                log.debug("[GrievanceQueryUtils] Skipping empty value for key: {}", key);
                continue;
            }

            if (isSystemParam(key)) {
                log.debug("[GrievanceQueryUtils] Skipping system param: {}", key);
                continue;
            }

            String field = key;

            // Handle userDetails.* dynamic mapping
            if (field.startsWith("userDetails.") && field.split("\\.").length == 2) {

                String rawKey = field.substring("userDetails.".length());
                String matched = matchDynamicUserDetailsKey(rawKey);

                if (matched != null) {
                    field = "userDetails." + matched;
                    log.info("[GrievanceQueryUtils] Dynamically mapped '{}' → '{}'", rawKey, matched);
                } else {
                    log.warn("[GrievanceQueryUtils] No match found for '{}'", rawKey);
                }
            }

            field = escapeNestedKey(field);

            log.info("[GrievanceQueryUtils] Applying filter - field: '{}', value: '{}'", field, value);

            // Exact match for status
            if ("status".equalsIgnoreCase(field)) {
                criteriaList.add(Criteria.where(field).is(value.trim()));
                continue;
            }

            // Numeric exact match
            if (field.startsWith("userDetails.") && value.matches("\\d+")) {
                criteriaList.add(Criteria.where(field).is(value.trim()));
            } else {
                // Regex match for flexible search
                String escaped = escapeRegex(value.trim());
                criteriaList.add(Criteria.where(field).regex(".*" + escaped + ".*", "i"));
            }
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList));
        }

        return query;
    }

    // ------------------------------------------
    // 🔥 DYNAMIC USER FIELD MATCHING ENGINE
    // ------------------------------------------

    private static String matchDynamicUserDetailsKey(String rawKey) {
        Set<String> dynamicKeys = DynamicUserDetailsFieldLoader.getUSER_DETAIL_KEYS();
        if (dynamicKeys == null || dynamicKeys.isEmpty()) return null;

        String cleaned = normalize(rawKey);

        // 1️⃣ Exact normalized match
        for (String key : dynamicKeys) {
            if (normalize(key).equals(cleaned)) return key;
        }

        // 2️⃣ Starts-with match
        for (String key : dynamicKeys) {
            if (normalize(key).startsWith(cleaned)) return key;
        }

        // 3️⃣ Contains match
        for (String key : dynamicKeys) {
            if (normalize(key).contains(cleaned)) return key;
        }

        // 4️⃣ Initials match ("fn" → "First Name")
        for (String key : dynamicKeys) {
            String initials = Arrays.stream(key.split(" "))
                    .map(w -> w.substring(0, 1).toLowerCase())
                    .collect(Collectors.joining());

            if (initials.equals(cleaned)) return key;
        }

        return null;
    }

    private static String normalize(String val) {
        return val.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static boolean isSystemParam(String key) {
        return key.equalsIgnoreCase("tenantId") ||
                key.equalsIgnoreCase("businessId") ||
                key.equalsIgnoreCase("page") ||
                key.equalsIgnoreCase("size");
    }

    private static String escapeNestedKey(String key) {
        return key;
    }

    private static String escapeRegex(String input) {
        return input.replaceAll("([\\\\.*+?\\[^\\]$(){}=!<>|:\\-])", "\\\\$1");
    }
}