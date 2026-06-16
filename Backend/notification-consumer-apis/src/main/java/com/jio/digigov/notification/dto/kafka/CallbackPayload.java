package com.jio.digigov.notification.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Callback-specific payload for Kafka notification messages in the DPDP system.
 *
 * This class contains comprehensive webhook delivery information for notifying
 * Data Fiduciaries and Data Processors about privacy events. It supports JWT-based
 * authentication, custom headers, and configurable timeout settings for reliable
 * webhook delivery in enterprise environments.
 *
 * Callback Types Supported:
 * - Data Fiduciary notifications (consent changes, breach alerts)
 * - Data Processor notifications (processing requests, compliance updates)
 * - System webhook callbacks (integration endpoints, status updates)
 *
 * Security Features:
 * - JWT token authentication with configurable expiry
 * - Custom webhook headers for authentication and routing
 * - Payload signature verification for data integrity
 * - Timeout configuration for reliable delivery
 *
 * Webhook Standards Compliance:
 * - HTTP POST method with JSON payload
 * - Standard HTTP headers for content type and authentication
 * - Retry mechanism with exponential backoff
 * - Dead letter queue for failed deliveries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackPayload {

    /**
     * Target webhook URL for the callback delivery.
     * Must be a valid HTTPS URL for security compliance.
     * Should be registered and verified by the recipient organization.
     */
    private String callbackUrl;

    /**
     * Gets the webhook URL (alias for callbackUrl).
     * @return webhook URL
     */
    public String getWebhookUrl() {
        return callbackUrl;
    }

    /**
     * Template ID for the callback payload.
     * Used to resolve the payload template with dynamic arguments.
     */
    private String payloadTemplateId;

    /**
     * Template argument values for placeholder substitution.
     * Key-value pairs used to replace placeholders in callback templates.
     */
    private Map<String, Object> arguments;

    /**
     * Webhook secret for signature generation.
     * Used to create HMAC signatures for payload verification.
     */
    private String webhookSecret;

    /**
     * HTTP method for the webhook request.
     * Usually "POST" for callback notifications.
     */
    @Builder.Default
    private String httpMethod = "POST";
    
    /**
     * Gets the HTTP method for the webhook request.
     * @return HTTP method (default: "POST")
     */
    public String getHttpMethod() {
        return httpMethod != null ? httpMethod : "POST";
    }

    /**
     * Custom headers as a map for easier access.
     * Converts webhook headers list to map format.
     */
    public Map<String, String> getHeaders() {
        if (webhookHeaders == null) {
            return Map.of();
        }
        Map<String, String> headerMap = new HashMap<>();
        for (String header : webhookHeaders) {
            String[] parts = header.split(":", 2);
            if (parts.length == 2) {
                headerMap.put(parts[0].trim(), parts[1].trim());
            }
        }
        return headerMap;
    }

    /**
     * JWT authentication token for webhook security.
     * Contains signed claims about the event and sender identity.
     * Recipient should validate token signature and expiry.
     */
    private String jwtToken;

    /**
     * Type of recipient for the callback notification.
     * Determines the payload structure and expected response format.
     * Values: "DATA_FIDUCIARY", "DATA_PROCESSOR", "SYSTEM_WEBHOOK"
     */
    private String recipientType;

    /**
     * Unique identifier of the recipient organization.
     * Used for webhook URL resolution and audit tracking.
     * Maps to registered Data Fiduciary or Data Processor ID.
     */
    private String recipientId;

    /**
     * Event-specific data payload for the callback.
     * Contains all relevant information about the privacy event.
     * Structure varies based on event type and recipient needs.
     */
    private Map<String, Object> eventData;

    /**
     * Custom HTTP headers to include in the webhook request.
     * Supports additional authentication, routing, and processing hints.
     * Example: ["X-API-Key: abc123", "X-Source: dpdp-notification"]
     */
    private List<String> webhookHeaders;

    /**
     * Timeout in seconds for the webhook HTTP request.
     * Defines maximum wait time for recipient response.
     * Used to prevent hanging connections and enable retries.
     */
    private Integer timeoutSeconds;

    /**
     * Event type classification for the callback.
     * Helps recipients route and process the webhook appropriately.
     * Examples: "CONSENT_GRANTED", "DATA_BREACH", "PROCESSING_REQUEST"
     */
    private String eventType;

    /**
     * Correlation ID linking this callback to the original event.
     * Enables end-to-end tracking across multiple systems.
     * Used for debugging and audit trail construction.
     */
    private String correlationId;

    /**
     * Expected response format from the webhook recipient.
     * Defines the structure and content of the expected acknowledgment.
     * Examples: "JSON", "XML", "PLAIN_TEXT"
     */
    private String expectedResponseFormat;

    /**
     * Maximum number of retry attempts for failed webhooks.
     * Overrides default retry policy for specific callbacks.
     * Used for high-priority or time-sensitive notifications.
     */
    private Integer maxRetryAttempts;

    /**
     * Webhook delivery configuration and metadata.
     * Contains additional settings for delivery optimization and tracking.
     */
    private DeliveryConfiguration deliveryConfig;

    /**
     * Webhook delivery configuration options.
     * Controls how the webhook should be delivered and processed.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryConfiguration {

        /**
         * Flag to enable webhook signature verification.
         * When true, includes HMAC signature in webhook headers
         * for payload integrity verification.
         */
        private boolean enableSignatureVerification;

        /**
         * Signature algorithm for webhook payload verification.
         * Examples: "HMAC-SHA256", "HMAC-SHA512"
         * Used when signature verification is enabled.
         */
        private String signatureAlgorithm;

        /**
         * Flag to enable detailed delivery tracking.
         * When true, records detailed timing and response information
         * for performance analysis and debugging.
         */
        private boolean enableDetailedTracking;

        /**
         * Custom user agent string for webhook requests.
         * Identifies the notification service to webhook recipients.
         * Example: "DPDP-Notification-Service/1.0"
         */
        private String userAgent;

        /**
         * Flag to follow HTTP redirects automatically.
         * When true, webhook client follows 3xx redirect responses
         * up to a maximum number of redirects.
         */
        private boolean followRedirects;

        /**
         * Maximum number of HTTP redirects to follow.
         * Prevents infinite redirect loops while allowing
         * legitimate endpoint relocations.
         */
        private Integer maxRedirects;

        /**
         * Additional metadata for webhook processing.
         * Extensible field for future configuration options
         * without breaking existing integrations.
         */
        private Map<String, String> additionalMetadata;
    }
}