package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.dto.response.event.TriggerEventResponseDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.event.NotificationEventResponseDto;
import com.jio.digigov.notification.service.event.EventTriggerService;
import com.jio.digigov.notification.service.event.NotificationEventService;
import com.jio.digigov.notification.service.kafka.AsyncNotificationProducerService;
import com.jio.digigov.notification.dto.response.kafka.AsyncNotificationResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Event Trigger APIs.
 * 
 * This controller provides endpoints for managing notification events in a multi-tenant environment.
 * It handles event triggering, retrieval, and analytics operations while ensuring proper tenant
 * isolation and business context validation.
 * 
 * Key Features:
 * - Event triggering with multi-channel notification delivery (SMS/Email)
 * - Comprehensive event retrieval with filtering and pagination
 * - Event analytics and counting operations
 * - Multi-tenant support with tenant and business validation
 * - Transaction tracking and audit logging
 * 
 * Security:
 * - Requires X-Tenant-Id header for all operations
 * - X-Business-Id header optional for event listing (required for other operations)
 * - Optional X-Transaction-Id header for request tracing
 * - Input validation using Jakarta Bean Validation
 * 
 * Error Handling:
 * - Returns appropriate HTTP status codes for different scenarios
 * - Detailed error messages for validation failures
 * - Comprehensive logging for debugging and monitoring
 * 
 * Base URL: /v1/events
 */
@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Event Triggers", description = "Event triggering and notification delivery APIs. Initiate notification events with multi-channel delivery support and comprehensive status tracking.")
public class EventController extends BaseController {

    private final EventTriggerService eventTriggerService;
    private final NotificationEventService notificationEventService;
    private final AsyncNotificationProducerService asyncNotificationProducerService;

