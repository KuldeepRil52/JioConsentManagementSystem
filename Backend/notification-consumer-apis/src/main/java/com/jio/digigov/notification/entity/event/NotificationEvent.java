package com.jio.digigov.notification.entity.event;

import com.jio.digigov.notification.entity.base.BaseEntity;
import com.jio.digigov.notification.enums.EventStatus;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Notification Event entity for tracking triggered events
 * Uses multi-tenant database architecture (no tenantId field, uses tenant-specific databases)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_events")
@CompoundIndexes({
    @CompoundIndex(name = "event_id_idx", 
                  def = "{'eventId': 1}", 
                  unique = true),
    @CompoundIndex(name = "business_event_type_idx", 
                  def = "{'businessId': 1, 'eventType': 1}"),
    @CompoundIndex(name = "business_status_idx", 
                  def = "{'businessId': 1, 'status': 1}")
})
public class NotificationEvent extends BaseEntity {
    
    // Event identifier (generated)
    @NotBlank(message = "Event ID is required")
    @Indexed(unique = true)
    @Field("eventId")
    private String eventId; // e.g., "EVT_20240120_001"
    
    // Business identifier
    @NotBlank(message = "Business ID is required")
    @Field("businessId")
    private String businessId;
    
    // Event type
    @NotBlank(message = "Event type is required")
    @Field("eventType")
    private String eventType; // e.g., "CONSENT_GRANTED"
    
    // Resource type
    @Field("resource")
    private String resource; // e.g., "consent", "grievance"
    
    // Source system
    @Field("source")
    private String source; // e.g., "consent-app", "grievance-portal"
    
    // Resolved language for templates
    @Field("language")
    private String language; // Resolved language (after fallback chain)
    
    // Customer identifiers
    @NotNull
    @Field("customerIdentifiers")
    private CustomerIdentifiers customerIdentifiers;
    
    // Data Processor IDs (if any)
    @Field("dataProcessorIds")
    private List<String> dataProcessorIds;
    
    // Original event payload from trigger request
    @Field("eventPayload")
    private Map<String, Object> eventPayload;
    
    // Summary of created notifications
    @Field("notificationsSummary")
    private NotificationsSummary notificationsSummary;
    
    // Event processing status
    @NotNull
    @Field("status")
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    // Embedded classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerIdentifiers {
        
        // Type of identifier (MOBILE or EMAIL)
        @Field("type")
        private String type; // "MOBILE" or "EMAIL"
        
        // Identifier value
        @Field("value")
        private String value; // Mobile number or email address
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationsSummary {
        
        // Total number of notifications created
        @Field("totalCount")
        @Builder.Default
        private Integer totalCount = 0;
        
        // SMS notification ID (if created)
        @Field("smsNotificationId")
        private String smsNotificationId;
        
        // Email notification ID (if created)
        @Field("emailNotificationId")
        private String emailNotificationId;
        
        // Callback notification IDs (for future)
        @Field("callbackNotificationIds")
        private List<String> callbackNotificationIds;
        
        // Count by type
        @Field("smsCount")
        @Builder.Default
        private Integer smsCount = 0;
        
        @Field("emailCount")
        @Builder.Default
        private Integer emailCount = 0;
        
        @Field("callbackCount")
        @Builder.Default
        private Integer callbackCount = 0;
        
        // Status counts
        @Field("completedCount")
        @Builder.Default
        private Integer completedCount = 0;
        
        @Field("failedCount")
        @Builder.Default
        private Integer failedCount = 0;
    }
}