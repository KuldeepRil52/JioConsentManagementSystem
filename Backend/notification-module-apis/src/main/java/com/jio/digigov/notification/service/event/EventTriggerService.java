package com.jio.digigov.notification.service.event;

import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.dto.response.event.TriggerEventResponseDto;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for Event Trigger operations.
 * 
 * This service orchestrates the complete event processing workflow from validation to
 * notification delivery. It serves as the primary entry point for triggering data
 * protection events and managing their associated notification requirements.
 * 
 * Core Responsibilities:
 * - Event configuration validation and authorization
 * - Multi-recipient notification orchestration
 * - Template resolution and argument substitution
 * - Multi-channel delivery coordination (SMS, Email, Webhooks)
 * - Event tracking and audit logging
 * - Error handling and retry mechanisms
 * 
 * Supported Event Types:
 * - Data consent events (granted, revoked, modified)
 * - Data breach notifications
 * - Data processing lifecycle events
 * - Compliance reporting events
 * 
 * Multi-tenant Architecture:
 * - Tenant-specific event configurations
 * - Business-scoped notification rules
 * - Isolated data processing per tenant
 * 
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface EventTriggerService {
    
    /**
     * Triggers an event and processes all associated notifications.
     * 
     * This is the main entry point for event processing that orchestrates the complete
     * notification workflow. The method is designed to be atomic - either all notifications
     * are processed successfully or appropriate error handling ensures data consistency.
     * 
     * Processing Workflow:
     * 1. Event Configuration Validation
     *    - Validates the event configuration exists and is active for the business
     *    - Checks tenant authorization and business association
     *    - Verifies event type is supported and configured
     * 
     * 2. Recipient Resolution
     *    - Identifies all enabled recipients (Data Principal, Fiduciary, Processor)
     *    - Resolves recipient contact information and preferences
     *    - Validates recipient availability and status
     * 
     * 3. Template Processing
     *    - Retrieves appropriate templates for each recipient type
     *    - Supports multi-language template resolution
     *    - Performs argument substitution with provided values
     * 
     * 4. Multi-Channel Delivery
     *    - Sends notifications via configured channels (SMS, Email)
     *    - Handles webhook callbacks for Data Processors
     *    - Implements retry logic for failed deliveries
     * 
     * 5. Audit and Tracking
     *    - Records event processing status and timestamps
     *    - Logs notification delivery results
     *    - Updates event status for monitoring and reporting
     * 
     * Error Handling:
     * - Configuration errors: Returns error response immediately
     * - Partial delivery failures: Continues processing, reports individual failures
     * - System errors: Implements retry mechanisms with exponential backoff
     * 
     * @param request Event trigger request containing event details, customer identifiers, and dynamic arguments
     * @param tenantId Tenant identifier for multi-tenant data isolation and access control
     * @param businessId Business identifier for business-scoped configuration lookup and authorization
     * @param transactionId Optional transaction identifier for request correlation and distributed tracing
     * @param httpRequest HTTP servlet request for IP extraction and audit logging
     * @return TriggerEventResponseDto containing processing summary, delivery status, and any error details
     * @throws EventConfigurationNotFoundException if no active configuration exists for the event type
     * @throws InvalidArgumentException if required arguments are missing or invalid
     * @throws NotificationDeliveryException if all delivery channels fail
     */
    TriggerEventResponseDto triggerEvent(TriggerEventRequestDto request,
                                    String tenantId,
                                    String businessId,
                                    String transactionId,
                                    HttpServletRequest httpRequest);
}