    /**
     * Triggers an event and processes associated notifications.
     *
     * This is the primary endpoint for event processing in the notification system.
     * It supports both synchronous and asynchronous processing modes for optimal
     * performance and scalability.
     *
     * Processing Modes:
     *
     * Synchronous Mode (async=false, default):
     * 1. Validates the event configuration exists and is active for the business
     * 2. Resolves notification templates for all required languages and recipients
     * 3. Substitutes argument placeholders with provided dynamic values
     * 4. Delivers notifications via configured channels (SMS/Email) immediately
     * 5. Tracks event execution and notification delivery statuses
     * 6. Returns comprehensive response with delivery details
     *
     * Asynchronous Mode (async=true):
     * 1. Validates the event configuration exists and is active for the business
     * 2. Creates notification records in tenant-specific database
     * 3. Publishes messages to Kafka topics for async processing
     * 4. Returns immediately with tracking IDs for status monitoring
     * 5. Provides 10x improved throughput and sub-100ms response times
     * 6. Enables decoupled processing with retry and DLQ handling
     *
     * Supported Recipients:
     * - Data Principal: Individual whose data is being processed
     * - Data Fiduciary: Organization responsible for data protection
     * - Data Processor: Third-party entities processing the data
     *
     * @param tenantId The unique identifier for the tenant (required header)
     * @param businessId The unique identifier for the business unit (required header)
     * @param transactionId Optional transaction ID for request correlation and tracing
     * @param request The event trigger request containing event details and arguments
     * @return ResponseEntity containing response with delivery status or tracking information
     * @throws BadRequestException if event configuration is not found or inactive
     * @throws ValidationException if request validation fails
     * @throws InternalServerException if notification delivery fails
     */
    @PostMapping("/trigger")
    @Operation(
        summary = "Trigger notification event",
        description = "Triggers a notification event with multi-channel delivery support. " +
                     "Supports both synchronous and asynchronous processing modes for optimal performance and scalability. " +
                     "Validates event configuration, resolves templates, substitutes arguments, and delivers notifications via SMS/Email/Callback channels."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "Event accepted for asynchronous processing",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AsyncNotificationResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or event configuration not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid tenant or business headers",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during event processing",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<AsyncNotificationResponseDto>> triggerEvent(
            HttpServletRequest httpRequest,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(description = "Event trigger request containing event details and dynamic arguments", required = true)
            @Valid @RequestBody TriggerEventRequestDto request) {

        log.info("Triggering event: {} for tenant: {}, business: {}, transactionId: {}",
                request.getEventType(), tenantId, businessId, transactionId);

        // Check processing is requested
        log.info("Processing event asynchronously via Kafka: eventType={}, tenantId={}",
                request.getEventType(), tenantId);

        String correlationId = extractCorrelationId(httpRequest);

        try {
            CompletableFuture<AsyncNotificationResponseDto> futureResponse = asyncNotificationProducerService.processEventAsync(
                    request, tenantId, businessId, transactionId, httpRequest);

            AsyncNotificationResponseDto asyncResponse = futureResponse.get();

            StandardApiResponseDto<AsyncNotificationResponseDto> response = StandardApiResponseDto.success(
                asyncResponse,
                "Event accepted for asynchronous processing"
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            log.error("Failed to process async event: {}", e.getMessage());

            AsyncNotificationResponseDto failureResponse = AsyncNotificationResponseDto.failure(
                "UNKNOWN", transactionId, "Failed to process event: " + e.getMessage());

            StandardApiResponseDto<AsyncNotificationResponseDto> response = StandardApiResponseDto.success(
                failureResponse,
                "Event processing failed"
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves all triggered events with comprehensive filtering and pagination.
     *
     * This endpoint provides a flexible way to query historical event data with
     * support for multiple filter criteria and pagination for performance.
     *
     * Optional Notification Details:
     * - notifications=true: Include full SMS, Email, and Callback notification details
     * - Uses optimized MongoDB aggregation with single query (no N+1 problem)
     * - Returns all notifications per event (no artificial limits)
     *
     * Supported Event Filters:
     * - eventType: Filter by specific event type (e.g., "CONSENT_GRANTED")
     * - resource: Filter by resource identifier
     * - source: Filter by event source system
     * - status: Filter by event processing status
     * - priority: Filter by priority level (HIGH, MEDIUM, LOW)
     * - language: Filter by language code
     * - dataProcessorId: Filter by data processor ID
     * - fromDate/toDate: Filter by date range (ISO 8601 format)
     *
     * Supported Customer Filters:
     * - mobile: Filter by customer mobile number
     * - email: Filter by customer email address
     *
     * Supported Notification Filters:
     * - notificationStatus: Filter notifications by status (PENDING, SENT, DELIVERED, FAILED)
     * - notificationChannel: Filter notifications by channel (SMS, EMAIL, CALLBACK)
     *
     * Pagination:
     * - page: 1-based page number (default: 1, min: 1)
     * - pageSize: Number of items per page (default: 20, min: 1, max: 100)
     * - sort: Sorting criteria (e.g., "createdAt,desc")
     *
     * @param tenantId The unique identifier for the tenant (required header)
     * @param businessId The unique identifier for the business unit (optional header)
     * @param eventType Optional filter by event type
     * @param resource Optional filter by resource identifier
     * @param source Optional filter by event source
     * @param status Optional filter by processing status
     * @param fromDate Optional start date filter (ISO 8601 format)
     * @param toDate Optional end date filter (ISO 8601 format)
     * @param includeNotifications Include full notification details (default: false)
     * @param notificationStatus Optional filter for notification status
     * @param notificationChannel Optional filter for notification channel
     * @param mobile Optional filter by customer mobile number
     * @param email Optional filter by customer email address
     * @param priority Optional filter by priority level
     * @param dataProcessorId Optional filter by data processor ID
     * @param language Optional filter by language code
     * @param page Page number for pagination (1-based)
     * @param pageSize Number of items per page
     * @param sort Sorting criteria
     * @return ResponseEntity containing paginated list of NotificationEventResponseDto
     */
    @GetMapping
    @Operation(
        summary = "Get all triggered events",
        description = "Retrieves all triggered events with comprehensive filtering and pagination support. " +
                     "Provides flexible querying capabilities for historical event data with tenant and business isolation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Events retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter parameters or pagination values",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid tenant or business headers",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<PagedResponseDto<NotificationEventResponseDto>> getAllEvents(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_BUSINESS_ID, required = false) String businessId,

            @Parameter(description = "Filter by specific event type", required = false, example = "CONSENT_GRANTED")
            @RequestParam(value = "eventType", required = false) String eventType,

            @Parameter(description = "Filter by resource identifier", required = false, example = "user_123")
            @RequestParam(value = "resource", required = false) String resource,

            @Parameter(description = "Filter by event source system", required = false, example = "consent-manager")
            @RequestParam(value = "source", required = false) String source,

            @Parameter(description = "Filter by event processing status", required = false, example = "SUCCESS", schema = @Schema(allowableValues = {"SUCCESS", "FAILED", "PENDING"}))
            @RequestParam(value = "status", required = false) String status,

            @Parameter(description = "Start date filter (ISO 8601 format)", required = false, example = "2024-01-01T00:00:00")
            @RequestParam(value = "fromDate", required = false) String fromDate,

            @Parameter(description = "End date filter (ISO 8601 format)", required = false, example = "2024-12-31T23:59:59")
            @RequestParam(value = "toDate", required = false) String toDate,

            @Parameter(description = "Include full notification details (SMS, Email, Callback)", required = false, example = "true")
            @RequestParam(value = "notifications", required = false, defaultValue = "false") Boolean includeNotifications,

            @Parameter(description = "Filter notifications by status", required = false, example = "DELIVERED", schema = @Schema(allowableValues = {"PENDING", "SENT", "DELIVERED", "FAILED", "BOUNCED"}))
            @RequestParam(value = "notificationStatus", required = false) String notificationStatus,

            @Parameter(description = "Filter notifications by channel", required = false, example = "SMS", schema = @Schema(allowableValues = {"SMS", "EMAIL", "CALLBACK"}))
            @RequestParam(value = "notificationChannel", required = false) String notificationChannel,

            @Parameter(description = "Filter by customer mobile number", required = false, example = "+919876543210")
            @RequestParam(value = "mobile", required = false) String mobile,

            @Parameter(description = "Filter by customer email address", required = false, example = "user@example.com")
            @RequestParam(value = "email", required = false) String email,

            @Parameter(description = "Filter by priority level", required = false, example = "HIGH", schema = @Schema(allowableValues = {"HIGH", "MEDIUM", "LOW"}))
            @RequestParam(value = "priority", required = false) String priority,

            @Parameter(description = "Filter by data processor ID", required = false, example = "processor_123")
            @RequestParam(value = "dataProcessorId", required = false) String dataProcessorId,

            @Parameter(description = "Filter by language code", required = false, example = "en")
            @RequestParam(value = "language", required = false) String language,

            @Parameter(description = "Page number (0-based)", required = false, example = "0")
            @RequestParam(value = "page", defaultValue = "0") @Min(0) Integer page,

            @Parameter(description = "Number of items per page", required = false, example = "20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) Integer size,

            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')", required = false, example = "createdAt,desc")
            @RequestParam(value = "sort", required = false) String sort) {

        log.info("Retrieving events for tenant: {}, business: {}, page: {}, size: {}, includeNotifications: {}",
                tenantId, businessId, page, size, includeNotifications);

        PagedResponseDto<NotificationEventResponseDto> response = notificationEventService.getAllEvents(
                tenantId, businessId, eventType, resource, source, status, fromDate, toDate,
                includeNotifications, notificationStatus, notificationChannel, mobile, email,
                priority, dataProcessorId, language, page, size, sort);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific event by its unique identifier.
     * 
     * This endpoint fetches detailed information about a single notification event,
     * including its processing status, delivery details, and associated metadata.
     * The event must belong to the specified tenant and business for security.
     * 
     * @param tenantId The unique identifier for the tenant (required header)
     * @param businessId The unique identifier for the business unit (required header)
     * @param eventId The unique identifier of the event to retrieve
     * @return ResponseEntity containing NotificationEventResponseDto with event details
     * @throws NotFoundException if event is not found for the given tenant/business
     * @throws ForbiddenException if event belongs to a different tenant/business
     */
    @GetMapping("/{eventId}")
    @Operation(
        summary = "Get event by ID",
        description = "Retrieves detailed information about a specific notification event by its unique identifier. " +
                     "Returns comprehensive event details including processing status, delivery information, and metadata."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Event retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationEventResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Event not found for the specified tenant/business",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid event ID or missing required headers",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid tenant or business headers",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<NotificationEventResponseDto> getEventById(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(description = "Unique event identifier", required = true, example = "evt_20240120_001")
            @PathVariable("eventId") @NotBlank(message = "Event ID is required") String eventId) {
        
        log.info("Retrieving event: {} for tenant: {}, business: {}", eventId, tenantId, businessId);

        NotificationEventResponseDto response = notificationEventService.getEventById(
                eventId, tenantId, businessId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves event count statistics with comprehensive breakdown.
     * 
     * This endpoint provides analytics and counting information for events
     * with the same filtering capabilities as the list endpoint. It returns
     * both total counts and breakdown by various dimensions.
     * 
     * Count Breakdown Includes:
     * - Total event count matching filters
     * - Count by event status (SUCCESS, FAILED, PENDING)
     * - Count by event type
     * - Count by notification channels
     * 
     * Supported Filters (same as list endpoint):
     * - eventType, resource, source, status, fromDate, toDate
     * 
     * @param tenantId The unique identifier for the tenant (required header)
     * @param businessId The unique identifier for the business unit (required header)
     * @param eventType Optional filter by event type
     * @param resource Optional filter by resource identifier
     * @param source Optional filter by event source
     * @param status Optional filter by processing status
     * @param fromDate Optional start date filter (ISO 8601 format)
     * @param toDate Optional end date filter (ISO 8601 format)
     * @return ResponseEntity containing CountResponseDto with detailed statistics
     */
    @GetMapping("/count")
    @Operation(
        summary = "Get event count statistics",
        description = "Returns event count statistics with comprehensive breakdown by status, type, and channels. " +
                     "Provides analytics and counting information with the same filtering capabilities as the list endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Event count statistics retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter parameters",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid tenant or business headers",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<CountResponseDto> getEventCount(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(description = "Filter by specific event type", required = false, example = "CONSENT_GRANTED")
            @RequestParam(value = "eventType", required = false) String eventType,

            @Parameter(description = "Filter by resource identifier", required = false, example = "user_123")
            @RequestParam(value = "resource", required = false) String resource,

            @Parameter(description = "Filter by event source system", required = false, example = "consent-manager")
            @RequestParam(value = "source", required = false) String source,

            @Parameter(description = "Filter by processing status", required = false, example = "SUCCESS", schema = @Schema(allowableValues = {"SUCCESS", "FAILED", "PENDING"}))
            @RequestParam(value = "status", required = false) String status,

            @Parameter(description = "Start date filter (ISO 8601 format)", required = false, example = "2024-01-01T00:00:00")
            @RequestParam(value = "fromDate", required = false) String fromDate,

            @Parameter(description = "End date filter (ISO 8601 format)", required = false, example = "2024-12-31T23:59:59")
            @RequestParam(value = "toDate", required = false) String toDate) {
        
        log.info("Getting event count for tenant: {}, business: {}", tenantId, businessId);

        CountResponseDto response = notificationEventService.getEventCount(
                tenantId, businessId, eventType, resource, source, status, fromDate, toDate);
        
        return ResponseEntity.ok(response);
    }
}