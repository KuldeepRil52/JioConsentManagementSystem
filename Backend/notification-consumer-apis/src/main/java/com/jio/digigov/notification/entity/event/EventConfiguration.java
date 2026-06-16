package com.jio.digigov.notification.entity.event;

import com.jio.digigov.notification.entity.base.BaseEntity;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.ScopeLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Event Configuration entity for defining notification rules and settings.
 *
 * This entity represents the configuration for how notifications should be sent when specific
 * events occur within a business context. It defines which recipients should be notified,
 * through which channels, and with what priority for each event type.
 *
 * Architecture:
 * - Uses multi-tenant database architecture with tenant-specific databases
 * - No tenantId field needed as each tenant has a separate database
 * - Business-scoped configurations (each business can have multiple event configs)
 * - Unique constraint ensures one configuration per business+eventType combination
 *
 * Notification Recipients:
 * - Data Principal: The individual whose personal data is being processed
 * - Data Fiduciary: The organization responsible for determining data processing purposes
 * - Data Processor: Third-party entities that process data on behalf of the fiduciary
 *
 * Event Lifecycle:
 * - ACTIVE: Configuration is operational and will trigger notifications
 * - INACTIVE: Configuration is disabled (soft delete) but preserved for audit
 *
 * Index Strategy:
 * - Unique index on (businessId, eventType) for primary lookups
 * - Composite index on (businessId, isActive) for active configuration queries
 * - Unique index on configId for direct configuration access
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "event_configurations")
@CompoundIndexes({
        @CompoundIndex(name = "unique_event_business_idx",
                def = "{'businessId': 1, 'eventType': 1}",
                unique = true),
        @CompoundIndex(name = "business_active_idx",
                def = "{'businessId': 1, 'isActive': 1}")
})
public class EventConfiguration extends BaseEntity {

    /**
     * Unique configuration identifier automatically generated for each event configuration.
     * Format: EC_{timestamp}_{uuid_fragment}
     * Example: "EC_1640995200000_A1B2C3D4"
     * Used for direct configuration lookup and API references.
     */
    @Indexed(unique = true)
    @Field("configId")
    private String configId;

    /**
     * Business identifier that owns this event configuration.
     * Each business can have multiple event configurations but only one per event type.
     * Used for tenant isolation and business-specific notification rules.
     */
    @NotBlank(message = "Business ID is required")
    @Field("businessId")
    private String businessId;

    // Scope level (BUSINESS only for now)
    @NotNull
    @Field("scopeLevel")
    @Builder.Default
    private ScopeLevel scopeLevel = ScopeLevel.BUSINESS;

    /**
     * The type of event this configuration handles.
     * Examples: "USER_REGISTRATION", "CONSENT_GRANTED", "CONSENT_REVOKED", "DATA_BREACH"
     * Must be unique within a business context (enforced by database constraint).
     */
    @NotBlank(message = "Event type is required")
    @Field("eventType")
    private String eventType;

    /**
     * Nested configuration object defining notification settings for different recipient types.
     * Contains settings for Data Principal, Data Fiduciary, and Data Processor notifications.
     * Each recipient type can have different channels and delivery methods configured.
     */
    @NotNull
    @Field("notifications")
    private NotificationsConfig notifications;

    /**
     * Priority level for this event type affecting notification delivery order and urgency.
     * Values: HIGH, MEDIUM, LOW
     * HIGH priority events are processed first and may use expedited delivery channels.
     */
    @NotNull
    @Field("priority")
    @Builder.Default
    private EventPriority priority = EventPriority.MEDIUM;

    /**
     * Indicates whether this event configuration is currently active.
     * Inactive configurations are preserved for audit purposes but won't trigger notifications.
     * Used for soft delete functionality to maintain data integrity.
     */
    @Field("isActive")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Nested configuration class for organizing notification settings by recipient type.
     *
     * This embedded class groups notification settings for different types of data stakeholders.
     * Each recipient type can have independent notification preferences, channels, and methods.
     *
     * Recipient Types:
     * - Data Principal: Individual whose data is being processed (end user)
     * - Data Fiduciary: Organization determining data processing purposes (business)
     * - Data Processor: Third-party processing data on behalf of fiduciary (vendor)
     * - Data Protection Officer: DPO responsible for handling privacy compliance and grievances
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationsConfig {

        /**
         * Notification settings for Data Principal (the individual whose data is processed).
         * Typically includes direct notifications via SMS/Email to inform about data activities.
         */
        @Field("dataPrincipal")
        private RecipientSettings dataPrincipal;

        /**
         * Notification settings for Data Fiduciary (organization responsible for data).
         * Usually internal notifications to compliance/privacy officers about data events.
         */
        @Field("dataFiduciary")
        private RecipientSettings dataFiduciary;

        /**
         * Notification settings for Data Processor (third-party processing data).
         * Typically webhook/API callbacks to inform processors of relevant data events.
         */
        @Field("dataProcessor")
        private RecipientSettings dataProcessor;

        /**
         * Notification settings for Data Protection Officer (DPO).
         * Email notifications sent to DPO for grievance-related events and compliance matters.
         * DPO email address is fetched from dpo_configurations collection at runtime.
         * Typically enabled for all grievance events (GRIEVANCE_RAISED, GRIEVANCE_RESOLVED, etc.).
         */
        @Field("dataProtectionOfficer")
        private RecipientSettings dataProtectionOfficer;
    }

    /**
     * Configuration settings for a specific recipient type's notifications.
     *
     * This class defines how notifications should be delivered to a particular recipient type,
     * including which channels to use and what delivery methods to employ.
     *
     * Channel Types:
     * - SMS: Text message notifications
     * - EMAIL: Email notifications
     * - PUSH: Push notifications (future)
     *
     * Method Types:
     * - DIRECT: Direct delivery to recipient
     * - CALLBACK: Webhook/API callback
     * - QUEUE: Asynchronous queue processing
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipientSettings {

        /**
         * Whether notifications are enabled for this recipient type.
         * When false, no notifications will be sent regardless of channel configuration.
         */
        @Field("enabled")
        @Builder.Default
        private Boolean enabled = false;

        /**
         * List of notification channels to use for this recipient.
         * Examples: ["SMS", "EMAIL"], ["EMAIL"], ["SMS"]
         * Each channel will attempt delivery independently.
         */
        @Field("channels")
        private List<String> channels;

        /**
         * Delivery method for notifications.
         * Examples: "DIRECT" for immediate delivery, "CALLBACK" for webhooks,
         * "QUEUE" for asynchronous processing.
         */
        @Field("method")
        private String method;
    }
}