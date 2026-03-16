package com.jio.digigov.notification.controller.v1.masterlist;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.dto.request.masterlist.CreateMasterListRequestDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.masterlist.TenantMasterListResponseDto;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.service.masterlist.TenantMasterListConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for managing tenant-specific master list configurations.
 *
 * This controller provides APIs to create, read, update, and delete tenant-specific
 * master list configurations. These configurations override the default static
 * master list configuration file for specific tenants.
 */
@RestController
@RequestMapping("/v1/master-lists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Master List Configuration", description = "APIs for managing tenant master list configurations")
public class MasterListConfigController extends MasterListBaseController {

    private final TenantMasterListConfigService tenantMasterListConfigService;

    @Operation(summary = "Get master list configuration",
               description = "Retrieves the master list configuration for the specified tenant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Master list configuration retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No master list configuration found"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid tenant ID header"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<StandardApiResponseDto<TenantMasterListResponseDto>> getMasterListConfig(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId) {

        log.info("Request to get master list configuration for tenant: {}", tenantId);

        try {
            Optional<TenantMasterListConfig> config =
                tenantMasterListConfigService.getTenantConfig(tenantId);

            String correlationId = extractCorrelationId(httpRequest);

            if (config.isPresent()) {
                TenantMasterListResponseDto response = convertToResponse(config.get());
                log.info("Found master list configuration for tenant {}", tenantId);

                StandardApiResponseDto<TenantMasterListResponseDto> apiResponse = StandardApiResponseDto.success(
                    response,
                    "Master list configuration retrieved successfully"
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

                return ResponseEntity.ok(apiResponse);
            } else {
                log.info("No active master list configuration found for tenant {}", tenantId);
                StandardApiResponseDto<TenantMasterListResponseDto> apiResponse =
                        StandardApiResponseDto.<TenantMasterListResponseDto>notFound(
                    "Master list configuration"
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
            }
        } catch (Exception e) {
            log.error("Error retrieving active master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);

            String correlationId = extractCorrelationId(httpRequest);
            StandardApiResponseDto<TenantMasterListResponseDto> apiResponse =
                    StandardApiResponseDto.<TenantMasterListResponseDto>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Create master list configuration",
               description = "Creates a new master list configuration with event mappings for the tenant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Master list created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing headers"),
        @ApiResponse(responseCode = "409", description = "Configuration already exists")
    })
    @PostMapping
    public ResponseEntity<StandardApiResponseDto<TenantMasterListResponseDto>> createMasterListConfig(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @Valid @RequestBody CreateMasterListRequestDto request) {

        log.info("Request to create master list configuration for tenant {} with {} entries",
                tenantId, request.getMasterListConfig().size());

        String correlationId = extractCorrelationId(httpRequest);

        try {
            TenantMasterListConfig savedConfig = tenantMasterListConfigService.createMasterListConfig(
                tenantId, request);

            TenantMasterListResponseDto response = convertToResponse(savedConfig);
            log.info("Successfully created master list configuration for tenant {}", tenantId);

            StandardApiResponseDto<TenantMasterListResponseDto> apiResponse = StandardApiResponseDto.success(
                response,
                "Master list configuration created successfully"
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            log.error("Validation error creating master list configuration for tenant {}: {}",
                     tenantId, e.getMessage());

            StandardApiResponseDto<TenantMasterListResponseDto> apiResponse =
                    StandardApiResponseDto.<TenantMasterListResponseDto>validationError(
                e.getMessage()
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Error creating master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);

            StandardApiResponseDto<TenantMasterListResponseDto> apiResponse =
                    StandardApiResponseDto.<TenantMasterListResponseDto>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Update master list configuration",
               description = "Updates the existing master list configuration for the tenant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Master list updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing headers"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @PutMapping
    public ResponseEntity<StandardApiResponseDto<TenantMasterListResponseDto>> updateMasterListConfig(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @Valid @RequestBody CreateMasterListRequestDto request) {

        log.info("Request to update master list configuration for tenant {} with {} entries",
                tenantId, request.getMasterListConfig().size());

        String correlationId = extractCorrelationId(httpRequest);

        try {
            TenantMasterListConfig updatedConfig = tenantMasterListConfigService.updateMasterListConfig(
                tenantId, request);

            TenantMasterListResponseDto response = convertToResponse(updatedConfig);
            log.info("Successfully updated master list configuration for tenant {}", tenantId);

            StandardApiResponseDto<TenantMasterListResponseDto> apiResponse = StandardApiResponseDto.success(
                response,
                "Master list configuration updated successfully"
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (TenantMasterListConfigService.TenantMasterListConfigException e) {
            log.error("Error updating master list configuration for tenant {}: {}",
                     tenantId, e.getMessage());

            StandardApiResponseDto<TenantMasterListResponseDto> apiResponse;
            if (e.getMessage().contains("not found")) {
                apiResponse = StandardApiResponseDto.<TenantMasterListResponseDto>notFound(
                    "Master list configuration"
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
            } else {
                apiResponse = StandardApiResponseDto.<TenantMasterListResponseDto>validationError(
                    e.getMessage()
                ).withTransactionId(correlationId)
                    .withPath(httpRequest.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }
        } catch (Exception e) {
            log.error("Error updating master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);

            StandardApiResponseDto<TenantMasterListResponseDto> apiResponse =
                    StandardApiResponseDto.<TenantMasterListResponseDto>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @Operation(summary = "Delete master list configuration",
            description = "Deletes the master list configuration for the " +
                    "specified tenant, causing fallback to static configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Master list configuration deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid tenant ID header"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping
    public ResponseEntity<StandardApiResponseDto<Void>> deleteMasterListConfig(
            HttpServletRequest httpRequest,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId) {

        log.info("Request to delete master list configuration for tenant: {}", tenantId);

        String correlationId = extractCorrelationId(httpRequest);

        try {
            tenantMasterListConfigService.deleteTenantConfig(tenantId);
            log.info("Successfully deleted master list configuration for tenant {}", tenantId);

            StandardApiResponseDto<Void> apiResponse = StandardApiResponseDto.<Void>success(
                "Active master list configuration deleted successfully"
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error deleting active master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);

            StandardApiResponseDto<Void> apiResponse =
                    StandardApiResponseDto.<Void>internalServerError()
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
}