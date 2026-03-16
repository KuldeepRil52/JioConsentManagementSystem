package com.jio.digigov.notification.dto.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Event message for Kafka-based asynchronous notification processing.
 *
 * This class represents the raw event data sent from producer to all consumers.
 * Consumers will independently validate and create notification records based on
 * event configuration and customer identifier type.
 *
 * Message Flow:
 * 1. Producer receives TriggerEventRequestDto and creates EventMessage
 * 2. Same EventMessage sent to all Kafka topics (sms, email, callback)
 * 3. Each consumer decides whether to process based on:
 *    - Customer identifier type compatibility
 *    - Event configuration settings
 *    - Application properties fallback
 * 4. Consumers create notification records and handle processing/failures
 *
 * Consumer Processing:
 * - SMS Consumer: Processes if customerIdentifierType = "MOBILE"
 * - EMAIL Consumer: Processes if customerIdentifierType = "EMAIL"
 * - Callback Consumer: Always processes (creates DF + DP callbacks)
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventMessage {

    /**
     * Unique identifier for this message instance.
     * Generated when message is created for tracking across entire lifecycle.
     */
    private String messageId;

    /**
     * Unique identifier for the parent event that triggered this message.
     * Links back to the original TriggerEventRequestDto.
     */
    private String eventId;

    /**
     * Correlation identifier linking related processing across consumers.
     * Used for tracing the same event across SMS, EMAIL, and CALLBACK processing.
     */
    private String correlationId;

    /**
     * Tenant identifier for multi-tenant data isolation.
     * Determines which MongoDB database to use for lookups and record creation.
     */
    private String tenantId;

    /**
     * Business identifier for business-specific configuration lookup.
     * Used by consumers to fetch event configurations and validate settings.
     */
    private String businessId;

    /**
     * Transaction identifier for tracking request flow across services.
     * Used for correlation and tracing in distributed processing.
     */
    private String transactionId;

    /**
     * Source IP address of the client that triggered the event.
     * Extracted from HTTP headers (X-Forwarded-For, X-Real-IP, or RemoteAddr).
     * Used for audit trails and security tracking in consumers.
     */
    private String sourceIp;

    /**
     * Type of event from the original trigger request.
     * Used by consumers for template resolution and event configuration lookup.
     */
    private String eventType;

    /**
     * Optional resource type associated with the event.
     * Used for categorization and filtering.
     */
    private String resource;

    /**
     * Source system that triggered the event.
     * Used for audit trails and routing decisions.
     */
    private String source;

    /**
     * Customer identification information for the Data Principal.
     * Contains type (MOBILE/EMAIL) and value for notification delivery.
     */
    private CustomerIdentifiers customerIdentifiers;

    /**
     * Language for notification templates.
     * Used by consumers for template resolution.
     */
    private String language;

    /**
     * Optional list of specific Data Processor IDs to notify.
     * Used by callback consumer for DP validation and callback creation.
     */
    private List<String> dataProcessorIds;

    /**
     * Dynamic payload containing event-specific data.
     * Raw data that consumers will use for template argument resolution.
     */
    private Map<String, Object> eventPayload;

    /**
     * Timestamp when the message was created.
     * Used for message aging, monitoring, and audit trails.
     */
    private LocalDateTime timestamp;

    /**
     * Recipient type for this notification message.
     * Used to distinguish between Data Principal, DPO, and other recipient types.
     * Defaults to null for backward compatibility (treated as DATA_PRINCIPAL).
     */
    @Schema(description = "Recipient type for this notification",
            example = "DATA_PRINCIPAL",
            allowableValues = {"DATA_PRINCIPAL", "DATA_FIDUCIARY", "DATA_PROCESSOR", "DATA_PROTECTION_OFFICER"})
    private String recipientType;

    /**
     * JWT token containing event payload and metadata.
     * Only populated for DPO emails - contains the complete event context
     * for template argument resolution in the consumer.
     */
    @Schema(description = "JWT token for DPO emails (contains event payload)")
    private String jwtToken;

    /**
     * Customer identification nested class.
     * Contains the type and value for identifying the Data Principal.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerIdentifiers {
        /**
         * Type of customer identifier (MOBILE or EMAIL).
         * Determines which consumer will process this event.
         */
        private String type;

        /**
         * The actual identifier value.
         * Phone number for MOBILE, email address for EMAIL.
         */
        private String value;
    }
}