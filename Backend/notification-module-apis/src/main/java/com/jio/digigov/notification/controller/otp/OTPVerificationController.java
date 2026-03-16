package com.jio.digigov.notification.controller.otp;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.otp.VerifyOTPRequestDto;
import com.jio.digigov.notification.dto.response.otp.VerifyOTPResponseDto;
import com.jio.digigov.notification.service.otp.OTPVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for OTP verification operations.
 *
 * This controller provides endpoints for verifying OTPs generated through INIT_OTP events.
 * It implements a hybrid security model to support both system-wide and tenant-specific
 * OTP verification flows.
 *
 * Hybrid Security Model:
 *
 * 1. System OTP Verification (/v1/system/otp/verify):
 *    - No authentication required
 *    - No tenant/business headers required
 *    - Used for system-wide OTPs (tenant onboarding, admin notifications, etc.)
 *    - OTPs stored in shared database (tenant_db_shared)
 *    - Suitable for public-facing forms and system automation
 *
 * 2. Tenant OTP Verification (/v1/otp/verify):
 *    - Requires X-Tenant-ID and X-Business-ID headers
 *    - Used for tenant-specific OTPs (user login, 2FA, transaction verification, etc.)
 *    - OTPs stored in tenant-specific databases (tenant_db_{tenantId})
 *    - Enforces tenant isolation and multi-tenancy compliance
 *
 * Verification Security Features:
 * - Transaction ID validation (security token)
 * - Expiry time enforcement
 * - Attempt limit tracking (default: 3 attempts)
 * - One-time use enforcement (prevents replay attacks)
 * - Case-insensitive OTP comparison for alphanumeric OTPs
 * - Detailed error responses for debugging
 *
 * Status Codes:
 * - 200 OK: OTP verified successfully or verification failed with details
 * - 400 Bad Request: Invalid request body or missing required fields
 * - 500 Internal Server Error: Unexpected error during verification
 *
 * Usage Examples:
 *
 * System OTP verification (no headers):
 * <pre>
 * POST /v1/system/otp/verify
 * Content-Type: application/json
 *
 * {
 *   "eventId": "EVT_20251105_90305",
 *   "txnId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
 *   "otpValue": "123456"
 * }
 * </pre>
 *
 * Tenant OTP verification (with headers):
 * <pre>
 * POST /v1/otp/verify
 * X-Tenant-ID: tenant123
 * X-Business-ID: business456
 * Content-Type: application/json
 *
 * {
 *   "eventId": "EVT_20251105_90305",
 *   "txnId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
 *   "otpValue": "123456"
 * }
 * </pre>
 *
 * Success Response:
 * <pre>
 * {
 *   "verified": true,
 *   "status": "SUCCESS",
 *   "message": "OTP verified successfully",
 *   "attemptsRemaining": null,
 *   "txnId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
 *   "eventId": "EVT_20251105_90305"
 * }
 * </pre>
 *
 * Error Response:
 * <pre>
 * {
 *   "verified": false,
 *   "status": "INVALID_OTP",
 *   "message": "Invalid OTP provided",
 *   "attemptsRemaining": 2,
 *   "txnId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
 *   "eventId": "EVT_20251105_90305"
 * }
 * </pre>
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-01-21
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OTP Verification", description = "OTP verification endpoints with hybrid security model")
public class OTPVerificationController extends BaseController {

    private final OTPVerificationService otpVerificationService;

