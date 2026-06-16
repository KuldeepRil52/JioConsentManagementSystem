package com.jio.digigov.notification.dto.request.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for triggering notification events.
 * 
 * This DTO encapsulates all the information required to trigger a data protection
 * notification event. It includes event identification, customer details, and
 * optional context information for message personalization.
 * 
 * Usage Context:
 * - Primary entry point for event trigger API calls
 * - Contains all necessary data for event validation and processing
 * - Supports dynamic message template arguments through eventPayload
 * - Enables multi-channel notification delivery coordination
 * 
 * Validation Features:
 * - Jakarta Bean Validation annotations for field validation
 * - Nested validation for complex objects (CustomerIdentifiersDto)
 * - Comprehensive error messages for client guidance
 * 
 * JSON Processing:
 * - Ignores unknown fields for API evolution compatibility
 * - OpenAPI schema annotations for documentation generation
 * - Jackson-compatible serialization/deserialization
 * 
 * Event Flow:
 * 1. Client submits TriggerEventRequestDto to /v1/events/trigger endpoint
 * 2. Request validated against Jakarta Bean Validation constraints
 * 3. Event configuration looked up using eventType and business context
 * 4. Customer identified using customerIdentifiers (mobile/email)
 * 5. Notifications sent to configured recipients via enabled channels
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to trigger a notification event")
public class TriggerEventRequestDto {
    
    /**
     * The type of event being triggered.
     * Must match an existing active event configuration for the business.
     * Examples: "USER_REGISTRATION", "CONSENT_GRANTED", "CONSENT_REVOKED", "DATA_BREACH"
     */
    @NotBlank(message = "Event type is required")
    @Schema(description = "Event type identifier", example = "CONSENT_GRANTED", required = true)
    private String eventType;
    
    /**
     * Optional resource type associated with the event.
     * Used for categorization and filtering in event queries.
     * Examples: "consent", "user", "data-processing", "privacy-policy"
     */
    @Schema(description = "Resource type", example = "consent")
    private String resource;
    
    @Schema(description = "Source system identifier", example = "consent-app")
    private String source;
    
    /**
     * Customer identification information for the Data Principal.
     * Used to identify the individual whose data is being processed and who should
     * receive direct notifications. Supports mobile number or email identification.
     */
    @NotNull(message = "Customer identifiers are required")
    @Valid
    @Schema(description = "Customer identification information", required = true)
    private CustomerIdentifiersDto customerIdentifiers;
    
    /**
     * Optional list of specific Data Processor IDs to notify.
     * If provided, only these processors will receive notifications (if enabled in config).
     * If null/empty, all active processors associated with the business will be notified.
     */
    @Schema(description = "List of data processor IDs to notify")
    private List<String> dataProcessorIds;
    
    @Schema(description = "Language for notification templates", example = "english")
    private String language;
    
    /**
     * Dynamic payload containing event-specific data for template substitution.
     * Key-value pairs that will be used to replace placeholders in notification templates.
     * Examples: {"userName": "John Doe", "consentType": "Marketing", "expiryDate": "2024-12-31"}
     */
    @Schema(description = "Event-specific payload data")
    private Map<String, Object> eventPayload;
    
    /**
     * Nested DTO for customer identification.
     * 
     * Provides a structured way to identify the Data Principal who should receive
     * direct notifications. Supports multiple identifier types for flexibility.
     * 
     * Supported Types:
     * - MOBILE: Phone number (with country code, e.g., "919867123456")
     * - EMAIL: Email address (e.g., "user@example.com")
     * 
     * The identifier is used to:
     * 1. Lookup customer contact preferences
     * 2. Determine appropriate notification channels
     * 3. Personalize notification content
     * 4. Track notification delivery status
     */
    @Data
    @Schema(description = "Customer identification information")
    public static class CustomerIdentifiersDto {
        
        /**
         * Type of customer identifier.
         * Determines how the value should be interpreted and which notification channels are available.
         */
        @NotBlank(message = "Identifier type is required")
        @Schema(description = "Type of identifier", example = "MOBILE", allowableValues = {"MOBILE", "EMAIL"}, required = true)
        private String type;
        
        /**
         * The actual identifier value.
         * Format depends on type:
         * - MOBILE: Phone number with country code (e.g., "919867123456")
         * - EMAIL: Valid email address (e.g., "user@example.com")
         */
        @NotBlank(message = "Identifier value is required")
        @Schema(description = "Identifier value", example = "919867123456", required = true)
        private String value;
    }
}