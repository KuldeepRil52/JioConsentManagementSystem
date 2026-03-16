package com.jio.digigov.grievance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.constant.HeaderConstants;
import com.jio.digigov.grievance.dto.GrievanceUpdateResponseDto;
import com.jio.digigov.grievance.dto.request.GrievanceCreateRequest;
import com.jio.digigov.grievance.dto.request.GrievanceUpdateRequest;
import com.jio.digigov.grievance.dto.response.GrievanceDetailResponse;
import com.jio.digigov.grievance.dto.response.GrievanceListResponse;
import com.jio.digigov.grievance.dto.response.GrievanceResponse;
import com.jio.digigov.grievance.dto.response.PagedResponse;
import com.jio.digigov.grievance.entity.Grievance;
import com.jio.digigov.grievance.exception.*;
import com.jio.digigov.grievance.mapper.GrievanceMapper;
import com.jio.digigov.grievance.repository.impl.BusinessKeyRepository;
import com.jio.digigov.grievance.service.GrievanceService;
import com.jio.digigov.grievance.service.RequestResponseSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST Controller for managing grievances.
 * Supports CRUD operations with tenant, business, and transaction tracking.
 * All errors are logged and return proper HTTP status codes.
 */
@RestController
@RequestMapping("/api/v1/grievances")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Grievance", description = "Manage grievances with CRUD operations, multi-tenant support, and transaction tracking.")
public class GrievanceController {

    private final GrievanceService service;

    @Autowired
    private BusinessKeyRepository businessKeyRepository;

    @Autowired
    private RequestResponseSignatureService signatureService;

    @Autowired
    private ObjectMapper mapper;

