package com.jio.digigov.auditmodule.util;

import com.jio.digigov.auditmodule.dto.AuditRequest;
import com.jio.digigov.auditmodule.exception.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Utility methods for building queries and hashing audit payloads.
 */
@Slf4j
@UtilityClass
public class AuditQueryUtils {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<String> SUPPORTED_PARAMS = Set.of(
            "businessId", "component", "actionType", "group", "initiator",
            "actorId", "resourceId", "status", "auditId"
    );

    /**
     * Validates businessId is not null or blank.
     */
    public static void validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new ValidationException("X-Business-Id header is required for audit operations");
        }
    }

    /**
     * Builds Mongo query from supported params.
     */
    public static Query buildQuery(Map<String, String> params) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();
            log.info("Processing filter param field={} value={}", field, value);
            if (SUPPORTED_PARAMS.contains(field) && value != null && !value.isBlank()) {
                criteria.add(Criteria.where(field).is(value));
            } else if (!field.equalsIgnoreCase("tenantId") && !field.equalsIgnoreCase("businessId")) {
                log.warn("Ignoring unsupported filter param field={} value={}", field, value);
            }
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        return query;
    }

    /**
     * Returns SHA-256 hash of the payload.
     */
    public static String hashPayload(AuditRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String json = gson.toJson(request);
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash payload", e);
        }
    }
}
