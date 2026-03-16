package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.dpo.CreateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.request.dpo.UpdateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.dpo.DpoConfigurationResponseDto;
import com.jio.digigov.notification.service.dpo.DpoConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Data Protection Officer (DPO) Configuration Management APIs.
 *
 * <p>Handles CRUD operations for DPO configurations with multi-tenant support.
 * DPO configurations are tenant-scoped (not business-scoped) - each tenant
 * can have only ONE DPO configuration shared across all businesses.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Create DPO configuration (one per tenant)</li>
 *   <li>Retrieve active DPO configuration</li>
 *   <li>Update DPO contact information</li>
 *   <li>Delete (soft delete) DPO configuration</li>
 * </ul>
 *
 * <p><b>Important:</b> DPO configurations are tenant-level resources.
 * No X-Business-Id header is required or used.</p>
 *
 * @author DPDP Notification Team
 * @version 1.0
 * @since 2025-10-24
 */
@RestController
@RequestMapping("/v1/dpo-configurations")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "DPO Configuration",
        description = "Data Protection Officer configuration management. Configure DPO contact information " +
                "for receiving notifications about data privacy events (e.g., grievances, consent requests). " +
                "One DPO configuration per tenant, shared across all businesses.")
public class DpoConfigurationController extends BaseController {

    private final DpoConfigurationService dpoConfigurationService;

    /**
     * Create a new DPO configuration for the tenant.
     */
    @PostMapping
    @Operation(
        summary = "Create DPO configuration",
        description = "Creates a new DPO configuration for the tenant. Only one DPO configuration " +
                "is allowed per tenant. This configuration will be used to send notifications to the " +
                "Data Protection Officer for privacy-related events."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "DPO configuration created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DpoConfigurationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "DPO configuration already exists for this tenant",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<StandardApiResponseDto<DpoConfigurationResponseDto>> createDpoConfiguration(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_BUSINESS_ID, required = false) String businessId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(description = "DPO configuration details", required = true)
            @Valid @RequestBody CreateDpoConfigurationRequestDto request) {

        log.info("Creating DPO configuration for tenant: {}, businessId: {}", tenantId, businessId);

        DpoConfigurationResponseDto configResponse = dpoConfigurationService.createDpoConfiguration(
                tenantId, businessId, request);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<DpoConfigurationResponseDto> response = StandardApiResponseDto.success(
            configResponse,
            "DPO configuration created successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get the active DPO configuration for the tenant.
     */
    @GetMapping
    @Operation(
        summary = "Get DPO configuration",
        description = "Retrieves the active DPO configuration for the tenant. " +
                     "Returns the DPO contact information including name, email, mobile, and address."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "DPO configuration retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DpoConfigurationResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "DPO configuration not found for this tenant",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<StandardApiResponseDto<DpoConfigurationResponseDto>> getDpoConfiguration(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId) {

        log.info("Retrieving DPO configuration for tenant: {}", tenantId);

        DpoConfigurationResponseDto configResponse = dpoConfigurationService.getDpoConfiguration(tenantId);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<DpoConfigurationResponseDto> response = StandardApiResponseDto.success(
            configResponse,
            "DPO configuration retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    /**
     * Update the existing DPO configuration for the tenant.
     */
    @PutMapping
    @Operation(
        summary = "Update DPO configuration",
        description = "Updates the existing DPO configuration for the tenant. " +
                     "Replaces the current DPO contact information with the new values provided."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "DPO configuration updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DpoConfigurationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "DPO configuration not found for this tenant",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<StandardApiResponseDto<DpoConfigurationResponseDto>> updateDpoConfiguration(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(description = "Updated DPO configuration details", required = true)
            @Valid @RequestBody UpdateDpoConfigurationRequestDto request) {

        log.info("Updating DPO configuration for tenant: {}", tenantId);

        DpoConfigurationResponseDto configResponse = dpoConfigurationService.updateDpoConfiguration(
                tenantId, request);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<DpoConfigurationResponseDto> response = StandardApiResponseDto.success(
            configResponse,
            "DPO configuration updated successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete the DPO configuration for the tenant.
     */
    @DeleteMapping
    @Operation(
        summary = "Delete DPO configuration",
        description = "Deletes the DPO configuration for the tenant. " +
                     "The configuration is removed from the system."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "DPO configuration deleted successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "DPO configuration not found for this tenant",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<StandardApiResponseDto<Void>> deleteDpoConfiguration(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId) {

        log.info("Deleting DPO configuration for tenant: {}", tenantId);

        dpoConfigurationService.deleteDpoConfiguration(tenantId);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<Void> response = StandardApiResponseDto.<Void>success(
            "DPO configuration deleted successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
}
