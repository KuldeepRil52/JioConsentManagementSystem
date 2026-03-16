package com.jio.digigov.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Notification Audit Entity
 * Tracks all notification operations for compliance and debugging
 * Stored in tenant-specific database: tenant_db_{tenantId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification_audits")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_business_idx",
                  def = "{'tenant_id': 1, 'business_id': 1}"),
    @CompoundIndex(name = "txn_eventType_idx",
                  def = "{'txn_id': 1, 'event_type': 1}")
})
public class NotificationAudit extends BaseEntity {
    
    @Field("audit_id")
    private String auditId;
    
    @Field("tenant_id")
    private String tenantId;
    
    @Field("business_id")
    private String businessId;
    
    @Field("event_type")
    private String eventType; // TEMPLATE_CREATE, OTP_INIT, OTP_VERIFY, etc.
    
    @Field("status")
    private String status; // SUCCESS, FAILED, PENDING
    
    @Field("txn_id")
    private String txnId;
    
    @Field("template_id")
    private String templateId;
    
    @Field("operation")
    private String operation; // onboard, approve, init, verify, etc.
    
    @Field("request_data")
    private Object requestData;
    
    @Field("response_data")
    private Object responseData;
    
    @Field("error_message")
    private String errorMessage;
    
    @Field("processing_time_ms")
    private Long processingTimeMs;
}