package com.jio.digigov.notification.constant;

/**
 * Centralized constants for the DPDP Notification Consumer.
 * Contains Kafka topics, MongoDB collections, headers, validation limits, and defaults.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public final class NotificationConstants {

    // Kafka Topics
    public static final String KAFKA_TOPIC_SMS = "notification.sms";
    public static final String KAFKA_TOPIC_EMAIL = "notification.email";
    public static final String KAFKA_TOPIC_CALLBACK = "notification.callback";
    public static final String KAFKA_TOPIC_DLQ = "notification.dlq";

    // MongoDB Collections
    public static final String COLLECTION_NOTIFICATIONS = "notification_events";
    public static final String COLLECTION_TEMPLATES = "notification_templates";
    public static final String COLLECTION_CONFIGURATIONS = "configurations";
    public static final String COLLECTION_AUDIT = "audit_logs";
    public static final String COLLECTION_SMS_NOTIFICATIONS = "notification_sms";
    public static final String COLLECTION_EMAIL_NOTIFICATIONS = "notification_email";
    public static final String COLLECTION_CALLBACK_NOTIFICATIONS = "notification_callback";
    public static final String COLLECTION_EVENT_CONFIGURATIONS = "event_configurations";
    public static final String COLLECTION_DATA_PROCESSORS = "data_processors";

    // HTTP Headers
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_BUSINESS_ID = "X-Business-Id";
    public static final String HEADER_TRANSACTION_ID = "X-Transaction-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_SOURCE_IP = "X-Source-IP";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_SESSION_ID = "X-Session-ID";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    // Kafka Headers
    public static final String KAFKA_HEADER_SOURCE_IP = "X-Source-IP";
    public static final String KAFKA_HEADER_USERNAME = "X-Username";
    public static final String KAFKA_HEADER_SESSION_ID = "X-Session-ID";
    public static final String KAFKA_HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String KAFKA_HEADER_TIMESTAMP = "X-Timestamp";

    // Validation Limits
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int MAX_RECIPIENTS = 100;
    public static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB
    public static final int MAX_TEMPLATE_ARGS = 50;
    public static final int SMS_MAX_LENGTH = 160;
    public static final int EMAIL_SUBJECT_MAX_LENGTH = 255;
    public static final int MAX_REMARK_LENGTH = 500;

    // Defaults
    public static final String DEFAULT_TENANT = "default";
    public static final String DEFAULT_USERNAME = "SYSTEM";
    public static final String DEFAULT_LANGUAGE = "english";
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    // Status Values
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RETRY_SCHEDULED = "RETRY_SCHEDULED";

    // Notification Channels
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_CALLBACK = "CALLBACK";

    // Kafka Message Key Suffixes for Callback Recipients
    public static final String MESSAGE_KEY_SUFFIX_DATA_FIDUCIARY = "_DF";
    public static final String MESSAGE_KEY_SUFFIX_DATA_PROCESSOR = "_DP_";

    // Event Priorities
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_LOW = "LOW";

    // Template Types
    public static final String TEMPLATE_TYPE_SMS = "SMS";
    public static final String TEMPLATE_TYPE_EMAIL = "EMAIL";
    public static final String TEMPLATE_TYPE_CALLBACK = "CALLBACK";

    // DLT Constants (TRAI Compliance)
    public static final String DLT_CATEGORY_TRANSACTIONAL = "T";
    public static final String DLT_CATEGORY_PROMOTIONAL = "P";
    public static final String DLT_CATEGORY_SERVICE_IMPLICIT = "SI";
    public static final String DLT_CATEGORY_SERVICE_EXPLICIT = "SE";

    // Cache Keys
    public static final String CACHE_KEY_TOKEN = "token:";
    public static final String CACHE_KEY_TEMPLATE = "template:";
    public static final String CACHE_KEY_CONFIG = "config:";
    public static final String CACHE_KEY_MASTER_LIST = "masterlist:";

    // Error Messages
    public static final String ERROR_INVALID_TENANT = "Invalid or missing tenant identifier";
    public static final String ERROR_INVALID_REQUEST = "Invalid request format or missing required fields";
    public static final String ERROR_TEMPLATE_NOT_FOUND = "Template not found";
    public static final String ERROR_CONFIG_NOT_FOUND = "Configuration not found";
    public static final String ERROR_UNAUTHORIZED = "Missing or invalid authentication";

    // Date/Time Formats
    public static final String DATE_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String DATE_FORMAT_SIMPLE = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_FILE = "yyyyMMdd_HHmmss";

    // Regex Patterns
    public static final String PATTERN_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PATTERN_MOBILE = "^[6-9]\\d{9}$";
    public static final String PATTERN_TENANT_ID = "^[a-zA-Z0-9_-]{2,50}$";
    public static final String PATTERN_EVENT_ID = "^[a-zA-Z0-9_-]{1,100}$";

    // Consumer-Specific Constants
    public static final int CONSUMER_RETRY_BACKOFF_MULTIPLIER = 2;
    public static final int CONSUMER_MAX_BATCH_SIZE = 100;
    public static final int CONSUMER_POLL_TIMEOUT_MS = 10000;
    public static final int CONSUMER_SESSION_TIMEOUT_MS = 30000;

    private NotificationConstants() {
        // Prevent instantiation of utility class
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}