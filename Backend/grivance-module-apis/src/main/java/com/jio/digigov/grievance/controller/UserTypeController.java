package com.jio.digigov.grievance.controller;

import com.jio.digigov.grievance.constant.HeaderConstants;
import com.jio.digigov.grievance.dto.request.UserTypeCreateRequest;
import com.jio.digigov.grievance.dto.request.UserTypeUpdateRequest;
import com.jio.digigov.grievance.dto.response.UserTypeResponse;
import com.jio.digigov.grievance.entity.UserType;
import com.jio.digigov.grievance.enumeration.ScopeLevel;
import com.jio.digigov.grievance.mapper.UserTypeMapper;
import com.jio.digigov.grievance.service.UserTypeService;
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
 * REST Controller for managing User Types with multi-tenant support.
 * Supports CRUD operations with transaction logging and error handling.
 */
@RestController
@RequestMapping("/api/v1/user-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Type", description = "Manage user types for different tenants and businesses.")
public class UserTypeController {

    private final UserTypeService service;

    /**
     * Create a new UserType.
     */
    @PostMapping
    @Operation(summary = "Create user type", description = "Creates a new user type for a tenant and business.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "UserType created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or duplicate entry"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> createUserType(
            @Parameter(description = "UserType creation request", required = true)
            @Valid @RequestBody UserTypeCreateRequest request,

            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(description = "Business ID", required = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Transaction ID", required = true)
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,

            @Parameter(description = "Scope Level", required = true)
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Creating UserType name={} tenant={} business={}", transactionId, request.getName(), tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            UserType saved = service.create(UserTypeMapper.toEntity(request), tenantId, businessId, scopeLevel);
            return ResponseEntity.ok(UserTypeMapper.toResponse(saved));

        } catch (IllegalArgumentException e) {
            log.warn("TXN={} | Duplicate name while creating UserType tenant={} business={} error={}",
                    transactionId, tenantId, businessId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Duplicate entry",
                            "message", "A user type with the same name already exists for this business. Please choose a different name."
                    ));

        } catch (Exception e) {
            log.error("TXN={} | Failed to create UserType tenant={} business={} error={}",
                    transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while creating the user type"
                    ));
        }
    }

    /**
     * List all UserTypes for a tenant and business.
     */
    @GetMapping
    @Operation(summary = "List user types", description = "Retrieve all user types for a tenant and business based on scope level.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "UserTypes retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<UserTypeResponse>> listUserTypes(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Listing all UserTypes tenant={} business={} scope={}", transactionId, tenantId, businessId, scopeLevel);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            List<UserType> types = service.list(tenantId, businessId, scopeLevel);
            List<UserTypeResponse> responses = types.stream()
                    .map(UserTypeMapper::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("TXN={} | Failed to list UserTypes tenant={} business={} error={}", transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get a UserType by ID.
     */
    @GetMapping("/{userTypeId}")
    @Operation(summary = "Get user type by ID", description = "Retrieve a user type by its ID for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "UserType retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "UserType not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> getUserTypeById(
            @PathVariable String userTypeId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Fetching UserType={} tenant={} business={}",
                transactionId, userTypeId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            Optional<UserType> userTypeOpt = service.getById(userTypeId, tenantId, businessId,scopeLevel);

            if (userTypeOpt.isPresent()) {
                return ResponseEntity.ok(UserTypeMapper.toResponse(userTypeOpt.get()));
            } else {
                log.warn("TXN={} | UserType={} not found tenant={} business={}",
                        transactionId, userTypeId, tenantId, businessId);

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "UserType not found",
                                "message", "No user type found with the given ID: " + userTypeId
                        ));
            }

        } catch (IllegalArgumentException e) {
            log.error("TXN={} | Invalid input while fetching UserType={} tenant={} business={} error={}",
                    transactionId, userTypeId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Invalid input",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("TXN={} | Failed to fetch UserType={} tenant={} business={} error={}",
                    transactionId, userTypeId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while fetching the user type"
                    ));
        }
    }

    /**
     * Update an existing UserType.
     */
    @PutMapping("/{userTypeId}")
    @Operation(summary = "Update user type", description = "Update a user type by its ID for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "UserType updated successfully"),
            @ApiResponse(responseCode = "404", description = "UserType not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> updateUserType(
            @PathVariable String userTypeId,
            @RequestBody UserTypeUpdateRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Updating UserType={} tenant={} business={}",
                transactionId, userTypeId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            // Validate input ID before processing
            if (userTypeId == null || userTypeId.trim().isEmpty()) {
                log.warn("TXN={} | Invalid userTypeId provided for update tenant={} business={}",
                        transactionId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "error", "Invalid input",
                                "message", "UserType ID must not be null or empty"
                        ));
            }

            Optional<UserType> updatedUserType = service.update(userTypeId, request, tenantId, businessId);

            if (updatedUserType.isPresent()) {
                return ResponseEntity.ok(UserTypeMapper.toResponse(updatedUserType.get()));
            } else {
                log.warn("TXN={} | UserType={} not found for update tenant={} business={}",
                        transactionId, userTypeId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "UserType not found",
                                "message", "No user type found with the given ID: " + userTypeId
                        ));
            }

        } catch (IllegalArgumentException e) {
            log.error("TXN={} | Invalid input while updating UserType={} tenant={} business={} error={}",
                    transactionId, userTypeId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Invalid input",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("TXN={} | Failed to update UserType={} tenant={} business={} error={}",
                    transactionId, userTypeId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while updating the user type"
                    ));
        }
    }

    /**
     * Delete a UserType by ID.
     */
    @DeleteMapping("/{userTypeId}")
    @Operation(summary = "Delete user type", description = "Delete a user type by ID for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "UserType deleted successfully"),
            @ApiResponse(responseCode = "404", description = "UserType not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> deleteUserType(
            @PathVariable String userTypeId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Deleting UserType={} tenant={} business={}", transactionId, userTypeId, tenantId, businessId);

        try {
            boolean deleted = service.delete(userTypeId, tenantId, businessId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "UserType deleted successfully",
                        "userTypeId", userTypeId
                ));
            } else {
                log.warn("TXN={} | UserType={} not found for delete tenant={} business={}", transactionId, userTypeId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "FAILED",
                        "errorCode", "USER_TYPE_NOT_FOUND",
                        "message", "No user type found with the given ID: " + userTypeId
                ));
            }
        } catch (Exception e) {
            log.error("TXN={} | Failed to delete UserType={} tenant={} business={} error={}",
                    transactionId, userTypeId, tenantId, businessId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "errorCode", "DELETE_OPERATION_FAILED",
                    "message", "An unexpected error occurred while deleting the user type."
            ));
        }
    }

    /**
     * Count all UserTypes for a tenant and business.
     */
    @GetMapping("/count")
    @Operation(summary = "Count user types", description = "Get total number of user types for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> countUserTypes(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Counting UserTypes tenant={} business={}", transactionId, tenantId, businessId);

        try {
            long total = service.count(tenantId, businessId);
            return ResponseEntity.ok(Map.of("total", total));
        } catch (Exception e) {
            log.error("TXN={} | Failed to count UserTypes tenant={} business={} error={}", transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
