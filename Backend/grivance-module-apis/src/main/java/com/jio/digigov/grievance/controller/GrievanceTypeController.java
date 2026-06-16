package com.jio.digigov.grievance.controller;

import com.jio.digigov.grievance.constant.HeaderConstants;
import com.jio.digigov.grievance.dto.request.GrievanceTypeCreateRequest;
import com.jio.digigov.grievance.dto.request.GrievanceTypeUpdateRequest;
import com.jio.digigov.grievance.dto.response.GrievanceTypeResponse;
import com.jio.digigov.grievance.entity.GrievanceType;
import com.jio.digigov.grievance.enumeration.ScopeLevel;
import com.jio.digigov.grievance.mapper.GrievanceTypeMapper;
import com.jio.digigov.grievance.service.GrievanceTypeService;
import com.jio.digigov.grievance.util.ScopeValidationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for managing Grievance Types with multi-tenant support.
 * Supports CRUD operations with transaction logging and error handling.
 */
@RestController
@RequestMapping("/api/v1/grievance-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Grievance Type", description = "Manage grievance types for different tenants and businesses.")
public class GrievanceTypeController {

    private final GrievanceTypeService service;

    /**
     * Create a new GrievanceType.
     */
    @PostMapping
    @Operation(summary = "Create grievance type", description = "Creates a new grievance type for a tenant and business.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GrievanceType created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or duplicate entry"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> createGrievanceType(
            @Parameter(description = "Grievance type creation request", required = true)
            @Valid @RequestBody GrievanceTypeCreateRequest request,

            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(description = "Business ID", required = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Transaction ID", required = true)
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,

            @Parameter(description = "Transaction ID", required = true)
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Creating GrievanceType category={} tenant={} business={}", transactionId, request.getGrievanceType(), tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            GrievanceType saved = service.create(GrievanceTypeMapper.toEntity(request), tenantId, businessId, scopeLevel);
            return ResponseEntity.ok(GrievanceTypeMapper.toResponse(saved));

        } catch (IllegalArgumentException e) {
            log.warn("TXN={} | Duplicate grievance type while creating tenant={} business={} error={}",
                    transactionId, tenantId, businessId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Duplicate entry",
                            "message", "A grievance type with the same name already exists for this business. Please choose a different name."
                    ));

        } catch (Exception e) {
            log.error("TXN={} | Failed to create GrievanceType tenant={} business={} error={}",
                    transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while creating the grievance type"
                    ));
        }
    }

    /**
     * List all GrievanceTypes for a tenant and business.
     */
    @GetMapping
    @Operation(summary = "List grievance types", description = "Retrieve all grievance types for a tenant and business based on scope level.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GrievanceTypes retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<GrievanceTypeResponse>> listGrievanceTypes(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Listing GrievanceTypes tenant={} business={} scope={}", transactionId, tenantId, businessId, scopeLevel);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            List<GrievanceType> types = service.list(tenantId, businessId, scopeLevel);
            List<GrievanceTypeResponse> responses = types.stream()
                    .map(GrievanceTypeMapper::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("TXN={} | Failed to list GrievanceTypes tenant={} business={} error={}", transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get a GrievanceType by ID.
     */
    @GetMapping("/{grievanceTypeId}")
    @Operation(summary = "Get grievance type by ID", description = "Retrieve a grievance type by its ID for a tenant and business.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GrievanceType retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "GrievanceType not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> getGrievanceTypeById(
            @PathVariable String grievanceTypeId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Fetching GrievanceType={} tenant={} business={}", transactionId, grievanceTypeId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            Optional<GrievanceType> grievanceTypeOpt = service.getById(grievanceTypeId, tenantId, businessId);

            if (grievanceTypeOpt.isPresent()) {
                return ResponseEntity.ok(GrievanceTypeMapper.toResponse(grievanceTypeOpt.get()));
            } else {
                log.warn("TXN={} | GrievanceType={} not found tenant={} business={}", transactionId, grievanceTypeId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "GrievanceType not found",
                                "message", "No grievance type found with the given ID: " + grievanceTypeId
                        ));
            }

        } catch (Exception e) {
            log.error("TXN={} | Error fetching GrievanceType={} tenant={} business={} error={}", transactionId, grievanceTypeId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while retrieving the grievance type"
                    ));
        }
    }

    /**
     * Update an existing GrievanceType.
     */
    @PutMapping("/{grievanceTypeId}")
    @Operation(summary = "Update grievance type", description = "Update a grievance type by its ID for a tenant and business.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GrievanceType updated successfully"),
            @ApiResponse(responseCode = "404", description = "GrievanceType not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> updateGrievanceType(
            @PathVariable String grievanceTypeId,
            @RequestBody GrievanceTypeUpdateRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Updating GrievanceType={} tenant={} business={}", transactionId, grievanceTypeId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            Optional<GrievanceType> updatedType = service.update(grievanceTypeId, request, tenantId, businessId);

            if (updatedType.isPresent()) {
                return ResponseEntity.ok(GrievanceTypeMapper.toResponse(updatedType.get()));
            } else {
                log.warn("TXN={} | GrievanceType={} not found for update tenant={} business={}", transactionId, grievanceTypeId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "GrievanceType not found",
                                "message", "No grievance type found with the given ID: " + grievanceTypeId
                        ));
            }

        } catch (IllegalArgumentException e) {
            log.error("TXN={} | Invalid input while updating GrievanceType={} tenant={} business={} error={}", transactionId, grievanceTypeId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Invalid input",
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("TXN={} | Failed to update GrievanceType={} tenant={} business={} error={}", transactionId, grievanceTypeId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while updating the grievance type"
                    ));
        }
    }

    /**
     * Delete a GrievanceType.
     */
    @DeleteMapping("/{grievanceTypeId}")
    @Operation(summary = "Delete grievance type", description = "Delete a grievance type by ID for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GrievanceType deleted successfully"),
            @ApiResponse(responseCode = "404", description = "GrievanceType not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> deleteGrievanceType(
            @PathVariable String grievanceTypeId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Deleting GrievanceType={} tenant={} business={}", transactionId, grievanceTypeId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            boolean deleted = service.delete(grievanceTypeId, tenantId, businessId);

            if (deleted) {
                log.info("TXN={} | Deleted GrievanceType={} tenant={} business={}", transactionId, grievanceTypeId, tenantId, businessId);
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "GrievanceType deleted successfully",
                        "grievanceTypeId", grievanceTypeId
                ));
            } else {
                log.warn("TXN={} | GrievanceType={} not found for delete tenant={} business={}", transactionId, grievanceTypeId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "FAILED",
                        "errorCode", "GRIEVANCE_TYPE_NOT_FOUND",
                        "message", "No grievance type found with the given ID: " + grievanceTypeId
                ));
            }

        } catch (Exception e) {
            log.error("TXN={} | Error deleting GrievanceType={} tenant={} business={} error={}",
                    transactionId, grievanceTypeId, tenantId, businessId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "errorCode", "DELETE_OPERATION_FAILED",
                    "message", "An unexpected error occurred while deleting the grievance type."
            ));
        }
    }

    /**
     * Count all GrievanceTypes for a tenant and business.
     */
    @GetMapping("/count")
    @Operation(summary = "Count grievance types", description = "Get total number of grievance types for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> countGrievanceTypes(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {
        log.info("TXN={} | Counting GrievanceTypes tenant={} business={}", transactionId, tenantId, businessId);
        try {
            long total = service.count(tenantId, businessId);
            return ResponseEntity.ok(Map.of("total", total));
        } catch (Exception e) {
            log.error("TXN={} | Failed to count GrievanceTypes tenant={} business={} error={}", transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}