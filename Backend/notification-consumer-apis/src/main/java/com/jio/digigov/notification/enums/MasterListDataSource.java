package com.jio.digigov.notification.enums;

/**
 * Enumeration representing different data sources for master list resolution.
 *
 * This enum defines the various sources from which template argument values
 * can be resolved during notification processing:
 *
 * - PAYLOAD: Extract values from the incoming TriggerEventRequestDto payload
 * - TOKEN: Decrypt JWT token and extract values from claims
 * - DB: Query tenant-specific database collections
 * - GENERATE: Generate dynamic values using configured generators
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
public enum MasterListDataSource {

    /**
     * Extract values from the TriggerEventRequestDto payload.
     * Supports nested object navigation using dot notation (e.g., "eventPayload.customerName").
     * Can access customerIdentifiers, eventPayload, and all other request fields.
     */
    PAYLOAD,

    /**
     * Decrypt JWT token from eventPayload.token and extract values from claims.
     * Uses existing jwt.callback.secret configuration for token validation.
     * Supports nested claim navigation using dot notation (e.g., "claims.user.id").
     */
    TOKEN,

    /**
     * Query tenant-specific database collections.
     * Always queries using businessId and supports batch queries per collection.
     * Supports nested field extraction using dot notation (e.g., "user.profile.name").
     */
    DB,

    /**
     * Generate dynamic values using configured generators.
     * Supports OTP, UUID, TIMESTAMP, RANDOM_STRING generators with configurable options.
     * Values are regenerated for each resolution (not cached).
     */
    GENERATE
}