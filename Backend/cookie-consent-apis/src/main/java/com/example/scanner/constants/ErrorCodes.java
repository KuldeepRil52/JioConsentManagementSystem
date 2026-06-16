package com.example.scanner.constants;

/**
 * COMPLETELY CORRECTED error codes for the consent management system
 * FIXES APPLIED:
 * - All JCMP codes converted to R-format
 * - No duplicate error codes
 * - Proper user-friendly descriptions
 * - Removed JCMP utility methods
 * - Consistent naming and organization
 */
public final class ErrorCodes {

    // ==== GENERAL ERROR CODES (R4001-R4004) ====
    public static final String VALIDATION_ERROR = "R4001";
    public static final String BUSINESS_RULE_VIOLATION = "R4002";
    public static final String NOT_FOUND = "R4003";
    public static final String TRANSACTION_NOT_FOUND = "R4004";

    // ==== TEMPLATE VALIDATION ERRORS (R4005-R4019) ====
    public static final String TEMPLATE_NAME_REQUIRED = "R4005";
    public static final String BUSINESS_ID_REQUIRED = "R4006";
    public static final String SCAN_ID_REQUIRED = "R4007";
    public static final String SCAN_NOT_COMPLETED = "R4008";
    public static final String INVALID_TEMPLATE_STATUS = "R4009";
    public static final String VERSION_NUMBER_INVALID = "R4010";
    public static final String VERSION_STATUS_TRANSITION_INVALID = "R4011";
    public static final String PREFERENCES_REQUIRED = "R4012";
    public static final String MULTILINGUAL_CONFIG_REQUIRED = "R4013";
    public static final String UI_CONFIG_REQUIRED = "R4014";
    public static final String PURPOSE_IDS_REQUIRED = "R4015";
    public static final String PREFERENCE_VALIDITY_REQUIRED = "R4016";
    public static final String PROCESSOR_ACTIVITY_IDS_REQUIRED = "R4017";

    // ==== PERMISSION/SECURITY ERRORS (R4031-R4039) ====
    public static final String INSUFFICIENT_PERMISSIONS = "R4031";

    // ==== NOT FOUND ERRORS (R4041-R4059) ====
    public static final String TEMPLATE_NOT_FOUND = "R4041";
    public static final String NO_COOKIES_FOUND = "R4042";
    public static final String COOKIE_NOT_FOUND = "R4043";
    public static final String TEMPLATE_VERSION_NOT_FOUND = "R4044";
    public static final String TEMPLATE_NO_ACTIVE_VERSION = "R4045";
    public static final String CONSENT_HANDLE_NOT_FOUND = "R4046";
    public static final String CONSENT_NOT_FOUND = "R4047";
    public static final String CONSENT_VERSION_NOT_FOUND = "R4048";
    public static final String CONSENT_NO_ACTIVE_VERSION = "R4049";
    public static final String CONSENT_JWS_NOT_FOUND = "R40011";
    public static final String MANDATORY_PREFERENCE_REJECTED = "R4230";
    public static final String INCOMPLETE_PREFERENCES = "R4231";


    // ==== HTTP METHOD ERRORS (R4051-R4059) ====
    public static final String METHOD_NOT_ALLOWED = "R4051";

    // ==== CONFLICT ERRORS (R4091-R4099) ====
    public static final String TEMPLATE_NAME_EXISTS = "R4091";
    public static final String TEMPLATE_EXISTS_FOR_SCAN = "R4092";
    public static final String CONSENT_HANDLE_ALREADY_USED = "R4093";
    public static final String CONSENT_HANDLE_EXPIRED = "R4094";
    public static final String CONSENT_CANNOT_UPDATE_EXPIRED = "R4095";
    public static final String CONSENT_HANDLE_CUSTOMER_MISMATCH = "R4096";
    public static final String CONSENT_HANDLE_BUSINESS_MISMATCH = "R4097";
    public static final String CONSENT_VERSION_CONFLICT = "R4098";
    public static final String TEMPLATE_VERSION_CONFLICT = "R4099";

