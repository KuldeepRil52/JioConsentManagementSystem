package com.jio.digigov.grievance.controller;

import com.jio.digigov.grievance.constant.HeaderConstants;
import com.jio.digigov.grievance.dto.request.UserDetailCreateRequest;
import com.jio.digigov.grievance.dto.request.UserDetailUpdateRequest;
import com.jio.digigov.grievance.dto.response.UserDetailResponse;
import com.jio.digigov.grievance.entity.UserDetail;
import com.jio.digigov.grievance.enumeration.ScopeLevel;
import com.jio.digigov.grievance.mapper.UserDetailMapper;
import com.jio.digigov.grievance.service.UserDetailService;
import com.jio.digigov.grievance.util.ScopeValidationUtils;
import io.swagger.v3.oas.annotations.Operation;
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
 * REST Controller for managing User Details with multi-tenant support.
 */
@RestController
@RequestMapping("/api/v1/user-details")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Detail", description = "Manage user details for different tenants and businesses.")
public class UserDetailController {

    private final UserDetailService service;

    @PostMapping
    @Operation(summary = "Create user detail", description = "Creates a new user detail for a tenant and business.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "UserDetail created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or duplicate entry"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> createUserDetail(
            @Valid @RequestBody UserDetailCreateRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Creating UserDetail name={} tenant={} business={}",
                transactionId, request.getName(), tenantId, businessId);

        try {
            UserDetail saved = service.create(UserDetailMapper.toEntity(request), tenantId, businessId, scopeLevel);
            return ResponseEntity.ok(UserDetailMapper.toResponse(saved));

        } catch (IllegalArgumentException e) {
            log.warn("TXN={} | Duplicate name while creating UserDetail tenant={} business={} error={}",
                    transactionId, tenantId, businessId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Duplicate entry",
                            "message", "A user with the same name already exists for this business. Please choose a different name."
                    ));

        } catch (Exception e) {
            log.error("TXN={} | Failed to create UserDetail tenant={} business={} error={}",
                    transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while creating the user detail"
                    ));
        }
    }

    @GetMapping
    @Operation(summary = "List user details", description = "Retrieve all user details for a tenant and business based on scope level.")
    public ResponseEntity<List<UserDetailResponse>> listUserDetails(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Listing all UserDetails tenant={} business={} scope={}", transactionId, tenantId, businessId, scopeLevel);

        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            List<UserDetail> details = service.list(tenantId, businessId, scopeLevel);
            List<UserDetailResponse> responses = details.stream()
                    .map(UserDetailMapper::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("TXN={} | Failed to list UserDetails tenant={} business={} error={}", transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{userDetailId}")
    @Operation(summary = "Get user detail by ID", description = "Retrieve a user detail by ID for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "UserDetail fetched successfully"),
            @ApiResponse(responseCode = "404", description = "UserDetail not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> getUserDetailById(
            @PathVariable String userDetailId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Fetching UserDetail={} tenant={} business={}",
                transactionId, userDetailId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            Optional<UserDetail> userDetailOpt = service.getById(userDetailId, tenantId, businessId);

            if (userDetailOpt.isPresent()) {
                return ResponseEntity.ok(UserDetailMapper.toResponse(userDetailOpt.get()));
            } else {
                log.warn("TXN={} | UserDetail={} not found tenant={} business={}",
                        transactionId, userDetailId, tenantId, businessId);

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "UserDetail not found",
                                "message", "No user detail found with the given ID: " + userDetailId
                        ));
            }

        } catch (IllegalArgumentException e) {
            log.error("TXN={} | Invalid input for UserDetail={} tenant={} business={} error={}",
                    transactionId, userDetailId, tenantId, businessId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Invalid input",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("TXN={} | Failed to fetch UserDetail={} tenant={} business={} error={}",
                    transactionId, userDetailId, tenantId, businessId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while fetching the user detail"
                    ));
        }
    }

    @PutMapping("/{userDetailId}")
    @Operation(summary = "Update user detail", description = "Update a user detail by ID for a tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "UserDetail updated successfully"),
            @ApiResponse(responseCode = "404", description = "UserDetail not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> updateUserDetail(
            @PathVariable String userDetailId,
            @RequestBody UserDetailUpdateRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Updating UserDetail={} tenant={} business={}",
                transactionId, userDetailId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            Optional<UserDetail> updatedUserDetail = service.update(userDetailId, request, tenantId, businessId);

            if (updatedUserDetail.isPresent()) {
                return ResponseEntity.ok(UserDetailMapper.toResponse(updatedUserDetail.get()));
            } else {
                log.warn("TXN={} | UserDetail={} not found for update tenant={} business={}",
                        transactionId, userDetailId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "UserDetail not found",
                                "message", "No user detail found with the given ID: " + userDetailId
                        ));
            }

        } catch (IllegalArgumentException e) {
            log.error("TXN={} | Invalid input while updating UserDetail={} tenant={} business={} error={}",
                    transactionId, userDetailId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Invalid input",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("TXN={} | Failed to update UserDetail={} tenant={} business={} error={}",
                    transactionId, userDetailId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An unexpected error occurred while updating the user detail"
                    ));
        }
    }

    @DeleteMapping("/{userDetailId}")
    @Operation(summary = "Delete user detail", description = "Delete a user detail by ID for a tenant and business.")
    public ResponseEntity<Map<String, String>> deleteUserDetail(
            @PathVariable String userDetailId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Deleting UserDetail={} tenant={} business={}",
                transactionId, userDetailId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            boolean deleted = service.delete(userDetailId, tenantId, businessId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "User detail deleted successfully",
                        "userDetailId", userDetailId
                ));
            } else {
                log.warn("TXN={} | UserDetail={} not found for delete tenant={} business={}",
                        transactionId, userDetailId, tenantId, businessId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "FAILED",
                        "errorCode", "USER_DETAIL_NOT_FOUND",
                        "message", "No user detail found with the given ID: " + userDetailId
                ));
            }
        } catch (Exception e) {
            log.error("TXN={} | Failed to delete UserDetail={} tenant={} business={} error={}",
                    transactionId, userDetailId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "errorCode", "DELETE_OPERATION_FAILED",
                    "message", "An unexpected error occurred while deleting the user detail."
            ));
        }
    }

    @GetMapping("/count")
    @Operation(summary = "Count user details", description = "Get total number of user details for a tenant and business.")
    public ResponseEntity<Map<String, Long>> countUserDetails(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestHeader(HeaderConstants.X_SCOPE_LEVEL) ScopeLevel scopeLevel) {

        log.info("TXN={} | Counting UserDetails tenant={} business={}", transactionId, tenantId, businessId);
        ScopeValidationUtils.validateScope(tenantId, businessId, scopeLevel);
        try {
            long total = service.count(tenantId, businessId);
            return ResponseEntity.ok(Map.of("total", total));
        } catch (Exception e) {
            log.error("TXN={} | Failed to count UserDetails tenant={} business={} error={}",
                    transactionId, tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
