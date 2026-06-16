package com.jio.digigov.notification.service.audit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for sending audit events to the central audit service.
 * All audit operations are asynchronous and fire-and-forget to prevent
 * blocking business operations.
 */
public interface AuditEventService {

    /**
     * Audit template operations (create, update, delete).
     *
     * @param templateId      the template identifier
     * @param actionType      the action type (template status or "DELETED")
     * @param tenantId        the tenant identifier
     * @param businessId      the business identifier
     * @param transactionId   the transaction identifier
     * @param request         the HTTP servlet request for IP extraction
     */
    void auditTemplateOperation(
            String templateId,
            String actionType,
            String tenantId,
            String businessId,
            String transactionId,
            HttpServletRequest request
    );

    /**
     * Audit event trigger operations from producer.
     *
     * @param eventId         the event identifier
     * @param eventStatus     the event status
     * @param tenantId        the tenant identifier
     * @param businessId      the business identifier
     * @param transactionId   the transaction identifier
     * @param request         the HTTP servlet request for IP extraction
     * @param isSystemTrigger whether this is a system-level trigger
     */
    void auditTriggerEvent(
            String eventId,
            String eventStatus,
            String tenantId,
            String businessId,
            String transactionId,
            HttpServletRequest request,
            boolean isSystemTrigger
    );

    /**
     * Audit OTP operations (init and verify).
     *
     * @param eventId         the event/transaction identifier
     * @param operation       the operation type ("INIT" or "VERIFY")
     * @param status          the operation status ("SUCCESS" or "FAILED")
     * @param tenantId        the tenant identifier
     * @param businessId      the business identifier
     * @param transactionId   the transaction identifier
     * @param request         the HTTP servlet request for IP extraction
     */
    void auditOtpOperation(
            String eventId,
            String operation,
            String status,
            String tenantId,
            String businessId,
            String transactionId,
            HttpServletRequest request
    );

    /**
     * Audit tenant onboarding operations.
     *
     * @param jobId           the onboarding job identifier
     * @param status          the onboarding status ("INITIATED", "COMPLETED", "FAILED")
     * @param tenantId        the tenant identifier
     * @param businessId      the business identifier
     * @param transactionId   the transaction identifier
     */
    void auditTenantOnboarding(
            String jobId,
            String status,
            String tenantId,
            String businessId,
            String transactionId
    );

    /**
     * Audit notification delivery attempts (for consumer).
     *
     * @param notificationId  the notification identifier
     * @param status          the delivery status
     * @param component       the consumer component ("SMS_CONSUMER", "EMAIL_CONSUMER", "CALLBACK_CONSUMER")
     * @param tenantId        the tenant identifier
     * @param businessId      the business identifier
     * @param transactionId   the transaction identifier
     * @param sourceIp        the source IP address (from EventMessage)
     * @param correlationId   the correlation identifier
     */
    void auditNotificationDelivery(
            String notificationId,
            String status,
            String component,
            String tenantId,
            String businessId,
            String transactionId,
            String sourceIp,
            String correlationId
    );
}
