package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.notification.CallbackPurgeStatsFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.notification.CallbackPurgeStatsResponseDto;
import com.jio.digigov.notification.service.callback.CallbackPurgeStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST Controller for callback purge statistics operations.
 *
 * Provides endpoints for retrieving comprehensive purge statistics for
 * CONSENT_EXPIRED and CONSENT_WITHDRAWN callback notifications, including
 * SLA compliance tracking and recipient-wise breakdowns.
 *
 * Purge Categories:
 * - Purged: Callbacks that transitioned from ACKNOWLEDGED to DELETED
 * - Pending: Callbacks that are ACKNOWLEDGED but not DELETED (within SLA)
 * - Overdue: Callbacks that are ACKNOWLEDGED but not DELETED (exceeded SLA)
 *
 * All endpoints are tenant-aware and require proper tenant identification
 * in the request headers.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/notifications/callbacks")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Callback Purge Statistics",
     description = "APIs for callback purge statistics and SLA compliance tracking")
public class CallbackPurgeStatsController extends BaseController {

    private final CallbackPurgeStatsService callbackPurgeStatsService;

    /**
     * Retrieves comprehensive callback purge statistics.
     *
     * Returns statistics for CONSENT_EXPIRED and CONSENT_WITHDRAWN events, categorized as:
     * - Purged: Callbacks that have been successfully purged (ACKNOWLEDGED → DELETED)
     * - Pending: Callbacks awaiting purge (ACKNOWLEDGED, within SLA)
     * - Overdue: Callbacks that exceeded the SLA threshold
     *
     * Statistics are provided at three levels:
     * 1. Global statistics across all callbacks
     * 2. Breakdown by Data Fiduciary with complete organization data
     * 3. Breakdown by Data Processor with complete processor data
     *
     * Each level includes event-type-specific statistics (CONSENT_EXPIRED vs CONSENT_WITHDRAWN).
     *
     * @param request HTTP servlet request for tenant extraction
     * @param eventType Optional filter by event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN)
     * @param recipientType Optional filter by recipient type (DATA_FIDUCIARY or DATA_PROCESSOR)
     * @param recipientId Optional filter by specific recipient ID
     * @param fromDate Optional start date for filtering (inclusive, based on acknowledged timestamp)
     * @param toDate Optional end date for filtering (exclusive, based on acknowledged timestamp)
     * @return ResponseEntity containing comprehensive purge statistics
     */
    @GetMapping("/purge-stats")
    @Operation(
            summary = "Get callback purge statistics",
            description = "Retrieves comprehensive purge statistics for CONSENT_EXPIRED and " +
                    "CONSENT_WITHDRAWN events, including SLA compliance tracking. " +
                    "Statistics are categorized as purged, pending, or overdue. " +
                    "Includes breakdowns by Data Fiduciary and Data Processor with complete " +
                    "recipient data."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved purge statistics",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CallbackPurgeStatsResponseDto.class)
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
    public ResponseEntity<StandardApiResponseDto<CallbackPurgeStatsResponseDto>> getPurgeStatistics(
            HttpServletRequest request,

            @Parameter(
                    description = "Filter by event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN). " +
                            "If not provided, statistics include both event types.",
                    example = "CONSENT_EXPIRED"
            )
            @RequestParam(required = false) String eventType,

            @Parameter(
                    description = "Filter by recipient type (DATA_FIDUCIARY or DATA_PROCESSOR). "
                            + "If not provided, statistics include both types.",
                    example = "DATA_FIDUCIARY"
            )
            @RequestParam(required = false) String recipientType,

            @Parameter(
                    description = "Filter by specific recipient ID (business ID for Data Fiduciary"
                            + ", data processor ID for Data Processor). "
                            + "If not provided, includes all recipients.",
                    example = "DF_12345"
            )
            @RequestParam(required = false) String recipientId,

            @Parameter(
                    description = "Start date for filtering callbacks (inclusive). " +
                            "Filters based on the ACKNOWLEDGED timestamp. " +
                            "Format: yyyy-MM-dd'T'HH:mm:ss",
                    example = "2025-01-01T00:00:00"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @Parameter(
                    description = "End date for filtering callbacks (exclusive). " +
                            "Filters based on the ACKNOWLEDGED timestamp. " +
                            "Format: yyyy-MM-dd'T'HH:mm:ss",
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        // Extract tenant ID from required X-Tenant-Id header
        String tenantId = extractTenantId(request);

        log.info("Getting callback purge statistics for tenant: {}, eventType: {}, " +
                        "recipientType: {}, recipientId: {}, fromDate: {}, toDate: {}",
                tenantId, eventType, recipientType, recipientId, fromDate, toDate);

        // Build filter request DTO
        CallbackPurgeStatsFilterRequestDto filter = CallbackPurgeStatsFilterRequestDto.builder()
                .eventType(eventType)
                .recipientType(recipientType)
                .recipientId(recipientId)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        // Get purge statistics from service
        CallbackPurgeStatsResponseDto stats = callbackPurgeStatsService
                .getPurgeStatistics(tenantId, filter);

        // Build and return success response
        return buildStatsSuccessResponse(request, stats,
                "Callback purge statistics retrieved successfully");
    }

    /**
     * Builds a success response for purge statistics.
     *
     * @param request HTTP servlet request
     * @param data Purge statistics data
     * @param message Success message
     * @return ResponseEntity with StandardApiResponseDto
     */
    private ResponseEntity<StandardApiResponseDto<CallbackPurgeStatsResponseDto>>
            buildStatsSuccessResponse(
            HttpServletRequest request, CallbackPurgeStatsResponseDto data, String message) {

        String correlationId = extractCorrelationId(request);
        StandardApiResponseDto<CallbackPurgeStatsResponseDto> response =
                StandardApiResponseDto.success(data, message)
                        .withTransactionId(correlationId)
                        .withPath(request.getRequestURI());

        return ResponseEntity.ok(response);
    }
}
