package com.jio.digigov.notification.enums;

/**
 * Enumeration defining different types of cacheable data in the notification system.
 * Each cache type has specific TTL and refresh characteristics.
 */
public enum CacheType {

    /**
     * DigiGov authentication tokens for API access
     */
    DIGIGOV_TOKEN("digigov-token", "DigiGov authentication tokens"),

    /**
     * DigiGov admin authentication tokens
     */
    DIGIGOV_ADMIN_TOKEN("digigov-admin-token", "DigiGov admin tokens"),

    /**
     * Business configuration settings (NGConfiguration) - Legacy
     */
    NG_CONFIGURATION("ng-configuration", "Business configuration settings (Legacy)"),

    /**
     * Notification configuration settings (NotificationConfig) - New
     */
    NOTIFICATION_CONFIGURATION("notification-configuration", "Notification configuration settings"),

    /**
     * Master list configuration metadata
     */
    MASTER_LIST_CONFIG("master-list-config", "Master list configurations"),

    /**
     * Resolved database values from master list queries
     */
    MASTER_LIST_DB_VALUE("master-list-db-value", "Resolved database values"),

    /**
     * Event configuration settings per business
     */
    EVENT_CONFIG("event-config", "Event configuration settings"),

    /**
     * SMS/Email template content
     */
    TEMPLATE("template", "SMS/Email templates"),

    /**
     * Database query results for optimization
     */
    DB_QUERY_RESULT("db-query-result", "Database query results"),

    /**
     * Rate limit state for OTP and notification events
     */
    RATE_LIMIT("rate-limit", "Rate limit state");

    private final String key;
    private final String description;

    CacheType(String key, String description) {
        this.key = key;
        this.description = description;
    }

    /**
     * Gets the cache type key used in cache operations
     * @return the cache type key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the human-readable description of this cache type
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Finds a CacheType by its key
     * @param key the key to search for
     * @return the matching CacheType or null if not found
     */
    public static CacheType fromKey(String key) {
        for (CacheType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }
}