package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.notification.ConsentDeletionFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDashboardResponseDto;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDetailsResponseDto;
import com.jio.digigov.notification.service.callback.ConsentDeletionDashboardService;
import com.jio.digigov.notification.service.callback.ConsentDeletionDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * REST Controller for Consent Deletion Dashboard operations.
 *
 * Provides endpoints for retrieving consent deletion dashboard data including
 * overview metrics and paginated list of consent deletion requests for
 * CONSENT_EXPIRED and CONSENT_WITHDRAWN events.
 *
 * Overview Metrics:
 * - Deletion Requests: Count of unique consentIds
 * - Completed: Consents where ALL recipients (DF + all DPs) have DELETED status
 * - Deferred: Consents where ANY recipient has DEFERRED status
 * - In Progress: Deletion Requests - Completed - Deferred
 *
 * All endpoints are tenant-aware and require proper tenant identification
 * in the request headers (X-Tenant-ID and X-Business-ID).
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/notifications/callbacks")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Consent Deletion Dashboard",
     description = "APIs for consent deletion dashboard with overview metrics and request tracking")
public class ConsentDeletionDashboardController extends BaseController {

    private final ConsentDeletionDashboardService consentDeletionDashboardService;
    private final ConsentDeletionDetailsService consentDeletionDetailsService;