    // ==== COOKIE AND SCAN RELATED ERRORS (R1001-R1099) ====
    public static final String EMPTY_ERROR = "R1001";
    public static final String INVALID_FORMAT_ERROR = "R1002";
    public static final String INVALID_STATE_ERROR = "R1003";
    public static final String DUPLICATE_ERROR = "R1004";

    // ==== SERVER ERROR CODES (R5001-R5099) ====
    public static final String INTERNAL_ERROR = "R5001";
    public static final String SCAN_EXECUTION_ERROR = "R5002";
    public static final String EXTERNAL_SERVICE_ERROR = "R5003";
    public static final String CATEGORIZATION_ERROR = "R5004";
    public static final String VERSION_INTEGRITY_CHECK_FAILED = "R5005";
    public static final String REFERENCE_INTEGRITY_VIOLATION = "R5006";
    public static final String TENANT_ISOLATION_VIOLATION = "R5007";
    public static final String EXTERNAL_SOURCE_CODE_SERVICE_ERROR = "R5008";

    // ==== BUSINESS RULE VIOLATIONS (R4221-R4299) ====
    public static final String TEMPLATE_NOT_UPDATABLE = "R4221";
    public static final String TEMPLATE_MULTIPLE_ACTIVE_VERSIONS = "R4222";
    public static final String TEMPLATE_UPDATE_DRAFT_NOT_ALLOWED = "R4223";
    public static final String CONSENT_MULTIPLE_ACTIVE_VERSIONS = "R4224";
    public static final String CONCURRENT_VERSION_CREATION = "R4225";
    public static final String IMMUTABLE_FIELD_MODIFICATION = "R4226";
    public static final String UPDATE_FREQUENCY_LIMIT_EXCEEDED = "R4227";
    public static final String UPDATE_NOT_ALLOWED_BUSINESS_HOURS = "R4228";
    public static final String INVALID_TEMPLATE = "R4229";
    public static final String MISSING_MANDATORY_PREFERENCE = "R4232";

    public static final String CATEGORY_NOT_FOUND = "CAT4041";
    public static final String CATEGORY_UPDATE_FAILED = "CAT5001";

    public static final String CONSENT_CANNOT_UPDATE_REVOKED = "R4100";

    public static final String INVALID_REQUEST = "R4120";


