package com.jio.digigov.notification.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thread-local context for passing event metadata during notification processing.
 *
 * This class provides a thread-safe way to pass event-related information from
 * Kafka consumers to downstream services (like ValueGenerator) without explicitly
 * passing parameters through every method call.
 *
 * THREAD MANAGEMENT JUSTIFICATION (J2EE Environment):
 * This class intentionally uses ThreadLocal for event context management in a
 * Kafka consumer environment. This is appropriate because:
 *
 * 1. Message Isolation: Each Kafka message is processed in a separate thread from
 *    the consumer thread pool, ensuring event context is isolated between messages.
 *
 * 2. Context Propagation: Allows event metadata to flow through the call stack
 *    (consumer → service → master list resolver) without explicit parameter passing.
 *
 * 3. Async Processing: Supports asynchronous notification processing where context
 *    needs to be preserved across service boundaries.
 *
 * Usage Pattern:
 * 1. Consumer sets context at the beginning of message processing
 * 2. ValueGenerator accesses context during OTP generation
 * 3. Consumer clears context at the end of processing (in finally block)
 *
 * Thread Safety:
 * - Uses ThreadLocal for isolation between concurrent message processing
 * - Each thread has its own independent context instance
 * - Must be explicitly cleared to prevent memory leaks
 *
 * Example Usage:
 * <pre>
 * try {
 *     EventContext.setContext(EventContext.builder()
 *         .eventId(eventMessage.getEventId())
 *         .txnId(eventMessage.getTransactionId())
 *         .correlationId(eventMessage.getCorrelationId())
 *         .tenantId(eventMessage.getTenantId())
 *         .businessId(eventMessage.getBusinessId())
 *         .eventType(eventMessage.getEventType())
 *         .notificationId(notificationSms.getNotificationId())
 *         .recipientValue(eventMessage.getCustomerIdentifiers().getValue())
 *         .recipientType(eventMessage.getCustomerIdentifiers().getType())
 *         .build());
 *
 *     // Process notification (master list resolution happens here)
 *     masterListService.resolveArguments(...);
 *
 * } finally {
 *     EventContext.clearContext();
 * }
 * </pre>
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventContext {

    /**
     * ThreadLocal holder for event context - Intentional use for message-scoped context isolation
     * in Kafka consumer threads. MUST be cleared after each message processing.
     */
    private static final ThreadLocal<EventContext> contextHolder = new ThreadLocal<>();

    /**
     * Event identifier from the triggering event.
     * Used to link OTP record back to the original notification event.
     */
    private String eventId;

    /**
     * Transaction identifier for OTP verification.
     * Acts as the security token for OTP verification API.
     */
    private String txnId;

    /**
     * Correlation identifier for linking related notifications.
     * Used for audit trail and tracking.
     */
    private String correlationId;

    /**
     * Tenant identifier for multi-tenant database routing.
     * Special value "SYSTEM" routes to shared database.
     */
    private String tenantId;

    /**
     * Business identifier for configuration and authorization.
     */
    private String businessId;

    /**
     * Event type that triggered the notification.
     * E.g., "INIT_OTP", "CONSENT_GRANTED", etc.
     */
    private String eventType;

    /**
     * Notification identifier (SMS or Email notification ID).
     * Links OTP record to the actual notification delivery.
     */
    private String notificationId;

    /**
     * Recipient value (mobile number or email address).
     * Used for OTP record recipient information.
     */
    private String recipientValue;

    /**
     * Recipient type (MOBILE or EMAIL).
     * Determines the channel for OTP delivery.
     */
    private String recipientType;

    /**
     * Sets the event context for the current thread.
     * Should be called at the beginning of event processing.
     *
     * @param context The event context to set
     */
    public static void setContext(EventContext context) {
        contextHolder.set(context);
    }

    /**
     * Gets the event context for the current thread.
     *
     * @return The current event context, or null if not set
     */
    public static EventContext getContext() {
        return contextHolder.get();
    }

    /**
     * Clears the event context for the current thread.
     * MUST be called in a finally block to prevent memory leaks.
     */
    public static void clearContext() {
        contextHolder.remove();
    }

    /**
     * Checks if a context is currently set for this thread.
     *
     * @return true if context is set, false otherwise
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
}
