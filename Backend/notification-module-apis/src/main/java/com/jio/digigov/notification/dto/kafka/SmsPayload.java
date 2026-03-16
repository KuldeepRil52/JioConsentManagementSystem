package com.jio.digigov.notification.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SMS-specific payload for Kafka notification messages in the DPDP system.
 *
 * This class contains all necessary information for SMS delivery via the DigiGov
 * platform. It includes message content, regulatory compliance data, and delivery
 * preferences required for TRAI DLT compliance and multi-operator support.
 *
 * TRAI DLT Compliance:
 * - DLT Entity ID: Identifies the sending organization
 * - DLT Template ID: Links to pre-approved message template
 * - Content validation against registered templates
 *
 * Multi-Operator Support:
 * - Operator country codes for international delivery
 * - From address customization per operator
 * - Delivery optimization based on recipient location
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsPayload {

    /**
     * Recipient mobile number with country code.
     * Format: Country code + mobile number (e.g., "919876543210")
     * Must be a valid mobile number for SMS delivery.
     */
    private String mobileNumber;

    /**
     * SMS message content after template resolution.
     * Contains the final message text with all placeholders substituted.
     * Must comply with TRAI DLT approved template structure.
     */
    private String template;

    /**
     * Template argument values for placeholder substitution.
     * Key-value pairs used to replace placeholders in the SMS template.
     * Example: {"customerName": "John Doe", "consentId": "CNS123"}
     */
    private Map<String, Object> templateArgs;

    /**
     * DLT Entity ID for TRAI compliance.
     * Identifies the organization sending the SMS message.
     * Required for all commercial SMS in India.
     */
    private String dltEntityId;

    /**
     * DLT Template ID for TRAI compliance.
     * Links to the pre-approved message template in DLT system.
     * Ensures message content matches registered template.
     */
    private String dltTemplateId;

    /**
     * Sender ID or shortcode for SMS delivery.
     * Displayed as the sender name in recipient's SMS inbox.
     * Must be registered with telecom operators.
     */
    private String from;

    /**
     * List of operator country codes for international delivery.
     * Enables SMS routing through specific operators or countries.
     * Format: ISO country codes (e.g., ["IN", "US", "GB"])
     */
    private List<String> oprCountries;

    /**
     * Template ID for the SMS message content.
     * Used to resolve the SMS template with dynamic arguments.
     */
    private String templateId;

    /**
     * Additional delivery options and preferences.
     * Contains SMS-specific delivery configurations and scheduling.
     */
    private DeliveryOptions deliveryOptions;

    /**
     * Gets the template ID for the SMS message.
     * @return template identifier
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Gets the template arguments as objects for backward compatibility.
     * @return template arguments map
     */
    @JsonIgnore
    public Map<String, Object> getArguments() {
        return templateArgs != null ? new HashMap<>(templateArgs) : new HashMap<>();
    }

    /**
     * Sets the template arguments for JSON deserialization.
     * This method accepts both mutable maps and handles various map types
     * to avoid immutable collection issues during Kafka message deserialization.
     */
    @JsonProperty("arguments")
    public void setArguments(Object arguments) {
        if (arguments == null) {
            this.templateArgs = null;
        } else if (arguments instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> argMap = (Map<String, Object>) arguments;
            this.templateArgs = argMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue() != null ? entry.getValue().toString() : null,
                            (existing, replacement) -> replacement,
                            HashMap::new
                    ));
        } else {
            this.templateArgs = new HashMap<>();
        }
    }

    /**
     * Gets the template arguments for JSON serialization.
     * Returns the mutable map directly for serialization.
     * Note: Jackson will use the setter for deserialization and field access for serialization.
     */
    @JsonIgnore
    public Map<String, Object> getArgumentsForSerialization() {
        return templateArgs;
    }

    /**
     * Gets the sender ID for the SMS message.
     * @return sender identifier
     */
    public String getSenderId() {
        return from;
    }

    /**
     * SMS delivery configuration options.
     * Controls how and when the SMS message should be delivered.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryOptions {

        /**
         * Flag to send as Flash SMS (Class 0).
         * Flash SMS is displayed immediately on recipient's screen
         * without being stored in the SMS inbox.
         */
        private boolean flashSms;

        /**
         * Flag to enable Unicode support for non-ASCII characters.
         * Required for messages containing special characters, emojis,
         * or non-Latin scripts (Hindi, Arabic, etc.).
         */
        private boolean unicodeSupport;

        /**
         * Scheduled delivery time for the SMS.
         * If null, SMS is sent immediately.
         * Future timestamp for delayed delivery.
         */
        private LocalDateTime scheduleTime;

        /**
         * Request delivery receipt from operator.
         * Enables tracking of message delivery status
         * at the operator level.
         */
        private boolean requestDeliveryReceipt;

        /**
         * Validity period for the SMS message.
         * Defines how long operators should attempt delivery
         * before considering the message expired.
         */
        private Integer validityPeriodHours;

        /**
         * Custom message priority for operator routing.
         * Higher priority messages may receive preferential treatment
         * in operator queues.
         */
        private Integer messagePriority;
    }
}