    private ErrorCodes() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Get human-readable description for error code
     * @param errorCode The error code
     * @return Human-readable description
     */
    public static String getDescription(String errorCode) {
        return switch (errorCode) {
            // General errors
            case VALIDATION_ERROR -> "Validation failed";
            case BUSINESS_RULE_VIOLATION -> "Business rule violation";
            case NOT_FOUND -> "Resource not found";
            case TRANSACTION_NOT_FOUND -> "Transaction not found";

            // Template validation errors
            case TEMPLATE_NAME_REQUIRED -> "Template name is required and cannot be empty";
            case BUSINESS_ID_REQUIRED -> "Business ID is required and cannot be empty";
            case SCAN_ID_REQUIRED -> "Scan ID is required and must be from a completed scan";
            case SCAN_NOT_COMPLETED -> "Scan status is not COMPLETED";
            case INVALID_TEMPLATE_STATUS -> "Template status must be either DRAFT or PUBLISHED";
            case VERSION_NUMBER_INVALID -> "Invalid version number";
            case VERSION_STATUS_TRANSITION_INVALID -> "Invalid version status transition";
            case PREFERENCES_REQUIRED -> "At least one preference is required";
            case MULTILINGUAL_CONFIG_REQUIRED -> "Multilingual configuration is required";
            case UI_CONFIG_REQUIRED -> "UI configuration is required";
            case PURPOSE_IDS_REQUIRED -> "Purpose IDs are required for each preference";
            case PREFERENCE_VALIDITY_REQUIRED -> "Preference validity is required";
            case PROCESSOR_ACTIVITY_IDS_REQUIRED -> "Processor activity IDs are required for each preference";
            case INCOMPLETE_PREFERENCES -> "Request must contain all template preferences";

            // Permission errors
            case INSUFFICIENT_PERMISSIONS -> "Insufficient permissions for operation";

            // Not found errors
            case TEMPLATE_NOT_FOUND -> "Template not found";
            case NO_COOKIES_FOUND -> "No cookies found";
            case COOKIE_NOT_FOUND -> "Cookie not found";
            case TEMPLATE_VERSION_NOT_FOUND -> "Template version not found";
            case TEMPLATE_NO_ACTIVE_VERSION -> "Template has no active version";
            case CONSENT_HANDLE_NOT_FOUND -> "Consent handle not found";
            case CONSENT_NOT_FOUND -> "Consent not found";
            case CONSENT_VERSION_NOT_FOUND -> "Consent version not found";
            case CONSENT_NO_ACTIVE_VERSION -> "Consent has no active version";

            // HTTP method errors
            case METHOD_NOT_ALLOWED -> "HTTP method not allowed";

            // Conflict errors
            case TEMPLATE_NAME_EXISTS -> "Template name already exists for this tenant";
            case TEMPLATE_EXISTS_FOR_SCAN -> "Template already exists for this scan ID";
            case CONSENT_HANDLE_ALREADY_USED -> "Consent handle already used";
            case CONSENT_HANDLE_EXPIRED -> "Consent handle has expired";
            case CONSENT_CANNOT_UPDATE_EXPIRED -> "Cannot update expired consent";
            case CONSENT_HANDLE_CUSTOMER_MISMATCH -> "Consent handle customer does not match consent customer";
            case CONSENT_HANDLE_BUSINESS_MISMATCH -> "Consent handle business does not match consent business";
            case CONSENT_VERSION_CONFLICT -> "Consent version conflict during update";
            case TEMPLATE_VERSION_CONFLICT -> "Template version conflict during update";
            case MISSING_MANDATORY_PREFERENCE -> "Mandatory preference must be provided";

            // Cookie and scan errors
            case EMPTY_ERROR -> "Empty data provided";
            case INVALID_FORMAT_ERROR -> "Invalid data format";
            case INVALID_STATE_ERROR -> "Invalid state for operation";
            case DUPLICATE_ERROR -> "Duplicate resource";

            // Server errors
            case INTERNAL_ERROR -> "Internal server error";
            case SCAN_EXECUTION_ERROR -> "Scan execution error";
            case EXTERNAL_SERVICE_ERROR -> "External service error";
            case CATEGORIZATION_ERROR -> "Cookie categorization error";
            case VERSION_INTEGRITY_CHECK_FAILED -> "Version integrity check failed";
            case REFERENCE_INTEGRITY_VIOLATION -> "Reference integrity violation";
            case TENANT_ISOLATION_VIOLATION -> "Tenant isolation violation";
            case MANDATORY_PREFERENCE_REJECTED -> "Mandatory preferences cannot be rejected";

            // Business rule violations
            case TEMPLATE_NOT_UPDATABLE -> "Template not in valid state for update";
            case TEMPLATE_MULTIPLE_ACTIVE_VERSIONS -> "Multiple active template versions found";
            case TEMPLATE_UPDATE_DRAFT_NOT_ALLOWED -> "Cannot update draft template - use direct edit instead";
            case CONSENT_MULTIPLE_ACTIVE_VERSIONS -> "Multiple active consent versions found";
            case CONCURRENT_VERSION_CREATION -> "Concurrent version creation detected";
            case IMMUTABLE_FIELD_MODIFICATION -> "Immutable field modification attempted";
            case UPDATE_FREQUENCY_LIMIT_EXCEEDED -> "Update frequency limit exceeded";
            case UPDATE_NOT_ALLOWED_BUSINESS_HOURS -> "Update not allowed during business hours";

            case CONSENT_CANNOT_UPDATE_REVOKED -> "Cannot update revoked consent";
            case CONSENT_JWS_NOT_FOUND -> "x-jws-signature is required in the header.";

            case EXTERNAL_SOURCE_CODE_SERVICE_ERROR -> "External service communication failed";

            default -> "Unknown error";
        };
    }
}