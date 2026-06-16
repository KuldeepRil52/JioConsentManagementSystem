package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.dto.request.onboarding.OnboardingSetupRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.onboarding.OnboardingJobStatusResponseDto;
import com.jio.digigov.notification.dto.response.onboarding.OnboardingJobSummaryDto;
import com.jio.digigov.notification.dto.response.onboarding.OnboardingSetupResponseDto;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.exception.ValidationException;
import com.jio.digigov.notification.service.onboarding.OnboardingService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for tenant onboarding operations.
 *
 * Provides endpoints to:
 * - Initiate onboarding with default setup (TENANT scope only, requires tenant_id == business_id)
 * - Check onboarding job status
 * - List onboarding jobs with pagination
 *
 * All endpoints require X-Tenant-Id and X-Business-Id headers.
 * For the setup endpoint, tenant_id and business_id must be identical and scope must be TENANT.
 * X-Transaction-Id is optional (auto-generated if not provided).
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@RestController
@RequestMapping("/v1/onboarding")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Onboarding", description = "APIs for tenant/business onboarding with default notification infrastructure")
public class OnboardingController extends BaseController {

    private final OnboardingService onboardingService;

    /**
     * Initiates onboarding process for a tenant.
     *
     * This endpoint validates that no templates or event configurations exist,
     * creates an onboarding job, and triggers asynchronous processing.
     *
     * IMPORTANT: This endpoint only accepts TENANT scope and requires that
     * tenant_id and business_id are the same.
     *
     * @param request HTTP servlet request (for header extraction)
     * @param setupRequest Onboarding setup request body
     * @return 202 Accepted with job ID and status check URL
     */
    @PostMapping("/setup")
    @Operation(
            summary = "Initialize onboarding",
            description = "Validates prerequisites (no existing templates/configs) and creates async job for default setup. "
                    + "Returns immediately with job ID for status tracking. "
                    + "Only accepts TENANT scope with matching tenant_id and business_id."
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "202",
                description = "Onboarding job created successfully and processing started",
                content = @Content(schema = @Schema(implementation = OnboardingSetupResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Validation failed - invalid scope, tenant_id != business_id, "
                        + "or templates/configs already exist",
                content = @Content(schema = @Schema(implementation = StandardApiResponseDto.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @Content(schema = @Schema(implementation = StandardApiResponseDto.class))
        )
    })
    public ResponseEntity<StandardApiResponseDto<OnboardingSetupResponseDto>> setup(
            HttpServletRequest request,
            @Valid @RequestBody OnboardingSetupRequestDto setupRequest,
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "TENANT")
                    String scopeLevel) {

        String tenantId = extractTenantId(request);
        String businessId = extractBusinessId(request);
        String transactionId = extractTransactionId(request);
        ScopeLevel scope = parseScopeLevel(scopeLevel);

        // Validate that scope is TENANT only
        if (scope != ScopeLevel.TENANT) {
            log.error("Invalid scope level for onboarding: {}. Only TENANT scope is allowed", scope);
            throw new ValidationException("Onboarding API only accepts TENANT scope");
        }

        // Validate that tenant_id and business_id are the same
        if (!tenantId.equals(businessId)) {
            log.error("Tenant ID ({}) and Business ID ({}) must be the same for onboarding",
                    tenantId, businessId);
            throw new ValidationException(
                    "Tenant ID and Business ID must be the same for TENANT scope onboarding");
        }

        log.info("Onboarding setup request: tenant={}, business={}, scope={}, transaction={}, request={}",
                tenantId, businessId, scope, transactionId, setupRequest);

        OnboardingSetupResponseDto response = onboardingService.initiateOnboarding(
                tenantId,
                businessId,
                scope,
                setupRequest,
                transactionId,
                request
        );

        StandardApiResponseDto<OnboardingSetupResponseDto> apiResponse =
                StandardApiResponseDto.<OnboardingSetupResponseDto>builder()
                        .success(true)
                        .code("JDNM0000")
                        .message("Onboarding job created successfully")
                        .data(response)
                        .timestamp(java.time.LocalDateTime.now())
                        .transactionId(transactionId)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    /**
     * Retrieves the status of an onboarding job.
     *
     * @param request HTTP servlet request (for header extraction)
     * @param jobId Job identifier
     * @return 200 OK with detailed job status
     */
    @GetMapping("/jobs/{jobId}")
    @Operation(
            summary = "Get onboarding job status",
            description = "Retrieves detailed status of an onboarding job including progress, results, and errors."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Job status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OnboardingJobStatusResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found",
                    content = @Content(schema = @Schema(implementation = StandardApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Job does not belong to the specified business",
                    content = @Content(schema = @Schema(implementation = StandardApiResponseDto.class))
            )
    })
    public ResponseEntity<StandardApiResponseDto<OnboardingJobStatusResponseDto>> getJobStatus(
            HttpServletRequest request,
            @Parameter(description = "Onboarding job identifier", required = true)
            @PathVariable String jobId) {

        // Extract headers
        String tenantId = extractTenantId(request);
        String businessId = extractBusinessId(request);
        String transactionId = extractTransactionId(request);

        log.info("Get job status: tenant={}, business={}, jobId={}, transaction={}",
                tenantId, businessId, jobId, transactionId);

        // Get job status
        OnboardingJobStatusResponseDto response = onboardingService.getJobStatus(
                tenantId,
                businessId,
                jobId
        );

        StandardApiResponseDto<OnboardingJobStatusResponseDto> apiResponse =
                StandardApiResponseDto.<OnboardingJobStatusResponseDto>builder()
                        .success(true)
                        .code("JDNM0000")
                        .message("Job status retrieved successfully")
                        .data(response)
                        .timestamp(java.time.LocalDateTime.now())
                        .transactionId(transactionId)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lists onboarding jobs with pagination and filtering.
     *
     * @param request HTTP servlet request (for header extraction)
     * @param page Page number (0-indexed)
     * @param size Page size (max 100)
     * @param status Filter by job status (optional)
     * @param sort Sort specification (default: createdAt:desc)
     * @return 200 OK with paginated list of jobs
     */
    @GetMapping("/jobs")
    @Operation(
            summary = "List onboarding jobs",
            description = "Retrieves paginated list of onboarding jobs for a business with optional status filtering."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Jobs retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    public ResponseEntity<StandardApiResponseDto<Page<OnboardingJobSummaryDto>>> listJobs(
            HttpServletRequest request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by job status")
            @RequestParam(required = false) OnboardingJobStatus status,
            @Parameter(description = "Sort specification (field:direction)", example = "createdAt:desc")
            @RequestParam(defaultValue = "createdAt:desc") String sort) {

        // Extract headers
        String tenantId = extractTenantId(request);
        String businessId = extractBusinessId(request);
        String transactionId = extractTransactionId(request);

        log.info("List jobs: tenant={}, business={}, page={}, size={}, status={}, transaction={}",
                tenantId, businessId, page, size, status, transactionId);

        // Validate page size
        if (size > 100) {
            size = 100;
            log.warn("Page size limited to 100");
        }

        // Parse sort parameter
        Pageable pageable = createPageable(page, size, sort);

        // Get jobs
        Page<OnboardingJobSummaryDto> jobsPage = onboardingService.listJobs(
                tenantId,
                businessId,
                status,
                pageable
        );

        StandardApiResponseDto<Page<OnboardingJobSummaryDto>> apiResponse =
                StandardApiResponseDto.<Page<OnboardingJobSummaryDto>>builder()
                        .success(true)
                        .code("JDNM0000")
                        .message("Jobs retrieved successfully")
                        .data(jobsPage)
                        .timestamp(java.time.LocalDateTime.now())
                        .transactionId(transactionId)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Creates Pageable from page, size, and sort parameters.
     *
     * @param page Page number
     * @param size Page size
     * @param sortParam Sort specification (field:direction)
     * @return Pageable object
     */
    private Pageable createPageable(int page, int size, String sortParam) {
        String[] sortParts = sortParam.split(":");
        String sortField = sortParts.length > 0 ? sortParts[0] : "createdAt";
        String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortField);

        return PageRequest.of(page, size, sort);
    }

    private ScopeLevel parseScopeLevel(String scopeLevel) {
        try {
            return ScopeLevel.valueOf(scopeLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid scope level: " + scopeLevel);
        }
    }
}