    /**
     * Retrieves the consent deletion dashboard with overview metrics and paginated list.
     *
     * Returns comprehensive consent deletion data including:
     * - Overview metrics: total deletion requests, completed, deferred, in-progress
     * - Paginated list of consent deletion items with status breakdown
     *
     * The dashboard groups data by unique consentId, showing the latest event
     * for each consent when multiple events exist.
     *
     * @param request HTTP servlet request for tenant and business ID extraction
     * @param eventType Optional filter by event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN)
     * @param overallStatus Optional filter by overall status (DONE, PARTIAL, DEFERRED)
     * @param processorId Optional filter by specific Data Processor recipient ID
     * @param consentId Optional filter by specific consent ID
     * @param dataPrincipal Optional filter by data principal (customer identifier value)
     * @param fromDate Optional start date for filtering (inclusive)
     * @param toDate Optional end date for filtering (exclusive)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return ResponseEntity containing consent deletion dashboard data
     */
    @GetMapping("/consent-deletion")
    @Operation(
            summary = "Get consent deletion dashboard",
            description = "Retrieves consent deletion dashboard with overview metrics (deletion requests, " +
                    "completed, deferred, in-progress) and a paginated list of consent deletion requests. " +
                    "Data is grouped by unique consentId, showing the latest event for each consent. " +
                    "Supports filtering by event type, overall status, processor, consent ID, and date range."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved consent deletion dashboard",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConsentDeletionDashboardResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters or filter validation failed",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<ConsentDeletionDashboardResponseDto>> getConsentDeletionDashboard(
            HttpServletRequest request,

            @Parameter(
                    description = "Filter by event type (Trigger). " +
                            "If not provided, includes both CONSENT_EXPIRED and CONSENT_WITHDRAWN.",
                    example = "CONSENT_EXPIRED"
            )
            @RequestParam(required = false) String eventType,

            @Parameter(
                    description = "Filter by overall status (DONE, PARTIAL, PENDING, DEFERRED). " +
                            "If not provided, includes all statuses.",
                    example = "DONE"
            )
            @RequestParam(required = false) String overallStatus,

            @Parameter(
                    description = "Filter by specific Data Processor recipient ID. " +
                            "Filters consents that have callbacks for the specified processor.",
                    example = "DP_12345"
            )
            @RequestParam(required = false) String processorId,

            @Parameter(
                    description = "Filter by specific consent ID.",
                    example = "CONSENT_123"
            )
            @RequestParam(required = false) String consentId,

            @Parameter(
                    description = "Filter by data principal (customer identifier value).",
                    example = "9876543210"
            )
            @RequestParam(required = false) String dataPrincipal,

            @Parameter(
                    description = "Start date for filtering (inclusive). " +
                            "Format: yyyy-MM-dd'T'HH:mm:ss",
                    example = "2025-01-01T00:00:00"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @Parameter(
                    description = "End date for filtering (exclusive). " +
                            "Format: yyyy-MM-dd'T'HH:mm:ss",
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @Parameter(
                    description = "Page number (0-indexed)",
                    example = "0"
            )
            @RequestParam(required = false, defaultValue = "0")
            @Min(0) Integer page,

            @Parameter(
                    description = "Page size (max 100)",
                    example = "20"
            )
            @RequestParam(required = false, defaultValue = "20")
            @Min(1) @Max(100) Integer size
    ) {
        // Extract tenant ID and business ID from required headers
        String tenantId = extractTenantId(request);
        String businessId = extractBusinessId(request);

        log.info("Getting consent deletion dashboard for tenant: {}, businessId: {}, eventType: {}, " +
                        "overallStatus: {}, processorId: {}, consentId: {}, dataPrincipal: {}, " +
                        "fromDate: {}, toDate: {}, page: {}, size: {}",
                tenantId, businessId, eventType, overallStatus, processorId, consentId,
                dataPrincipal, fromDate, toDate, page, size);

        // Build filter request DTO
        ConsentDeletionFilterRequestDto filter = ConsentDeletionFilterRequestDto.builder()
                .eventType(eventType)
                .overallStatus(overallStatus)
                .processorId(processorId)
                .consentId(consentId)
                .dataPrincipal(dataPrincipal)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .build();

        // Get consent deletion dashboard from service
        ConsentDeletionDashboardResponseDto dashboard = consentDeletionDashboardService
                .getConsentDeletionDashboard(tenantId, businessId, filter);

        // Build and return success response
        return buildDashboardSuccessResponse(request, dashboard,
                "Consent deletion dashboard retrieved successfully");
    }

    /**
     * Builds a success response for consent deletion dashboard.
     *
     * @param request HTTP servlet request
     * @param data Dashboard data
     * @param message Success message
     * @return ResponseEntity with StandardApiResponseDto
     */
    private ResponseEntity<StandardApiResponseDto<ConsentDeletionDashboardResponseDto>>
            buildDashboardSuccessResponse(
            HttpServletRequest request, ConsentDeletionDashboardResponseDto data, String message) {

        String correlationId = extractCorrelationId(request);
        StandardApiResponseDto<ConsentDeletionDashboardResponseDto> response =
                StandardApiResponseDto.success(data, message)
                        .withTransactionId(correlationId)
                        .withPath(request.getRequestURI());

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detailed consent deletion information for a specific event.
     *
     * Aggregates data from multiple sources:
     * - notification_events: event details and withdrawal_data
     * - consents: template name and timestamp
     * - retention_config: data retention policy
     * - notification_callback: DP callback statuses
     * - data_processors: DP names
     *
     * @param request HTTP servlet request for tenant and business ID extraction
     * @param eventId Event identifier for which to retrieve details
     * @return ResponseEntity containing detailed consent deletion information
     */
    @GetMapping("/consent-deletion-details/{eventId}")
    @Operation(
            summary = "Get consent deletion details",
            description = "Retrieves detailed consent deletion information for a specific event. " +
                    "Includes consent information, data fiduciary status, data processor statuses, " +
                    "PII items, and complete withdrawal data."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved consent deletion details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConsentDeletionDetailsResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Event not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<ConsentDeletionDetailsResponseDto>> getConsentDeletionDetails(
            HttpServletRequest request,

            @Parameter(
                    description = "Event identifier for which to retrieve consent deletion details",
                    required = true,
                    example = "EVT_12345"
            )
            @PathVariable("eventId") @NotBlank String eventId
    ) {
        // Extract tenant ID and business ID from required headers
        String tenantId = extractTenantId(request);
        String businessId = extractBusinessId(request);

        log.info("Getting consent deletion details for eventId: {}, tenant: {}, businessId: {}",
                eventId, tenantId, businessId);

        // Get consent deletion details from service
        ConsentDeletionDetailsResponseDto details = consentDeletionDetailsService
                .getConsentDeletionDetails(eventId, tenantId, businessId);

        // Build and return success response
        return buildDetailsSuccessResponse(request, details,
                "Consent deletion details retrieved successfully");
    }

    /**
     * Builds a success response for consent deletion details.
     *
     * @param request HTTP servlet request
     * @param data Details data
     * @param message Success message
     * @return ResponseEntity with StandardApiResponseDto
     */
    private ResponseEntity<StandardApiResponseDto<ConsentDeletionDetailsResponseDto>>
            buildDetailsSuccessResponse(
            HttpServletRequest request, ConsentDeletionDetailsResponseDto data, String message) {

        String correlationId = extractCorrelationId(request);
        StandardApiResponseDto<ConsentDeletionDetailsResponseDto> response =
                StandardApiResponseDto.success(data, message)
                        .withTransactionId(correlationId)
                        .withPath(request.getRequestURI());

        return ResponseEntity.ok(response);
    }
}
