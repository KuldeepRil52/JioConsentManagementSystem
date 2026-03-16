package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.event.CreateEventConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.event.EventConfigurationResponseDto;
import com.jio.digigov.notification.service.event.EventConfigurationService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * REST Controller for Event Configuration Management APIs
 *
 * Handles CRUD operations for event configurations with multi-tenant support.
 * Event configurations define notification settings for different recipients
 * (Data Principal, Data Fiduciary, Data Processor) and specify which channels
 * are used for notification delivery.
 */
@RestController
@RequestMapping("/v1/event-configurations")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Event Configuration",
        description = "Event configuration management for notification settings. " +
                "Configure recipient notification preferences and delivery channels for specific event types.")
public class EventConfigurationController extends BaseController {

    private final EventConfigurationService eventConfigurationService;

    /**
     * Create a new event configuration
     */
    @PostMapping
    @Operation(
        summary = "Create event configuration",
        description = "Creates a new event configuration defining notification settings " +
                "for different recipients. Specifies which channels (SMS, EMAIL, CALLBACK) " +
                "are enabled for Data Principal, Data Fiduciary, and Data Processor notifications."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Event configuration created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventConfigurationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Event configuration already exists for this event type",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<StandardApiResponseDto<EventConfigurationResponseDto>> createEventConfiguration(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(description = "Event configuration details", required = true)
            @Valid @RequestBody CreateEventConfigurationRequestDto request) {
        
        log.info("Creating event configuration for tenant: {}, business: {}, eventType: {}", 
                tenantId, businessId, request.getEventType());
        
        EventConfigurationResponseDto configResponse = eventConfigurationService.createEventConfiguration(
                request, tenantId, businessId, transactionId);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<EventConfigurationResponseDto> response = StandardApiResponseDto.success(
            configResponse,
            "Event configuration created successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all event configurations with filtering and pagination
     */
    @GetMapping
    @Operation(
        summary = "Get all event configurations",
        description = "Retrieves all event configurations for a business with optional filtering by active status and priority. " +
                     "Results are paginated and can be sorted by various criteria."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Event configurations retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter parameters or pagination values",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<PagedResponseDto<EventConfigurationResponseDto>>> getAllEventConfigurations(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(description = "Filter by active status", required = false, example = "true")
            @RequestParam(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "Filter by priority level", required = false, example = "HIGH", schema = @Schema(allowableValues = {"CRITICAL", "HIGH", "MEDIUM", "LOW"}))
            @RequestParam(value = "priority", required = false) String priority,

            @Parameter(description = "Page number (0-based)", required = false, example = "0")
            @RequestParam(value = "page", defaultValue = "0") @Min(0) Integer page,

            @Parameter(description = "Number of items per page", required = false, example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) Integer pageSize,

            @Parameter(description = "Sort criteria (e.g., 'eventType,asc' or 'createdAt,desc')", required = false, example = "eventType,asc")
            @RequestParam(value = "sort", required = false) String sort) {
        
        log.info("Retrieving event configurations for tenant: {}, business: {}", tenantId, businessId);
        
        PagedResponseDto<EventConfigurationResponseDto> configsResponse = eventConfigurationService.getAllEventConfigurations(
                tenantId, businessId, isActive, priority, page, pageSize, sort);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<PagedResponseDto<EventConfigurationResponseDto>> response = StandardApiResponseDto.success(
            configsResponse,
            "Event configurations retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific event configuration by event type
     */
    @GetMapping("/{eventType}")
    @Operation(
        summary = "Get event configuration by event type",
        description = "Retrieves a specific event configuration by its event type identifier. " +
                     "Returns detailed notification settings for all configured recipients."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Event configuration retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventConfigurationResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Event configuration not found for the specified event type",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied to event configuration",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<EventConfigurationResponseDto>> getEventConfigurationByEventType(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(description = "Event type identifier", required = true, example = "CONSENT_GRANTED")
            @PathVariable("eventType") @NotBlank(message = "Event type is required") String eventType) {
        
        log.info("Retrieving event configuration for eventType: {}, tenant: {}, business: {}", 
                eventType, tenantId, businessId);
        
        EventConfigurationResponseDto configResponse = eventConfigurationService.getEventConfigurationByEventType(
                businessId, eventType, tenantId);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<EventConfigurationResponseDto> response = StandardApiResponseDto.success(
            configResponse,
            "Event configuration retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing event configuration
     */
    @PutMapping("/{eventType}")
    @Operation(
        summary = "Update event configuration",
        description = "Updates an existing event configuration for the specified event type. " +
                     "All configuration settings including recipient settings and channel preferences will be replaced."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Event configuration updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventConfigurationResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or validation errors",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Event configuration not found for the specified event type",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<EventConfigurationResponseDto>> updateEventConfiguration(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(description = "Event type identifier", required = true, example = "CONSENT_GRANTED")
            @PathVariable("eventType") @NotBlank(message = "Event type is required") String eventType,

            @Parameter(description = "Updated event configuration details", required = true)
            @Valid @RequestBody CreateEventConfigurationRequestDto request) {
        
        log.info("Updating event configuration for eventType: {}, tenant: {}, business: {}", 
                eventType, tenantId, businessId);
        
        EventConfigurationResponseDto configResponse = eventConfigurationService.updateEventConfiguration(
                businessId, eventType, request, tenantId, transactionId);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<EventConfigurationResponseDto> response = StandardApiResponseDto.success(
            configResponse,
            "Event configuration updated successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete an event configuration (hard delete)
     */
    @DeleteMapping("/{eventType}")
    @Operation(
        summary = "Delete event configuration",
        description = "Permanently deletes an event configuration from the database. " +
                     "This action cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Event configuration deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Event configuration not found for the specified event type",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<Void> deleteEventConfiguration(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(description = "Event type identifier", required = true, example = "CONSENT_GRANTED")
            @PathVariable("eventType") @NotBlank(message = "Event type is required") String eventType) {

        log.info("Deleting event configuration for eventType: {}, tenant: {}, business: {}",
                eventType, tenantId, businessId);

        eventConfigurationService.deleteEventConfiguration(businessId, eventType, tenantId, transactionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get count of event configurations with breakdown
     */
    @GetMapping("/count")
    @Operation(
        summary = "Get event configuration count",
        description = "Returns count statistics for event configurations with optional filtering. " +
                     "Provides breakdown by active status, priority levels, and other categories."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Event configuration count retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter parameters",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<CountResponseDto>> getEventConfigurationCount(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,

            @Parameter(description = "Filter by active status", required = false, example = "true")
            @RequestParam(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "Filter by priority level", required = false, example = "HIGH", schema = @Schema(allowableValues = {"CRITICAL", "HIGH", "MEDIUM", "LOW"}))
            @RequestParam(value = "priority", required = false) String priority) {
        
        log.info("Getting event configuration count for tenant: {}, business: {}", tenantId, businessId);
        
        CountResponseDto countResponse = eventConfigurationService.getEventConfigurationCount(
                tenantId, businessId, isActive, priority);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<CountResponseDto> response = StandardApiResponseDto.success(
            countResponse,
            "Event configuration count retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
}