    /**
     * Verify a system-wide OTP (no authentication required).
     *
     * This endpoint is designed for public-facing forms and system automation scenarios
     * where tenant/business context is not available. OTPs are stored in the shared
     * database and can be verified without authentication headers.
     *
     * Use Cases:
     * - Tenant onboarding verification
     * - Public form submissions
     * - System administrator notifications
     * - Internal automation workflows
     *
     * Security Considerations:
     * - Transaction ID acts as the security token
     * - OTPs expire after configured time (default: 5 minutes)
     * - Limited to 3 verification attempts by default
     * - No rate limiting implemented (consider adding for production)
     *
     * @param request VerifyOTPRequestDto containing txnId and otpValue
     * @return VerifyOTPResponseDto with verification result
     */
    @PostMapping("/v1/system/otp/verify")
    @Operation(
            summary = "Verify system-wide OTP",
            description = "Verifies an OTP without requiring authentication or tenant/business headers. " +
                          "Used for system-wide OTPs such as tenant onboarding and public forms."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "OTP verification result returned (success or failure)",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = VerifyOTPResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or missing required fields"
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error during verification"
        )
    })
    public ResponseEntity<VerifyOTPResponseDto> verifySystemOTP(
            @RequestBody @Valid VerifyOTPRequestDto request) {

        log.info("System OTP verification request received for eventId: {}, txnId: {}", request.getEventId(), request.getTxnId());

        try {
            // Verify OTP using SYSTEM as tenant and business identifiers
            VerifyOTPResponseDto response = otpVerificationService.verifyOTP(
                    request,
                    "SYSTEM",
                    "SYSTEM"
            );

            // Log verification result
            if (response.getVerified()) {
                log.info("System OTP verified successfully for eventId: {}, txnId: {}", request.getEventId(), request.getTxnId());
            } else {
                log.warn("System OTP verification failed for eventId: {}, txnId: {}, status: {}, message: {}",
                         request.getEventId(), request.getTxnId(), response.getStatus(), response.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying system OTP for txnId: {}", request.getTxnId());
            VerifyOTPResponseDto errorResponse = VerifyOTPResponseDto.builder()
                    .verified(false)
                    .status("VERIFICATION_ERROR")
                    .message("An error occurred during OTP verification: " + e.getMessage())
                    .txnId(request.getTxnId())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify a tenant-specific OTP (requires authentication headers).
     *
     * This endpoint enforces tenant isolation by requiring X-Tenant-ID and X-Business-ID
     * headers. OTPs are stored in tenant-specific databases and can only be verified
     * with correct tenant context.
     *
     * Use Cases:
     * - User login verification
     * - Two-factor authentication (2FA)
     * - Transaction verification
     * - Account recovery
     * - Sensitive operation confirmations
     *
     * Security Considerations:
     * - Requires X-Tenant-ID and X-Business-ID headers (enforced by TenantFilter)
     * - Transaction ID acts as additional security token
     * - Tenant database isolation prevents cross-tenant access
     * - OTPs expire after configured time (default: 5 minutes)
     * - Limited to configured max attempts (default: 3)
     *
     * @param request VerifyOTPRequestDto containing txnId and otpValue
     * @param httpRequest HttpServletRequest for extracting tenant/business headers
     * @return VerifyOTPResponseDto with verification result
     */
    @PostMapping("/v1/otp/verify")
    @Operation(
            summary = "Verify tenant-specific OTP",
            description = "Verifies an OTP with tenant/business context enforcement. " +
                          "Requires X-Tenant-ID and X-Business-ID headers for tenant isolation."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "OTP verification result returned (success or failure)",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = VerifyOTPResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body, missing required fields, or missing tenant/business headers"
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Missing or invalid authentication"
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error during verification"
        )
    })
    public ResponseEntity<VerifyOTPResponseDto> verifyTenantOTP(
            @RequestBody @Valid VerifyOTPRequestDto request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

        try {
            // Extract tenant and business IDs from headers
            String tenantId = extractTenantId(httpRequest);
            String businessId = extractBusinessId(httpRequest);

            log.info("Tenant OTP verification request received for eventId: {}, txnId: {}, tenantId: {}, businessId: {}",
                     request.getEventId(), request.getTxnId(), tenantId, businessId);

            // Verify OTP with tenant context
            VerifyOTPResponseDto response = otpVerificationService.verifyOTP(
                    request,
                    tenantId,
                    businessId
            );

            // Log verification result
            if (response.getVerified()) {
                log.info("Tenant OTP verified successfully for eventId: {}, txnId: {}, tenantId: {}, businessId: {}",
                         request.getEventId(), request.getTxnId(), tenantId, businessId);
            } else {
                log.warn("Tenant OTP verification failed for eventId: {}, txnId: {}, tenantId: {}, businessId: {}, status: {}, message: {}",
                         request.getEventId(), request.getTxnId(), tenantId, businessId, response.getStatus(), response.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Handle missing or invalid headers
            log.error("Invalid headers in tenant OTP verification request for txnId: {}", request.getTxnId());
            VerifyOTPResponseDto errorResponse = VerifyOTPResponseDto.builder()
                    .verified(false)
                    .status("INVALID_REQUEST")
                    .message("Missing or invalid tenant/business headers: " + e.getMessage())
                    .txnId(request.getTxnId())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("Error verifying tenant OTP for txnId: {}", request.getTxnId());
            VerifyOTPResponseDto errorResponse = VerifyOTPResponseDto.builder()
                    .verified(false)
                    .status("VERIFICATION_ERROR")
                    .message("An error occurred during OTP verification: " + e.getMessage())
                    .txnId(request.getTxnId())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
