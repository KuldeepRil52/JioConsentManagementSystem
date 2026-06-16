package com.jio.digigov.notification.constant;

import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;

/**
 * Default values constants for the DPDP Notification Module.
 *
 * This class provides centralized definition of all default values used in
 * controllers, services, and configuration. It ensures consistency and makes
 * default values easily configurable and maintainable.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
public final class DefaultValues {

    // Prevent instantiation
    private DefaultValues() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========================================
    // Controller Default Values
    // ========================================

    /**
     * Default scope level for template operations.
     * Used when no specific scope is provided in request headers.
     */
    public static final String DEFAULT_SCOPE_LEVEL = ScopeLevel.BUSINESS.name();

    /**
     * Default notification type for template operations.
     * Used when no specific type is provided in request headers.
     */
    public static final String DEFAULT_NOTIFICATION_TYPE = NotificationType.NOTIFICATION.name();

    /**
     * Default sort direction for paginated results.
     * Used when no specific sort direction is provided.
     */
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    /**
     * Default page size for paginated results.
     * Used when no specific page size is provided.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum allowed page size for paginated results.
     * Prevents excessive memory usage and performance issues.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Default language for template operations.
     * Used when no specific language is provided.
     */
    public static final String DEFAULT_LANGUAGE = "EN";

    /**
     * Default country code for SMS operations.
     * Used when no specific country code is provided.
     */
    public static final String DEFAULT_COUNTRY_CODE = "IN";

    // ========================================
    // Processing Default Values
    // ========================================

    /**
     * Default timeout for external service calls in milliseconds.
     */
    public static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds

    /**
     * Default retry attempts for failed operations.
     */
    public static final int DEFAULT_RETRY_ATTEMPTS = 3;

    /**
     * Default batch size for bulk operations.
     */
    public static final int DEFAULT_BATCH_SIZE = 100;

    // ========================================
    // Template Default Values
    // ========================================

    /**
     * Default template version when creating new templates.
     */
    public static final String DEFAULT_TEMPLATE_VERSION = "1.0";

    /**
     * Default email type for email templates.
     */
    public static final String DEFAULT_EMAIL_TYPE = "HTML";

    /**
     * Default sender name for notifications.
     */
    public static final String DEFAULT_SENDER_NAME = "DPDP System";

    // ========================================
    // Configuration Default Values
    // ========================================

    /**
     * Default database name when tenant-specific database is not available.
     */
    public static final String DEFAULT_DATABASE_NAME = "cms_db_admin";

}