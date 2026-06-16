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
                  def = "{'tenantId': 1, 'businessId': 1}"),
    @CompoundIndex(name = "txn_eventType_idx", 
                  def = "{'txnId': 1, 'eventType': 1}")
})
public class NotificationAudit extends BaseEntity {
    
    @Field("auditId")
    private String auditId;
    
    @Field("tenantId")
    private String tenantId;
    
    @Field("businessId")
    private String businessId;
    
    @Field("eventType")
    private String eventType; // TEMPLATE_CREATE, OTP_INIT, OTP_VERIFY, etc.
    
    @Field("status")
    private String status; // SUCCESS, FAILED, PENDING
    
    @Field("txnId")
    private String txnId;
    
    @Field("templateId")
    private String templateId;
    
    @Field("operation")
    private String operation; // onboard, approve, init, verify, etc.
    
    @Field("requestData")
    private Object requestData;
    
    @Field("responseData")
    private Object responseData;
    
    @Field("errorMessage")
    private String errorMessage;
    
    @Field("processingTimeMs")
    private Long processingTimeMs;
}