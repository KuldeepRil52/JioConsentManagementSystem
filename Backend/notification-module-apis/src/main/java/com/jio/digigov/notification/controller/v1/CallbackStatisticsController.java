package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.notification.CallbackStatsFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.notification.CallbackStatsResponseDto;
import com.jio.digigov.notification.service.callback.CallbackStatisticsService;
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
 * REST controller for callback notification statistics endpoints.
 *
 * Provides tenant-level statistics for callback notifications with comprehensive
 * filtering capabilities and breakdown by Data Fiduciary and Data Processor.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/notifications/callbacks")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Callback Statistics",
     description = "Tenant-level callback notification statistics with Data Fiduciary and Data Processor breakdowns")
public class CallbackStatisticsController extends BaseController {

    private final CallbackStatisticsService callbackStatisticsService;

    /**
     * Get comprehensive callback notification statistics.
     *
     * Returns statistics including:
     * - Global statistics (total, success, failure counts and percentages)
     * - Data Fiduciary breakdown with organization names from business_applications
     * - Data Processor breakdown with processor names from data_processors
     * - Event-type-specific statistics at each level
     *
     * Success statuses: SENT, RETRIEVED, ACKNOWLEDGED, PROCESSING, PROCESSED, DELETED
     * Failure statuses: FAILED, PENDING, RETRY_SCHEDULED
     *
     * All filters are optional. Without filters, returns statistics for all callbacks in the tenant.
     * Filters are applied with AND logic (all must match).
     *
     * @param request HTTP servlet request
     * @param eventType Optional filter by event type (e.g., CONSENT_CREATED, DATA_BREACH)
     * @param recipientType Optional filter by recipient type (DATA_FIDUCIARY or DATA_PROCESSOR only)
     * @param recipientId Optional filter by specific recipient ID (businessId for DF, dataProcessorId for DP)
     * @param fromDate Optional start date for filtering (inclusive, based on created_at field)
     * @param toDate Optional end date for filtering (exclusive, based on created_at field)
     * @return StandardApiResponseDto containing CallbackStatsResponseDto
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get callback notification statistics",
            description = "Retrieves comprehensive callback notification statistics with breakdown by Data Fiduciary " +
                    "and Data Processor, including event-type-specific metrics. " +
                    "\n\n**Success Statuses:** SENT, RETRIEVED, ACKNOWLEDGED, PROCESSING, PROCESSED, DELETED" +
                    "\n\n**Failure Statuses:** FAILED, PENDING, RETRY_SCHEDULED" +
                    "\n\n**Filters:** All filters are optional and applied with AND logic. " +
                    "Without filters, returns statistics for all callbacks in the tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Callback statistics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CallbackStatsResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters - Check filter values and date ranges",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - X-Tenant-Id header is required",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Failed to retrieve statistics",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<StandardApiResponseDto<CallbackStatsResponseDto>> getCallbackStatistics(
            HttpServletRequest request,

            @Parameter(
                    description = "Filter by event type (e.g., CONSENT_CREATED, DATA_BREACH, GRIEVANCE_RAISED). " +
                            "Accepts any event type string value from the database without validation.",
                    example = "CONSENT_CREATED"
            )
            @RequestParam(required = false)
            String eventType,

            @Parameter(
                    description = "Filter by recipient type. Only DATA_FIDUCIARY and DATA_PROCESSOR are allowed for callback statistics.",
                    example = "DATA_FIDUCIARY",
                    schema = @Schema(allowableValues = {"DATA_FIDUCIARY", "DATA_PROCESSOR"})
            )
            @RequestParam(required = false)
            String recipientType,

            @Parameter(
                    description = "Filter by specific recipient ID. " +
                            "For DATA_FIDUCIARY: use businessId. " +
                            "For DATA_PROCESSOR: use dataProcessorId.",
                    example = "DF_12345"
            )
            @RequestParam(required = false)
            String recipientId,

            @Parameter(
                    description = "Start date for filtering callbacks (inclusive). Filters based on the created_at timestamp. " +
                            "Must be in ISO-8601 format.",
                    example = "2025-01-01T00:00:00"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @Parameter(
                    description = "End date for filtering callbacks (exclusive). Filters based on the created_at timestamp. " +
                            "Must be in ISO-8601 format and after fromDate.",
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        // Extract tenant ID from required X-Tenant-Id header
        String tenantId = extractTenantId(request);

        log.info("Getting callback statistics for tenant: {}, eventType: {}, recipientType: {}, " +
                        "recipientId: {}, fromDate: {}, toDate: {}",
                tenantId, eventType, recipientType, recipientId, fromDate, toDate);

        // Build filter request
        CallbackStatsFilterRequestDto filter = CallbackStatsFilterRequestDto.builder()
                .eventType(eventType)
                .recipientType(recipientType)
                .recipientId(recipientId)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        // Get statistics from service
        CallbackStatsResponseDto stats = callbackStatisticsService.getCallbackStatistics(tenantId, filter);

        // Build and return success response
        return buildStatsSuccessResponse(request, stats, "Callback statistics retrieved successfully");
    }

    /**
     * Builds a success response for callback statistics.
     *
     * @param request HTTP servlet request
     * @param data Callback statistics data
     * @param message Success message
     * @return ResponseEntity with StandardApiResponseDto
     */
    private ResponseEntity<StandardApiResponseDto<CallbackStatsResponseDto>> buildStatsSuccessResponse(
            HttpServletRequest request, CallbackStatsResponseDto data, String message) {

        String correlationId = extractCorrelationId(request);
        StandardApiResponseDto<CallbackStatsResponseDto> response = StandardApiResponseDto.success(
                        data, message
                ).withTransactionId(correlationId)
                .withPath(request.getRequestURI());

        return ResponseEntity.ok(response);
    }
}
