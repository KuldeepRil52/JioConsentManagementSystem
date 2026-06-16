package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.BulkNotificationRequestDto;
import com.jio.digigov.notification.dto.request.BulkUpdateNotificationStatusRequestDto;
import com.jio.digigov.notification.dto.request.UpdateNotificationStatusRequestDto;
import com.jio.digigov.notification.dto.response.BulkNotificationStatusUpdateResponseDto;
import com.jio.digigov.notification.dto.response.MissedNotificationResponseDto;
import com.jio.digigov.notification.dto.response.NotificationStatusUpdateResponseDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.service.MissedNotificationService;
import com.jio.digigov.notification.service.NotificationStatusUpdateService;
import com.jio.digigov.notification.service.signature.RequestResponseSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Missed Callback Notification APIs.
 *
 * This controller provides 4 streamlined endpoints for Data Fiduciary and Data Processor systems
 * to retrieve failed and scheduled callback notifications only. These APIs automatically mark
 * retrieved notifications as RETRIEVED in the database to prevent reprocessing.
 *
 * Key Features:
 * - Callback-specific API structure
 * - Only returns failed/processing/scheduled retry notifications
 * - Excludes successful/acknowledged/pending notifications
 * - Automatically marks returned notifications as RETRIEVED
 * - Enhanced filtering capabilities for callback notifications
 * - Better structure and focused functionality
 *
 * Supported Notification Type: CALLBACK only
 * Included Statuses: FAILED, PROCESSING, RETRY_SCHEDULED
 * Excluded Statuses: SUCCESSFUL, ACKNOWLEDGED, PENDING
 *
 * Security:
 * - Requires tenant-id, business-id, requestor-type headers for all operations
 * - Requires data-processor-id header when requestor-type is DATA_PROCESSOR
 * - Requires x-jws-signature header for PUT/POST operations (when signature.enabled=true)
 * - All responses include x-jws-signature header for payload integrity (when signature.enabled=true)
 * - Input validation using Jakarta Bean Validation
 * - RSA256 signature verification for request payloads
 * - RSA256 signature generation for response payloads
 *
 * Base URL: /v1/missed-notifications
 */
@RestController
@RequestMapping("/v1/missed-notifications")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Missed Notification Management", description = "APIs for retrieving failed and scheduled callback notifications for Data Processors and Data Fiduciaries")
public class MissedNotificationController extends BaseController {

    private final MissedNotificationService missedNotificationService;
    private final NotificationStatusUpdateService statusUpdateService;
    private final RequestResponseSignatureService signatureService;

