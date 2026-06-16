package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.notification.NotificationFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.notification.NotificationCountResponseDto;
import com.jio.digigov.notification.dto.response.notification.NotificationResponseDto;
import com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto;
import com.jio.digigov.notification.constant.DefaultValues;
import com.jio.digigov.notification.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Event Notifications", description = "Comprehensive event notification management endpoints")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications with comprehensive filtering",
               description = "Retrieves paginated notifications with extensive filtering capabilities including event type, status, date ranges, and customer identifiers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<StandardApiResponseDto<PagedResponseDto<com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto>>> getAllNotifications(
        HttpServletRequest request,
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @Parameter(description = "Event ID filter") @RequestParam(required = false) String eventId,
        @Parameter(description = "Event type filter") @RequestParam(required = false) String eventType,
        @Parameter(description = "Business ID filter") @RequestParam(required = false) String businessId,
        @Parameter(description = "Status filter (PENDING, SUCCESS, FAILED)") @RequestParam(required = false) String status,
        @Parameter(description = "Notification type filter (SMS, EMAIL, CALLBACK)") @RequestParam(required = false) String notificationType,
        @Parameter(description = "Mobile number filter") @RequestParam(required = false) String mobile,
        @Parameter(description = "Email address filter") @RequestParam(required = false) String email,
        @Parameter(description = "From date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
        @Parameter(description = "To date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
        @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(defaultValue = "DESC") @Pattern(regexp = "^(ASC|DESC)$") String sortDirection,
        @Parameter(description = "Recipient type filter") @RequestParam(required = false) String recipientType,
        @Parameter(description = "Recipient ID filter") @RequestParam(required = false) String recipientId,
        @Parameter(description = "Customer identifier type") @RequestParam(required = false) String customerIdentifierType,
        @Parameter(description = "Customer identifier value") @RequestParam(required = false) String customerIdentifierValue,
        @Parameter(description = "Filter for failed notifications") @RequestParam(required = false) Boolean hasFailedNotifications,
        @Parameter(description = "Filter for pending notifications") @RequestParam(required = false) Boolean hasPendingNotifications,
        @Parameter(description = "Include notification details") @RequestParam(defaultValue = "true") boolean includeNotificationDetails,
        @Parameter(description = "Include event payload") @RequestParam(defaultValue = "false") boolean includeEventPayload) {

        String tenantId = extractTenantId(request);

        // X-Business-Id header takes first preference, then query param, then null (all businesses)
        String effectiveBusinessId = null;
        try {
            // First preference: X-Business-Id header
            effectiveBusinessId = extractBusinessId(request);
            if (effectiveBusinessId != null && !effectiveBusinessId.isEmpty()) {
                log.debug("Using X-Business-Id header for filtering: {}", effectiveBusinessId);
            } else {
                // Second preference: businessId query parameter
                effectiveBusinessId = businessId;
            }
        } catch (Exception e) {
            // Header not present, use query parameter
            effectiveBusinessId = businessId;
        }

        log.info("Getting all notifications for tenant: {}, businessId: {}, page: {}, size: {}",
                 tenantId, effectiveBusinessId, page, size);

        NotificationFilterRequestDto filterRequest = buildNotificationFilterRequest(
            page, size, eventId, eventType, effectiveBusinessId, status, notificationType, mobile, email,
            fromDate, toDate, sortBy, sortDirection, recipientType, recipientId,
            customerIdentifierType, customerIdentifierValue, hasFailedNotifications,
            hasPendingNotifications, includeNotificationDetails, includeEventPayload);

        PagedResponseDto<com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto> notificationsResponse = notificationService.getAllNotifications(tenantId, filterRequest);

        return buildSuccessResponse(request, notificationsResponse, "Notifications retrieved successfully");
    }

    @GetMapping("/count")
    @Operation(summary = "Get notification counts with detailed breakdown",
               description = "Provides comprehensive statistics about notifications with extensive filtering support - same filters as main API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification counts retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<StandardApiResponseDto<NotificationCountResponseDto>> getNotificationCount(
        HttpServletRequest request,
        @Parameter(description = "Event ID filter") @RequestParam(required = false) String eventId,
        @Parameter(description = "Event type filter") @RequestParam(required = false) String eventType,
        @Parameter(description = "Business ID filter") @RequestParam(required = false) String businessId,
        @Parameter(description = "Status filter (PENDING, SUCCESS, FAILED)") @RequestParam(required = false) String status,
        @Parameter(description = "Notification type filter (SMS, EMAIL, CALLBACK)") @RequestParam(required = false) String notificationType,
        @Parameter(description = "Mobile number filter") @RequestParam(required = false) String mobile,
        @Parameter(description = "Email address filter") @RequestParam(required = false) String email,
        @Parameter(description = "From date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
        @Parameter(description = "To date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
        @Parameter(description = "Recipient type filter") @RequestParam(required = false) String recipientType,
        @Parameter(description = "Recipient ID filter") @RequestParam(required = false) String recipientId,
        @Parameter(description = "Customer identifier type") @RequestParam(required = false) String customerIdentifierType,
        @Parameter(description = "Customer identifier value") @RequestParam(required = false) String customerIdentifierValue,
        @Parameter(description = "Filter for failed notifications") @RequestParam(required = false) Boolean hasFailedNotifications,
        @Parameter(description = "Filter for pending notifications") @RequestParam(required = false) Boolean hasPendingNotifications) {

        String tenantId = extractTenantId(request);

        // X-Business-Id header takes first preference, then query param, then null (all businesses)
        String effectiveBusinessId = null;
        try {
            // First preference: X-Business-Id header
            effectiveBusinessId = extractBusinessId(request);
            if (effectiveBusinessId != null && !effectiveBusinessId.isEmpty()) {
                log.debug("Using X-Business-Id header for filtering: {}", effectiveBusinessId);
            } else {
                // Second preference: businessId query parameter
                effectiveBusinessId = businessId;
            }
        } catch (Exception e) {
            // Header not present, use query parameter
            effectiveBusinessId = businessId;
        }

        log.info("Getting notification count for tenant: {}, businessId: {} with filters", tenantId, effectiveBusinessId);

        NotificationFilterRequestDto filterRequest = buildCountFilterRequest(
            eventId, eventType, effectiveBusinessId, status, notificationType, mobile, email,
            fromDate, toDate, recipientType, recipientId, customerIdentifierType,
            customerIdentifierValue, hasFailedNotifications, hasPendingNotifications);

        NotificationCountResponseDto countResponse = notificationService.getNotificationCount(tenantId, filterRequest);

        return buildCountSuccessResponse(request, countResponse, "Notification count retrieved successfully");
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get notification by event ID",
               description = "Retrieves complete notification information including the event and all associated SMS, Email, and Callback notifications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification found"),
        @ApiResponse(responseCode = "404", description = "Notification not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<StandardApiResponseDto<NotificationResponseDto>> getNotificationByEventId(
        HttpServletRequest request,
        @Parameter(description = "Event ID", required = true) @PathVariable @NotBlank String eventId) {

        String tenantId = extractTenantId(request);
        log.info("Getting notification by eventId: {} for tenant: {}", eventId, tenantId);

        Optional<NotificationResponseDto> notification = notificationService.getNotificationByEventId(tenantId, eventId);

        String correlationId = extractCorrelationId(request);

        if (notification.isPresent()) {
            StandardApiResponseDto<NotificationResponseDto> response = StandardApiResponseDto.success(
                    notification.get(),
                    "Notification retrieved successfully"
            ).withTransactionId(correlationId)
                    .withPath(request.getRequestURI());
            return ResponseEntity.ok(response);
        } else {
            StandardApiResponseDto<NotificationResponseDto> response = StandardApiResponseDto.<NotificationResponseDto>error(
                    "JDNM2001",
                    "Notification not found"
            ).withTransactionId(correlationId)
                    .withPath(request.getRequestURI());
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * Build notification filter request from request parameters
     */
    private NotificationFilterRequestDto buildNotificationFilterRequest(
            int page, int size, String eventId, String eventType, String businessId, String status,
            String notificationType, String mobile, String email, LocalDateTime fromDate,
            LocalDateTime toDate, String sortBy, String sortDirection, String recipientType,
            String recipientId, String customerIdentifierType, String customerIdentifierValue,
            Boolean hasFailedNotifications, Boolean hasPendingNotifications,
            boolean includeNotificationDetails, boolean includeEventPayload) {

        return NotificationFilterRequestDto.builder()
                .page(page)
                .size(size)
                .eventId(eventId)
                .eventType(eventType)
                .businessId(businessId)
                .status(status)
                .notificationType(notificationType)
                .mobile(mobile)
                .email(email)
                .fromDate(fromDate)
                .toDate(toDate)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .recipientType(recipientType)
                .recipientId(recipientId)
                .customerIdentifierType(customerIdentifierType)
                .customerIdentifierValue(customerIdentifierValue)
                .hasFailedNotifications(hasFailedNotifications)
                .hasPendingNotifications(hasPendingNotifications)
                .includeNotificationDetails(includeNotificationDetails)
                .includeEventPayload(includeEventPayload)
                .build();
    }

    /**
     * Build filter request for count queries (includes all filters except pagination/sorting/include flags)
     */
    private NotificationFilterRequestDto buildCountFilterRequest(
            String eventId, String eventType, String businessId, String status,
            String notificationType, String mobile, String email, LocalDateTime fromDate,
            LocalDateTime toDate, String recipientType, String recipientId,
            String customerIdentifierType, String customerIdentifierValue,
            Boolean hasFailedNotifications, Boolean hasPendingNotifications) {

        return NotificationFilterRequestDto.builder()
                .eventId(eventId)
                .eventType(eventType)
                .businessId(businessId)
                .status(status)
                .notificationType(notificationType)
                .mobile(mobile)
                .email(email)
                .fromDate(fromDate)
                .toDate(toDate)
                .recipientType(recipientType)
                .recipientId(recipientId)
                .customerIdentifierType(customerIdentifierType)
                .customerIdentifierValue(customerIdentifierValue)
                .hasFailedNotifications(hasFailedNotifications)
                .hasPendingNotifications(hasPendingNotifications)
                .build();
    }

    /**
     * Build success response with standard headers (generic version)
     */
    private <T> ResponseEntity<StandardApiResponseDto<PagedResponseDto<T>>> buildSuccessResponse(
            HttpServletRequest request, PagedResponseDto<T> data, String message) {

        String correlationId = extractCorrelationId(request);
        StandardApiResponseDto<PagedResponseDto<T>> response = StandardApiResponseDto.success(
                data, message
        ).withTransactionId(correlationId)
                .withPath(request.getRequestURI());

        return ResponseEntity.ok(response);
    }

    /**
     * Build count response with standard headers
     */
    private ResponseEntity<StandardApiResponseDto<NotificationCountResponseDto>> buildCountSuccessResponse(
            HttpServletRequest request, NotificationCountResponseDto data, String message) {

        String correlationId = extractCorrelationId(request);
        StandardApiResponseDto<NotificationCountResponseDto> response = StandardApiResponseDto.success(
                data, message
        ).withTransactionId(correlationId)
                .withPath(request.getRequestURI());

        return ResponseEntity.ok(response);
    }
}