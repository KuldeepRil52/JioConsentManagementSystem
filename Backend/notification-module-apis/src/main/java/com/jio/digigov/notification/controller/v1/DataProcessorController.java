package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.dto.request.event.CreateDataProcessorRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.event.DataProcessorResponseDto;
import com.jio.digigov.notification.service.event.DataProcessorService;
import com.jio.digigov.notification.util.SecurityValidationUtil;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for Data Processor Management APIs
 * Handles CRUD operations for data processors with multi-tenant support
 */
@RestController
@RequestMapping("/v1/data-processors")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Data Processor Management", description = "APIs for managing data processors in the notification system")
public class DataProcessorController {

    private final DataProcessorService dataProcessorService;
    private final SecurityValidationUtil securityValidationUtil;

    /**
     * Create a new data processor
     */
    @Operation(
        summary = "Create a new data processor",
        description = "Creates a new data processor with the provided configuration for a specific tenant and business"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Data processor created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Data processor with same ID already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<StandardApiResponseDto<DataProcessorResponseDto>> createDataProcessor(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,
            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,
            @Parameter(description = "Data processor creation request", required = true)
            @Valid @RequestBody CreateDataProcessorRequestDto request) {

        // Security validation
        String validatedTenantId = securityValidationUtil.validateTenantId(tenantId);
        String validatedBusinessId = securityValidationUtil.validateBusinessId(businessId);
        String validatedTransactionId = securityValidationUtil.validateTransactionId(transactionId);

        log.info("Creating data processor for tenant: {}, business: {}", validatedTenantId, validatedBusinessId);

        DataProcessorResponseDto response = dataProcessorService.createDataProcessor(
                request, validatedTenantId, validatedBusinessId, validatedTransactionId);

        StandardApiResponseDto<DataProcessorResponseDto> apiResponse = StandardApiResponseDto
            .success(response, "Data processor created successfully")
            .withTransactionId(transactionId);

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Get all data processors with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<StandardApiResponseDto<PagedResponseDto<DataProcessorResponseDto>>> getAllDataProcessors(
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID)
            @NotBlank(message = "Business ID is required") String businessId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
            @RequestParam(value = "sort", required = false) String sort) {

        log.info("Retrieving data processors for tenant: {}, business: {}", tenantId, businessId);

        PagedResponseDto<DataProcessorResponseDto> pagedResponse = dataProcessorService.getAllDataProcessors(
                tenantId, businessId, status, page, pageSize, sort);

        StandardApiResponseDto<PagedResponseDto<DataProcessorResponseDto>> apiResponse = StandardApiResponseDto
            .success(pagedResponse, "Data processors retrieved successfully");

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get specific data processor by ID
     */
    @GetMapping("/{dataProcessorId}")
    public ResponseEntity<DataProcessorResponseDto> getDataProcessorById(
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,
            @PathVariable("dataProcessorId") @NotBlank(message = "Data processor ID is required") String dataProcessorId) {

        log.info("Retrieving data processor: {} for tenant: {}, business: {}", dataProcessorId, tenantId, businessId);

        DataProcessorResponseDto response = dataProcessorService.getDataProcessorById(
                dataProcessorId, tenantId, businessId);

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing data processor
     */
    @PutMapping("/{dataProcessorId}")
    public ResponseEntity<DataProcessorResponseDto> updateDataProcessor(
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,
            @PathVariable("dataProcessorId") @NotBlank(message = "Data processor ID is required") String dataProcessorId,
            @Valid @RequestBody CreateDataProcessorRequestDto request) {

        log.info("Updating data processor: {} for tenant: {}, business: {}", dataProcessorId, tenantId, businessId);

        DataProcessorResponseDto response = dataProcessorService.updateDataProcessor(
                dataProcessorId, request, tenantId, businessId, transactionId);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a data processor
     */
    @DeleteMapping("/{dataProcessorId}")
    public ResponseEntity<Void> deleteDataProcessor(
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,
            @PathVariable("dataProcessorId") @NotBlank(message = "Data processor ID is required") String dataProcessorId) {

        log.info("Deleting data processor: {} for tenant: {}, business: {}", dataProcessorId, tenantId, businessId);

        dataProcessorService.deleteDataProcessor(dataProcessorId, tenantId, businessId, transactionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get count of data processors with breakdown
     */
    @GetMapping("/count")
    public ResponseEntity<CountResponseDto> getDataProcessorCount(
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank(message = "Business ID is required") String businessId,
            @RequestParam(value = "status", required = false) String status) {
        
        log.info("Getting data processor count for tenant: {}, business: {}", tenantId, businessId);
        
        CountResponseDto response = dataProcessorService.getDataProcessorCount(
                tenantId, businessId, status);
        
        return ResponseEntity.ok(response);
    }
}