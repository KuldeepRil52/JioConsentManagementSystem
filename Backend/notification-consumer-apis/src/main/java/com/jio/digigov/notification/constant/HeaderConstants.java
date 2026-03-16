package com.jio.digigov.notification.constant;

/**
 * HTTP header constants for the DPDP Notification Module.
 *
 * This class provides centralized definition of all HTTP headers used throughout
 * the application, ensuring consistency and preventing typos in header names.
 *
 * Header Categories:
 * - Custom Application Headers: X-Tenant-Id, X-Business-Id, etc.
 * - Standard HTTP Headers: Content-Type, Authorization, etc.
 * - Authentication Headers: Bearer, Basic, etc.
 * - Webhook Headers: X-Signature, X-Timestamp, etc.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
public final class HeaderConstants {

    // Prevent instantiation
    private HeaderConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========================================
    // Custom Application Headers
    // ========================================

    /**
     * Header for tenant identification in multi-tenant requests.
     * Used to route requests to appropriate tenant databases.
     */
    public static final String X_TENANT_ID = "X-Tenant-ID";

    /**
     * Header for business identification within a tenant.
     * Used for business-specific configuration and routing.
     */
    public static final String X_BUSINESS_ID = "X-Business-ID";

    /**
     * Header for scope level specification.
     * Defines the operational scope for the request.
     */
    public static final String X_SCOPE_LEVEL = "X-Scope-Level";

    /**
     * Header for type specification in requests.
     * Used to categorize request types for processing.
     */
    public static final String X_TYPE = "X-Type";

    /**
     * Header for scope level specification.
     * Defines the operational scope for the request (legacy format).
     */
    public static final String SCOPE_LEVEL = "scopeLevel";

    /**
     * Header for type specification (legacy format).
     * Used to categorize request types for processing.
     */
    public static final String TYPE = "type";

    /**
     * Header for recipient type specification.
     * Used to specify the type of recipient for notifications.
     */
    public static final String X_RECIPIENT_TYPE = "X-Recipient-Type";

    /**
     * Header for recipient type specification (legacy format).
     * Used to specify the type of recipient for notifications.
     */
    public static final String RECIPIENT_TYPE = "recipientType";

    /**
     * Header for recipient ID specification.
     * Used to specify the ID of the recipient for notifications.
     */
    public static final String X_RECIPIENT_ID = "X-Recipient-Id";

    /**
     * Header for recipient ID specification (legacy format).
     * Used to specify the ID of the recipient for notifications.
     */
    public static final String RECIPIENT_ID = "recipientId";

    /**
     * Header for transaction ID tracking.
     * Unique identifier for tracking requests across services.
     */
    public static final String X_TRANSACTION_ID = "X-Transaction-ID";

    /**
     * Header for correlation ID tracking.
     * Enables end-to-end request tracing across services.
     */
    public static final String X_CORRELATION_ID = "X-Correlation-ID";

    /**
     * Header for request ID tracking.
     * Unique identifier for individual requests.
     */
    public static final String X_REQUEST_ID = "X-Request-Id";

    /**
     * Header for source system identification.
     * Identifies the originating system for the request.
     */
    public static final String X_SOURCE_SYSTEM = "X-Source-System";

    // ========================================
    // Webhook and Callback Headers
    // ========================================

    /**
     * Header for webhook signature verification.
     * Contains HMAC signature for payload integrity.
     */
    public static final String X_SIGNATURE = "X-Signature";

    /**
     * Header for webhook timestamp.
     * Provides request timestamp for replay attack prevention.
     */
    public static final String X_TIMESTAMP = "X-Timestamp";

    /**
     * Header for webhook event type.
     * Categorizes the webhook event for proper handling.
     */
    public static final String X_EVENT_TYPE = "X-Event-Type";

    /**
     * Header for webhook delivery attempt.
     * Tracks the number of delivery attempts for webhooks.
     */
    public static final String X_DELIVERY_ATTEMPT = "X-Delivery-Attempt";

    // ========================================
    // Standard HTTP Headers
    // ========================================

    /**
     * Standard Content-Type header.
     * Specifies the media type of the request/response body.
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * Standard Authorization header.
     * Contains authentication credentials for the request.
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Standard Accept header.
     * Specifies acceptable media types for the response.
     */
    public static final String ACCEPT = "Accept";

    /**
     * Standard User-Agent header.
     * Identifies the client application making the request.
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * X-Forwarded-For header for client IP tracking.
     * Contains the original client IP when behind proxies.
     */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * X-Real-IP header for client IP tracking.
     * Contains the real client IP address.
     */
    public static final String X_REAL_IP = "X-Real-IP";

    // ========================================
    // Content Type Values
    // ========================================

    /**
     * JSON content type for API requests and responses.
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * XML content type for API requests and responses.
     */
    public static final String APPLICATION_XML = "application/xml";

    /**
     * Plain text content type for simple responses.
     */
    public static final String TEXT_PLAIN = "text/plain";

    /**
     * HTML content type for web responses.
     */
    public static final String TEXT_HTML = "text/html";

    /**
     * Form URL-encoded content type for form submissions.
     */
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";

    // ========================================
    // Authentication Prefixes
    // ========================================

    /**
     * Bearer token prefix for OAuth/JWT authentication.
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Basic authentication prefix for username:credentials credentials.
     */
    public static final String BASIC_PREFIX = "Basic ";

    /**
     * API Key prefix for key-based authentication.
     */
    public static final String API_KEY_PREFIX = "ApiKey ";
}