    /**
     * Create a new grievance.
     */
    @PostMapping
    @Operation(summary = "Create grievance", description = "Registers a new grievance for the specified tenant and business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Grievance created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
            @ApiResponse(responseCode = "404", description = "Grievance Template not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> createGrievance(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank String tenantId,

            @Parameter(description = "Business ID", required = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank String businessId,

            @Parameter(description = "Transaction ID", required = true)
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) @NotBlank String transactionId,

            @Parameter(description = "Grievance Template ID", required = true)
            @RequestHeader("X-GRIEVANCE-TEMPLATE-ID") @NotBlank String grievanceTemplateId,

            @Parameter(description = "Grievance creation request", required = true)
            @Valid @RequestBody GrievanceCreateRequest request,

            HttpServletRequest servletRequest) {

        try {
            log.info("Creating grievance | tenantId={} businessId={} transactionId={} grievanceTemplateId={}",
                    tenantId, businessId, transactionId, grievanceTemplateId);

            GrievanceResponse created = service.create(request, tenantId, businessId, grievanceTemplateId, transactionId, servletRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (EntityNotFoundException e) {
            log.warn("Template not found | grievanceTemplateId={} tenantId={} businessId={}", grievanceTemplateId, tenantId, businessId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "GRIEVANCE_TEMPLATE_NOT_FOUND",
                    "errorCode", HttpStatus.NOT_FOUND.value(),
                    "message", e.getMessage()
            ));

        } catch (BusinessException e) {
            log.error("Business error creating grievance | tenantId={} businessId={} errorCode={} message={}",
                    tenantId, businessId, e.getErrorCode(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", e.getErrorCode(),
                    "errorCode", HttpStatus.BAD_REQUEST.value(),
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("Unexpected error creating grievance | tenantId={} businessId={} error={}",
                    tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_SERVER_ERROR",
                    "errorCode", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "An unexpected error occurred while creating the grievance."
            ));
        }
    }


    @GetMapping("/search")
    @Operation(summary = "Search grievances", description = "Search grievances dynamically on ANY field (nested JSON supported)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filtered grievances retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PagedResponse<GrievanceListResponse>> searchGrievances(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        try {
            log.info("Searching grievances | tenantId={} businessId={} txId={} filters={} page={} size={}",
                    tenantId, businessId, transactionId, allParams, page, size);

            PagedResponse<GrievanceListResponse> response =
                    service.search(allParams, tenantId, businessId, page, size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during grievance search | tenantId={} businessId={} txId={} error={}",
                    tenantId, businessId, transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * List grievances with optional pagination.
     */
    @GetMapping
    @Operation(summary = "List grievances", description = "Retrieve list of grievances with optional pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grievances retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PagedResponse<GrievanceListResponse>> listGrievances(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank String tenantId,

            @Parameter(description = "Business ID", required = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank String businessId,

            @Parameter(description = "Transaction ID")
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,

            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        try {
            log.info("Listing grievances | tenantId={} businessId={} transactionId={} page={} size={}", tenantId, businessId, transactionId, page, size);
            PagedResponse<GrievanceListResponse> response = service.list(page, size, tenantId, businessId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing grievances | tenantId={} businessId={} transactionId={} error={}", tenantId, businessId, transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a grievance by ID.
     */
    @GetMapping("/{grievanceId}")
    @Operation(summary = "Get grievance by ID", description = "Retrieve detailed information about a grievance by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grievance retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid grievance ID"),
            @ApiResponse(responseCode = "404", description = "Grievance not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getGrievanceById(
            @RequestHeader("tenant-id") @NotBlank String tenantId,
            @RequestHeader("business-id") @NotBlank String businessId,
            @RequestHeader(value = "transaction-id", required = false) String transactionId,
            @PathVariable String grievanceId) {

        if (transactionId == null || transactionId.isBlank()) {
            transactionId = UUID.randomUUID().toString();
        }

        log.info("Fetching grievance | grievanceId={} | tenantId={} | businessId={} | transactionId={}",
                grievanceId, tenantId, businessId, transactionId);

        if (grievanceId == null || grievanceId.trim().isEmpty()) {
            return ErrorResponseBuilder.buildErrorResponse(
                    "GRV-400",
                    "Grievance ID must not be null or empty.",
                    HttpStatus.BAD_REQUEST,
                    transactionId
            );
        }

        try {
            // Fetch from DB
            Grievance grievance = service.getById(grievanceId, tenantId, businessId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("No grievance found with ID: " + grievanceId));
            log.info("Fetched grievance entity from DB for grievanceId={} : {}", grievanceId, grievance);
            // Map to DTO
            GrievanceDetailResponse response = GrievanceMapper.getByIdResponse(grievance);
            log.info("Mapped grievance entity to response DTO for grievanceId={} : {}", grievanceId, response);
            // Prepare payload for signing (convert response DTO to Map)
//            HashMap<String, Object> payload = mapper.convertValue(response, HashMap.class);
//            log.info("Payload to be signed on get grievance for grievanceId={} : {}", grievanceId, payload);
            // Sign the response → Detached JWS
            String signature = signatureService.signResponse(tenantId, response);
            log.info("Generated JWS signature on get grievance for grievanceId={} : {}", grievanceId, signature);
            return ResponseEntity.ok()
                    .header("x-jws-signature", signature)     // attach signature
                    .body(response);                          // return actual body

        } catch (ResourceNotFoundException e) {
            log.warn("Grievance not found | grievanceId={} | error={}", grievanceId, e.getMessage());
            return ErrorResponseBuilder.buildErrorResponse("GRV-404", e.getMessage(),
                    HttpStatus.NOT_FOUND, transactionId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid grievanceId format | grievanceId={} | error={}", grievanceId, e.getMessage());
            return ErrorResponseBuilder.buildErrorResponse("GRV-400", e.getMessage(),
                    HttpStatus.BAD_REQUEST, transactionId);

        } catch (Exception e) {
            log.error("Unexpected error | grievanceId={} | tenantId={} | businessId={} | transactionId={} | error={}",
                    grievanceId, tenantId, businessId, transactionId, e.getMessage(), e);
            return ErrorResponseBuilder.buildErrorResponse("GRV-500", e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, transactionId);
        }
    }

    /**
     * Get count of grievances with optional search filters.
     */
    @GetMapping("/count")
    @Operation(summary = "Count grievances", description = "Get number of grievances for a tenant and business with optional filters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> countGrievances(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank String tenantId,

            @Parameter(description = "Business ID", required = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank String businessId,

            @Parameter(description = "Transaction ID")
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,

            @Parameter(description = "Optional search filters")
            @RequestParam Map<String, String> allParams
    ) {
        try {
            allParams.put("tenantId", tenantId);
            allParams.put("businessId", businessId);

            log.info("Counting grievances with filters | tenantId={} businessId={} transactionId={} filters={}",
                    tenantId, businessId, transactionId, allParams);

            long total = service.countByFilters(allParams, tenantId, businessId);

            return ResponseEntity.ok(Map.of("total", total));
        } catch (Exception e) {
            log.error("Error counting grievances | tenantId={} businessId={} transactionId={} error={}",
                    tenantId, businessId, transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing grievance.
     */
    @PutMapping("/{grievanceId}")
    @Operation(summary = "Update grievance", description = "Update fields of an existing grievance by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grievance updated successfully"),
            @ApiResponse(responseCode = "404", description = "Grievance not found"),
            @ApiResponse(responseCode = "400", description = "Invalid update request or status transition"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateGrievance(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("tenant-id") @NotBlank String tenantId,

            @Parameter(description = "Business ID", required = true)
            @RequestHeader("business-id") @NotBlank String businessId,

            @Parameter(description = "x-jws-signature", required = false)
            @RequestHeader(value = "x-jws-signature", required = false) String x_jws_signature,

            @Parameter(description = "Resource Type", required = false)
            @RequestHeader(value = "requestor-type", required = false) String requestor_type,

            @Parameter(description = "Transaction ID")
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) @NotBlank String transactionId,

            @PathVariable String grievanceId,
            @Valid @RequestBody GrievanceUpdateRequest request,

            HttpServletRequest servletRequest) {

        log.info("TXN={} | Updating grievanceId={} tenantId={} businessId={}",
                transactionId, grievanceId, tenantId, businessId);

        try {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("tenant-id", tenantId);
            headers.put("business-id", businessId);
            headers.put("x-jws-signature", x_jws_signature);
            // if requestor_type is null or empty, set to DATAFIDUCIARY
            headers.put("requestor-type", requestor_type == null || requestor_type.trim().isEmpty()? "DATA_FIDUCIARY": requestor_type);
            log.info("Headers for signature verification: {}", headers);
            // Prepare payload for signing (convert response DTO to Map)
//            HashMap<String, Object> requestPayload = mapper.convertValue(request, HashMap.class);
//            log.info("Request payload for signature verification for grievanceId={} : {}", grievanceId, requestPayload);
            boolean verifyRequest = signatureService.verifyRequest(request, headers);
            log.info("verifyRequest={}", verifyRequest);
            Grievance updated = service.update(grievanceId, request, tenantId, businessId, transactionId, servletRequest);
            log.info("TXN={} | Grievance updated successfully grievanceId={} updated={}",
                    transactionId, grievanceId, updated);

            GrievanceUpdateResponseDto response = GrievanceUpdateResponseDto.builder()
                    .message("Grievance updated successfully")
                    .grievanceId(updated.getGrievanceId())
                    .grievanceJwtToken(updated.getGrievanceJwtToken())
                    .status(updated.getStatus())
                    .timestamp(LocalDateTime.now())
                    .build();

            // Prepare payload for signing (convert response DTO to Map)
            HashMap<String, Object> payload = mapper.convertValue(response, HashMap.class);
            log.info("Payload to be signed for grievanceId={} : {}", grievanceId, payload);

            // Sign the response → Detached JWS
            String signature = signatureService.signResponse(tenantId, payload);
            log.info("Generated JWS signature for grievanceId={} : {}", grievanceId, signature);


            return ResponseEntity.ok()
                    .header("x-jws-signature", signature)     // attach signature
                    .body(response);

        } catch (ResourceNotFoundException e) {
            log.error("TXN={} | Grievance not found | grievanceId={} | error={}", transactionId, grievanceId, e.getMessage());
            return ErrorResponseBuilder.buildErrorResponse("GRV-404", e.getMessage(), HttpStatus.NOT_FOUND, transactionId);

        } catch (InvalidRequestException e) {
            log.warn("TXN={} | Invalid update request | grievanceId={} | error={}", transactionId, grievanceId, e.getMessage());
            return ErrorResponseBuilder.buildErrorResponse("GRV-400", e.getMessage(), HttpStatus.BAD_REQUEST, transactionId);

        } catch (Exception e) {
            log.error("TXN={} | Unexpected error updating grievanceId={} | error={}", transactionId, grievanceId, e.getMessage(), e);
            return ErrorResponseBuilder.buildErrorResponse("GRV-500", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, transactionId);
        }
    }

    @PutMapping("/{grievanceId}/feedback")
    @Operation(
            summary = "Provide feedback for a grievance",
            description = "Allows users to submit feedback (rating 1–5) for a grievance by ID. Default feedback is 0."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid feedback value"),
            @ApiResponse(responseCode = "404", description = "Grievance not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateFeedback(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank String tenantId,

            @Parameter(description = "Business ID", required = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank String businessId,

            @Parameter(description = "Transaction ID (auto-generated if missing)")
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(description = "Grievance ID", required = true)
            @PathVariable String grievanceId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Feedback payload with rating value (1–5)",
                    required = true
            )
            @Valid @RequestBody Map<String, Integer> requestBody
    ) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            log.debug("Generated new transactionId={} for grievanceId={}", transactionId, grievanceId);
        }

        int feedback = requestBody.getOrDefault("feedback", 0);

        log.info("TXN={} | Received feedback update request grievanceId={} feedback={} tenant={} business={}",
                transactionId, grievanceId, feedback, tenantId, businessId);

        try {
            if (feedback < 1 || feedback > 5) {
                throw new ValidationException("Feedback value must be between 1 and 5");
            }

            Grievance updated = service.updateFeedback(grievanceId, feedback, tenantId, businessId);

            log.info("TXN={} | Feedback successfully updated grievanceId={} rating={}", transactionId, grievanceId, feedback);

            Map<String, Object> response = Map.of(
                    "message", "Feedback submitted successfully",
                    "grievanceId", grievanceId,
                    "feedback", feedback,
                    "status", "SUCCESS",
                    "timestamp", updated.getUpdatedAt(),
                    "transactionId", transactionId
            );

            return ResponseEntity.ok(response);

        } catch (ValidationException ve) {
            log.warn("TXN={} | Validation error grievanceId={} reason={}", transactionId, grievanceId, ve.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation Error",
                    "message", ve.getMessage(),
                    "status", "FAILED",
                    "transactionId", transactionId,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (BusinessException be) {
            log.error("TXN={} | Business exception grievanceId={} reason={}", transactionId, grievanceId, be.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Business Error",
                    "message", be.getMessage(),
                    "status", "FAILED",
                    "transactionId", transactionId,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("TXN={} | Unexpected error updating feedback grievanceId={} error={}",
                    transactionId, grievanceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage(),
                    "status", "FAILED",
                    "transactionId", transactionId,
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * Delete a grievance.
     */
//    @DeleteMapping("/{grievanceId}")
//    @Operation(summary = "Delete grievance", description = "Delete a grievance by its ID.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "204", description = "Grievance deleted successfully"),
//            @ApiResponse(responseCode = "404", description = "Grievance not found"),
//            @ApiResponse(responseCode = "500", description = "Internal server error")
//    })
//    public ResponseEntity<Void> deleteGrievance(
//            @Parameter(description = "Tenant ID", required = true)
//            @RequestHeader(HeaderConstants.X_TENANT_ID) @NotBlank String tenantId,
//
//            @Parameter(description = "Business ID", required = true)
//            @RequestHeader(HeaderConstants.X_BUSINESS_ID) @NotBlank String businessId,
//
//            @Parameter(description = "Transaction ID")
//            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
//
//            @PathVariable String grievanceId) {
//        try {
//            log.info("Deleting grievance | grievanceId={} tenantId={} businessId={} transactionId={}", grievanceId, tenantId, businessId, transactionId);
//            boolean deleted = service.delete(grievanceId, tenantId, businessId);
//            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            log.error("Error deleting grievance | grievanceId={} tenantId={} businessId={} transactionId={} error={}", grievanceId, tenantId, businessId, transactionId, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
}