    @Operation(
        summary = "Get count of missed callback notifications",
        description = "Returns count breakdown by status for failed/processing/scheduled callback notifications only. " +
                     "Provides comprehensive filtering capabilities for webhook notifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Missed callback notification count retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters or missing required headers"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid tenant or business"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/count")
    public ResponseEntity<CountResponseDto> getMissedCallbackNotificationsCount(
            HttpServletRequest request,

            @Parameter(description = "Filter by status (FAILED, PROCESSING, RETRY_SCHEDULED) - optional")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by event type - optional")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Filter by priority level (CRITICAL, HIGH, MEDIUM, LOW) - optional")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Start date for filtering (format: yyyy-MM-dd'T'HH:mm:ss) - optional")
            @RequestParam(required = false) String fromDate,
            @Parameter(description = "End date for filtering (format: yyyy-MM-dd'T'HH:mm:ss) - optional")
            @RequestParam(required = false) String toDate) {

        // Extract new headers
        String tenantId = extractTenantIdFromNewHeaders(request);
        String businessId = extractBusinessIdFromNewHeaders(request);
        String requestorType = extractRequestorType(request);
        String recipientId = extractRecipientIdFromNewHeaders(request, requestorType);
        String txn = extractTransactionId(request);

        log.info("Retrieving missed callback notifications count: tenantId={}, businessId={}, requestorType={}, recipientId={}, status={}, eventType={}, priority={}, fromDate={}, toDate={}, txn={}",
                tenantId, businessId, requestorType, recipientId, status, eventType, priority, fromDate, toDate, txn);

        LocalDateTime fromDateTime = parseDateTime(fromDate);
        LocalDateTime toDateTime = parseDateTime(toDate);

        CountResponseDto response = missedNotificationService.getMissedCallbackNotificationsCountEnhanced(
                tenantId, businessId, requestorType, recipientId, status,
                eventType, priority, fromDateTime, toDateTime);

        log.info("Retrieved missed callback notifications count: tenantId={}, businessId={}, totalCount={}, txn={}",
                tenantId, businessId, response.getData().getTotalCount(), txn);

        // Sign response
        HttpHeaders responseHeaders = addSignatureToResponse(tenantId, response);
        return ResponseEntity.ok().headers(responseHeaders).body(response);
    }

    @Operation(
        summary = "Get list of missed callback notification IDs",
        description = "Returns paginated list of callback notification IDs with basic information for efficient " +
                     "bulk monitoring and processing. Includes only failed/processing/scheduled callback notifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Missed callback notification IDs retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or missing required headers"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid tenant or business"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/list")
    public ResponseEntity<PagedResponseDto<MissedNotificationResponseDto>> listMissedCallbackNotificationIds(
            HttpServletRequest request,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Filter by status (FAILED, PROCESSING, RETRY_SCHEDULED) - optional")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by event type - optional")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Filter by priority level (CRITICAL, HIGH, MEDIUM, LOW) - optional")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Start date for filtering (format: yyyy-MM-dd'T'HH:mm:ss) - optional")
            @RequestParam(required = false) String fromDate,
            @Parameter(description = "End date for filtering (format: yyyy-MM-dd'T'HH:mm:ss) - optional")
            @RequestParam(required = false) String toDate,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        // Extract new headers
        String tenantId = extractTenantIdFromNewHeaders(request);
        String businessId = extractBusinessIdFromNewHeaders(request);
        String requestorType = extractRequestorType(request);
        String recipientId = extractRecipientIdFromNewHeaders(request, requestorType);
        String txn = extractTransactionId(request);

        log.info("Listing missed callback notification IDs: tenantId={}, businessId={}, page={}, size={}, requestorType={}, recipientId={}, status={}, eventType={}, priority={}, fromDate={}, toDate={}, sortBy={}, sortDirection={}, txn={}",
                tenantId, businessId, page, size, requestorType, recipientId, status, eventType, priority, fromDate, toDate, sortBy, sortDirection, txn);

        LocalDateTime fromDateTime = parseDateTime(fromDate);
        LocalDateTime toDateTime = parseDateTime(toDate);

        BulkNotificationRequestDto bulkRequest = BulkNotificationRequestDto.builder()
                .page(page)
                .size(size)
                .recipientType(requestorType)
                .recipientId(recipientId)
                .status(status)
                .eventType(eventType)
                .priority(priority)
                .fromDate(fromDateTime)
                .toDate(toDateTime)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PagedResponseDto<MissedNotificationResponseDto> response = missedNotificationService.listMissedCallbackNotificationIds(
                tenantId, businessId, bulkRequest);

        log.info("Listed missed callback notification IDs: tenantId={}, businessId={}, totalItems={}, txn={}",
                tenantId, businessId, response.getPagination().getTotalItems(), txn);

        // Sign response
        HttpHeaders responseHeaders = addSignatureToResponse(tenantId, response);
        return ResponseEntity.ok().headers(responseHeaders).body(response);
    }

    @Operation(
        summary = "Get missed callback notification by ID",
        description = "Returns complete callback notification details for the specified ID. " +
                     "Automatically marks the returned notification as RETRIEVED in the database. " +
                     "Only returns notifications with failed/processing/scheduled status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Missed callback notification retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Notification not found or not eligible for retrieval"),
        @ApiResponse(responseCode = "400", description = "Invalid notification ID or missing required headers"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid tenant or business"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<MissedNotificationResponseDto> getMissedCallbackNotificationById(
            HttpServletRequest request,

            @Parameter(description = "Notification ID to retrieve", required = true)
            @PathVariable String notificationId) {

        // Extract new headers
        String tenantId = extractTenantIdFromNewHeaders(request);
        String businessId = extractBusinessIdFromNewHeaders(request);
        String requestorType = extractRequestorType(request);
        String recipientId = extractRecipientIdFromNewHeaders(request, requestorType);
        String txn = extractTransactionId(request);

        log.info("Retrieving missed callback notification by ID: tenantId={}, businessId={}, requestorType={}, recipientId={}, notificationId={}, txn={}",
                tenantId, businessId, requestorType, recipientId, notificationId, txn);

        MissedNotificationResponseDto notification = missedNotificationService.getMissedCallbackNotificationByIdEnhanced(
                tenantId, businessId, notificationId);

        // Mark this notification as retrieved
        missedNotificationService.markCallbackNotificationAsRetrieved(tenantId, notificationId);

        log.info("Retrieved and marked callback notification as RETRIEVED: tenantId={}, businessId={}, notificationId={}, type={}, status={}, txn={}",
                tenantId, businessId, notificationId, notification.getType(), notification.getStatus(), txn);

        // Sign response
        HttpHeaders responseHeaders = addSignatureToResponse(tenantId, notification);
        return ResponseEntity.ok().headers(responseHeaders).body(notification);
    }

    @Operation(
        summary = "Get all missed callback notifications",
        description = "Returns complete callback notification details with advanced filtering capabilities. " +
                     "Automatically marks all returned notifications as RETRIEVED in the database. " +
                     "Only includes failed/processing/scheduled callback notifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Missed callback notifications retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid tenant or business"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/all")
    public ResponseEntity<PagedResponseDto<MissedNotificationResponseDto>> getAllMissedCallbackNotifications(
            HttpServletRequest request,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Filter by status (FAILED, PROCESSING, RETRY_SCHEDULED) - optional")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by event type - optional")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Filter by priority level (CRITICAL, HIGH, MEDIUM, LOW) - optional")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Start date for filtering (format: yyyy-MM-dd'T'HH:mm:ss) - optional")
            @RequestParam(required = false) String fromDate,
            @Parameter(description = "End date for filtering (format: yyyy-MM-dd'T'HH:mm:ss) - optional")
            @RequestParam(required = false) String toDate,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(description = "Comma-separated list of specific notification IDs (optional - leave empty to get all)")
            @RequestParam(required = false) List<String> notificationIds,
            @Parameter(description = "Minimum attempt count filter - optional")
            @RequestParam(required = false) Integer minAttemptCount,
            @Parameter(description = "Maximum attempt count filter - optional")
            @RequestParam(required = false) Integer maxAttemptCount) {

        // Extract new headers
        String tenantId = extractTenantIdFromNewHeaders(request);
        String businessId = extractBusinessIdFromNewHeaders(request);
        String requestorType = extractRequestorType(request);
        String recipientId = extractRecipientIdFromNewHeaders(request, requestorType);
        String txn = extractTransactionId(request);

        LocalDateTime fromDateTime = parseDateTime(fromDate);
        LocalDateTime toDateTime = parseDateTime(toDate);

        BulkNotificationRequestDto bulkRequest = BulkNotificationRequestDto.builder()
                .page(page)
                .size(size)
                .recipientType(requestorType)
                .recipientId(recipientId)
                .status(status)
                .eventType(eventType)
                .priority(priority)
                .fromDate(fromDateTime)
                .toDate(toDateTime)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .notificationIds(notificationIds)
                .minAttemptCount(minAttemptCount)
                .maxAttemptCount(maxAttemptCount)
                .build();

        log.info("Retrieving all missed callback notifications: tenantId={}, businessId={}, requestorType={}, recipientId={}, request={}, txn={}",
                tenantId, businessId, requestorType, recipientId, bulkRequest, txn);

        PagedResponseDto<MissedNotificationResponseDto> response = missedNotificationService.getMissedCallbackNotificationsBulkEnhanced(
                tenantId, businessId, bulkRequest);

        int callbackCount = response.getData().size();
        log.info("Retrieved and marked {} callback notifications as RETRIEVED: tenantId={}, businessId={}, txn={}",
                callbackCount, tenantId, businessId, txn);

        log.info("Retrieved all missed callback notifications: tenantId={}, businessId={}, totalItems={}, txn={}",
                tenantId, businessId, response.getPagination().getTotalItems(), txn);

        // Sign response
        HttpHeaders responseHeaders = addSignatureToResponse(tenantId, response);
        return ResponseEntity.ok().headers(responseHeaders).body(response);
    }

    /**
     * Extract recipient type from request headers (mandatory for missed notification APIs).
     */
    private String extractRecipientType(HttpServletRequest request) {
        String recipientType = request.getHeader(HeaderConstants.RECIPIENT_TYPE);
        if (recipientType == null || recipientType.trim().isEmpty()) {
            recipientType = request.getHeader(HeaderConstants.X_RECIPIENT_TYPE);
        }

        if (recipientType == null || recipientType.trim().isEmpty()) {
            throw new IllegalArgumentException("recipientType header is required for missed notification APIs");
        }

        // Validate against expected values
        if (!recipientType.equals(RecipientType.DATA_FIDUCIARY.name()) && !recipientType.equals(RecipientType.DATA_PROCESSOR.name())) {
            throw new IllegalArgumentException("Invalid recipientType: " + recipientType +
                ". Must be " + RecipientType.DATA_FIDUCIARY.name() + " or " + RecipientType.DATA_PROCESSOR.name());
        }

        return recipientType;
    }

    /**
     * Extract recipient ID from request headers (mandatory for missed notification APIs).
     */
    private String extractRecipientId(HttpServletRequest request) {
        String recipientId = request.getHeader(HeaderConstants.RECIPIENT_ID);
        if (recipientId == null || recipientId.trim().isEmpty()) {
            recipientId = request.getHeader(HeaderConstants.X_RECIPIENT_ID);
        }

        if (recipientId == null || recipientId.trim().isEmpty()) {
            throw new IllegalArgumentException("recipientId header is required for missed notification APIs");
        }

        return recipientId;
    }

    /**
     * Parse date string to LocalDateTime.
     * Expected format: yyyy-MM-dd'T'HH:mm:ss
     */
    private LocalDateTime parseDateTime(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString +
                    ". Expected format: yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    /**
     * Updates the status of a single notification.
     */
    @Operation(
        summary = "Update notification status",
        description = "Updates the status of a specific notification. Commonly used to mark RETRIEVED notifications as ACKNOWLEDGED. " +
                     "Validates recipient authorization and status transition rules."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition or request data"),
        @ApiResponse(responseCode = "403", description = "Not authorized to update this notification"),
        @ApiResponse(responseCode = "404", description = "Notification not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{notificationId}/status")
    public ResponseEntity<StandardApiResponseDto<NotificationStatusUpdateResponseDto>> updateNotificationStatus(
            HttpServletRequest request,

            @Parameter(description = "Notification ID to update", required = true)
            @PathVariable String notificationId,

            @Valid @RequestBody UpdateNotificationStatusRequestDto statusRequest) {

        // Verify request signature BEFORE processing
        verifyRequestSignature(statusRequest, request);

        // Extract new headers
        String tenantId = extractTenantIdFromNewHeaders(request);
        String businessId = extractBusinessIdFromNewHeaders(request);
        String requestorType = extractRequestorType(request);
        String recipientId = extractRecipientIdFromNewHeaders(request, requestorType);
        String txn = extractTransactionId(request);

        log.info("Updating notification status: tenantId={}, businessId={}, notificationId={}, requestorType={}, recipientId={}, status={}, txn={}",
                tenantId, businessId, notificationId, requestorType, recipientId, statusRequest.getStatus(), txn);

        StandardApiResponseDto<NotificationStatusUpdateResponseDto> response =
            statusUpdateService.updateNotificationStatus(
                tenantId, businessId, notificationId,
                requestorType, recipientId, statusRequest);

        log.info("Notification status updated: notificationId={}, newStatus={}, txn={}",
                notificationId, statusRequest.getStatus(), txn);

        // Sign response
        HttpHeaders responseHeaders = addSignatureToResponse(tenantId, response);
        return ResponseEntity.ok().headers(responseHeaders).body(response);
    }

    /**
     * Bulk updates the status of multiple notifications.
     */
    @Operation(
        summary = "Bulk update notification statuses",
        description = "Updates the status of multiple notifications in a single request. " +
                     "Returns detailed success/failure information for each notification."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk update completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Not authorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/status")
    public ResponseEntity<StandardApiResponseDto<BulkNotificationStatusUpdateResponseDto>> bulkUpdateNotificationStatus(
            HttpServletRequest request,

            @Valid @RequestBody BulkUpdateNotificationStatusRequestDto bulkRequest) {

        // Verify request signature BEFORE processing
        verifyRequestSignature(bulkRequest, request);

        // Extract new headers
        String tenantId = extractTenantIdFromNewHeaders(request);
        String businessId = extractBusinessIdFromNewHeaders(request);
        String requestorType = extractRequestorType(request);
        String recipientId = extractRecipientIdFromNewHeaders(request, requestorType);
        String txn = extractTransactionId(request);

        log.info("Bulk updating notification statuses: tenantId={}, businessId={}, requestorType={}, recipientId={}, count={}, status={}, txn={}",
                tenantId, businessId, requestorType, recipientId, bulkRequest.getNotificationIds().size(),
                bulkRequest.getStatus(), txn);

        StandardApiResponseDto<BulkNotificationStatusUpdateResponseDto> response =
            statusUpdateService.bulkUpdateNotificationStatus(
                tenantId, businessId, requestorType, recipientId, bulkRequest);

        log.info("Bulk status update completed: totalProcessed={}, successful={}, failed={}, txn={}",
                response.getData().getTotalProcessed(),
                response.getData().getSuccessful().size(),
                response.getData().getFailed().size(),
                txn);

        // Sign response
        HttpHeaders responseHeaders = addSignatureToResponse(tenantId, response);
        return ResponseEntity.ok().headers(responseHeaders).body(response);
    }

    /**
     * Validates that the recipient type is either DATA_FIDUCIARY or DATA_PROCESSOR.
     */
    private void validateRecipientType(String recipientType) {
        if (!RecipientType.DATA_FIDUCIARY.name().equals(recipientType) &&
            !RecipientType.DATA_PROCESSOR.name().equals(recipientType)) {
            throw new IllegalArgumentException("Invalid recipientType: " + recipientType +
                ". Must be " + RecipientType.DATA_FIDUCIARY.name() + " or " + RecipientType.DATA_PROCESSOR.name());
        }
    }

    // ========================================
    // Signature-related Helper Methods
    // ========================================

    /**
     * Extracts new signature headers from request.
     * These headers replace old X-Tenant-ID, X-Business-ID, X-Recipient-Type, X-Recipient-ID.
     */
    private Map<String, String> extractSignatureHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();

        headers.put(HeaderConstants.TENANT_ID, request.getHeader(HeaderConstants.TENANT_ID));
        headers.put(HeaderConstants.BUSINESS_ID, request.getHeader(HeaderConstants.BUSINESS_ID));
        headers.put(HeaderConstants.REQUESTOR_TYPE, request.getHeader(HeaderConstants.REQUESTOR_TYPE));
        headers.put(HeaderConstants.DATA_PROCESSOR_ID, request.getHeader(HeaderConstants.DATA_PROCESSOR_ID));
        headers.put(HeaderConstants.X_JWS_SIGNATURE, request.getHeader(HeaderConstants.X_JWS_SIGNATURE));

        return headers;
    }

    /**
     * Extracts tenant ID from new header format.
     */
    private String extractTenantIdFromNewHeaders(HttpServletRequest request) {
        String tenantId = request.getHeader(HeaderConstants.TENANT_ID);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("tenant-id header is required");
        }
        return tenantId;
    }

    /**
     * Extracts business ID from new header format.
     */
    private String extractBusinessIdFromNewHeaders(HttpServletRequest request) {
        String businessId = request.getHeader(HeaderConstants.BUSINESS_ID);
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new IllegalArgumentException("business-id header is required");
        }
        return businessId;
    }

    /**
     * Extracts requestor type from new header format.
     */
    private String extractRequestorType(HttpServletRequest request) {
        String requestorType = request.getHeader(HeaderConstants.REQUESTOR_TYPE);
        if (requestorType == null || requestorType.trim().isEmpty()) {
            throw new IllegalArgumentException("requestor-type header is required");
        }

        validateRecipientType(requestorType);
        return requestorType;
    }

    /**
     * Extracts recipient ID based on requestor type.
     * For DATA_FIDUCIARY: uses business-id
     * For DATA_PROCESSOR: uses data-processor-id
     */
    private String extractRecipientIdFromNewHeaders(HttpServletRequest request, String requestorType) {
        if (RecipientType.DATA_FIDUCIARY.name().equals(requestorType)) {
            return extractBusinessIdFromNewHeaders(request);
        } else if (RecipientType.DATA_PROCESSOR.name().equals(requestorType)) {
            String dataProcessorId = request.getHeader(HeaderConstants.DATA_PROCESSOR_ID);
            if (dataProcessorId == null || dataProcessorId.trim().isEmpty()) {
                throw new IllegalArgumentException("data-processor-id header is required for DATA_PROCESSOR");
            }
            return dataProcessorId;
        } else {
            throw new IllegalArgumentException("Invalid requestor type: " + requestorType);
        }
    }

    /**
     * Adds signature to response headers if signing is enabled.
     *
     * @param tenantId the tenant ID for fetching the signing key
     * @param responsePayload the response payload to sign
     * @return HttpHeaders with x-jws-signature header
     */
    private HttpHeaders addSignatureToResponse(String tenantId, Object responsePayload) {
        HttpHeaders headers = new HttpHeaders();

        if (signatureService.isSignResponseEnabled()) {
            try {
                String signature = signatureService.signResponse(tenantId, responsePayload);
                if (signature != null) {
                    headers.add(HeaderConstants.X_JWS_SIGNATURE, signature);
                    log.debug("Added x-jws-signature to response headers for tenant={}", tenantId);
                }
            } catch (Exception e) {
                log.error("Failed to sign response for tenant={}: {}", tenantId, e.getMessage());
                // Re-throw to let GlobalExceptionHandler handle it
                throw e;
            }
        }

        return headers;
    }

    /**
     * Verifies request signature for PUT/POST operations.
     */
    private void verifyRequestSignature(Object payload, HttpServletRequest request) {
        if (signatureService.isVerifyRequestEnabled()) {
            Map<String, String> headers = extractSignatureHeaders(request);
            signatureService.verifyRequest(payload, headers);
            log.debug("Request signature verified successfully");
        }
    }
}