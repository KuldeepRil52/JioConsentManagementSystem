package com.jio.digigov.grievance.constant;

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
     * Header for transaction ID tracking.
     * Unique identifier for tracking requests across services.
     */
    public static final String X_TRANSACTION_ID = "X-Transaction-ID";

    /**
     * Header for Scope Level tracking.
     * Unique identifier for tracking requests across services.
     */
    public static final String X_SCOPE_LEVEL = "X-Scope-Level";

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
}
