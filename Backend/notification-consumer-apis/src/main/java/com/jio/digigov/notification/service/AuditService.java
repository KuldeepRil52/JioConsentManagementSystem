package com.jio.digigov.notification.service;

import com.jio.digigov.notification.enums.IdType;
import com.jio.digigov.notification.entity.NotificationAudit;

import java.util.List;
import java.util.Map;

/**
 * Service for tracking and logging notification operations.
 * Maintains audit trails for compliance and monitoring purposes.
 */
public interface AuditService {
    
    /**
     * Creates a comprehensive audit record with all context information
     */
    void createAudit(String txnId, String templateId, String notificationType, 
                    String eventType, IdType idType, String idValue, String status,
                    Object request, Object response, String errorMessage, Long responseTime);
    
    /**
     * Gets audit record by transaction ID
     */
    NotificationAudit getAuditByTxnId(String txnId);
    
    /**
     * Audits template operations with step-by-step tracking
     * 
     * @param templateId Template identifier
     * @param operationType Operation type (SMS/EMAIL/OTP)
     * @param step Operation step (CREATE_START, ONBOARD_SUCCESS, APPROVE_FAILED, etc.)
     * @param headers Request headers for context
     * @param payload Request or response payload
     * @param message Human-readable operation message
     */
    void auditTemplateOperation(String templateId, String operationType, String step, 
                               Map<String, String> headers, Object payload, String message);
    
    /**
     * Audits configuration operations
     * 
     * @param configId Configuration identifier
     * @param operation Operation type (CREATE, UPDATE, DELETE, TEST)
     * @param headers Request headers for context
     * @param payload Request or response payload
     * @param status Operation status (SUCCESS, FAILED)
     * @param message Operation message
     */
    void auditConfigurationOperation(String configId, String operation, 
                                   Map<String, String> headers, Object payload, 
                                   String status, String message);
    
    /**
     * Audits notification sending operations
     * 
     * @param templateId Template used for notification
     * @param recipient Notification recipient (masked)
     * @param channel Notification channel (SMS/EMAIL)
     * @param headers Request headers for context
     * @param request Notification request
     * @param response DigiGov response
     * @param status Operation status
     * @param responseTime Response time in milliseconds
     */
    void auditNotificationSending(String templateId, String recipient, String channel,
                                 Map<String, String> headers, Object request, Object response,
                                 String status, Long responseTime);
    
    /**
     * Audits token generation operations
     * 
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param networkType Network type (INTERNET/INTRANET)
     * @param headers Request headers for context
     * @param status Operation status
     * @param message Operation message
     */
    void auditTokenGeneration(String tenantId, String businessId, String networkType,
                             Map<String, String> headers, String status, String message);
    
    /**
     * Legacy audit methods (for backward compatibility)
     */
    void auditTemplateCreation(String templateId, String templateType, Object request, Object response);
    
    void auditOTPInit(String txnId, String templateId, IdType idType, String idValue, 
                     Object request, Object response, String status, Long responseTime);
    
    void auditOTPVerify(String txnId, Object request, Object response, 
                       String status, Long responseTime);
    
    /**
     * Gets all audit records for a template
     * 
     * @param templateId Template identifier
     * @return List of audit records sorted by timestamp
     */
    List<NotificationAudit> getAuditsByTemplateId(String templateId);
    
    /**
     * Gets audit records by operation type
     * 
     * @param operationType Operation type (TEMPLATE, OTP, NOTIFICATION, CONFIG)
     * @param limit Maximum number of records to return
     * @return List of audit records
     */
    List<NotificationAudit> getAuditsByOperationType(String operationType, int limit);
    
    /**
     * Gets audit records for a tenant within date range
     * 
     * @param tenantId Tenant identifier
     * @param fromDate Start date (ISO format)
     * @param toDate End date (ISO format)
     * @return List of audit records
     */
    List<NotificationAudit> getAuditsByTenantAndDateRange(String tenantId, String fromDate, String toDate);
    
    /**
     * Generates unique audit ID
     */
    String generateAuditId();
    
    /**
     * Masks sensitive information in audit payloads
     * 
     * @param payload Original payload
     * @return Payload with sensitive fields masked
     */
    Object maskSensitiveData(Object payload);
    
    /**
     * Archives old audit records (soft delete or move to archive collection)
     * 
     * @param olderThanDays Records older than this many days
     * @return Number of records archived
     */
    long archiveOldAudits(int olderThanDays);
}