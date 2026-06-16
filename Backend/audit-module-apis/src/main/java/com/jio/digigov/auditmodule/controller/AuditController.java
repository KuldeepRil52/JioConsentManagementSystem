package com.jio.digigov.auditmodule.controller;

import com.jio.digigov.auditmodule.dto.AuditRecordResponse;
import com.jio.digigov.auditmodule.dto.AuditRequest;
import com.jio.digigov.auditmodule.dto.AuditResponse;
import com.jio.digigov.auditmodule.dto.PagedResponse;
import com.jio.digigov.auditmodule.service.AuditService;
import com.jio.digigov.auditmodule.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/audit")
@Tag(name = "Audit API", description = "APIs for creating and retrieving audits")
public class AuditController extends BaseController{

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Create a new audit record.
     */
    @Operation(summary = "Create a new Audit event",
            description = "Stores a new audit record in the database for a given tenant and business")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Audit created successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error while creating audit")
    })
    @PostMapping
    public ResponseEntity<AuditResponse> createAudit(
            @Parameter(description = "Tenant ID (passed via header)", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Transaction ID (passed via header)", required = true)
            @RequestHeader("X-Transaction-ID") String transactionId,
            @Valid @RequestBody AuditRequest request) {

        log.info("Received request to create audit: businessId={}, tenantId={}", request.getBusinessId(), tenantId);

        if(request.getBusinessId() == null) {
            log.warn("Business ID is missing in the audit request for tenantId={}", tenantId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuditResponse.builder()
                            .status("failure")
                            .message("Business is required")
                            .build());
        }
        try {
            AuditResponse response = auditService.createAudit(request, tenantId, transactionId);

            if ("failure".equalsIgnoreCase(response.getStatus())) {
                log.warn("Audit creation failed for tenantId={} businessId={} message={}",
                        tenantId, request.getBusinessId(), response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            log.info("Audit created successfully tenantId={} businessId={}", tenantId, request.getBusinessId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating audit for tenantId={}, businessId={}, error={}",
                    tenantId, request.getBusinessId(), e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuditResponse.builder()
                            .status("failure")
                            .message("Failed to create audit: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Fetch audits with dynamic filters, sorting, and pagination.
     */
    @Operation(
            summary = "Get audits with filters, sorting, and pagination",
            description = "Fetch audit records by dynamic filters with optional sorting and pagination"
    )
    @ApiResponse(responseCode = "200", description = "Fetched audits successfully")
    @GetMapping
    public ResponseEntity<PagedResponse<AuditRecordResponse>> getAudits(
            @Parameter(description = "Tenant ID (passed via header)", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Business ID (passed via header)", required = true)
            @RequestHeader("X-Business-ID") String businessId,
            @Parameter(description = "Transaction ID (passed via header)", required = false)
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId,
            @RequestParam Map<String, String> params,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "asc") String sort
    ) {
        try {
            params.put("tenantId", tenantId);
            params.put("sort", sort);

            log.info("Fetching audits tenantId={} businessId={} filters={} page={} size={} sort={}",
                    tenantId, businessId, params, page, size, sort);

            PagedResponse<AuditRecordResponse> audits = auditService.getAuditsPaged(params, businessId, page, size, sort);

            return ResponseEntity.ok(audits);

        } catch (Exception e) {
            log.error("Error fetching audits tenantId={}, businessId={}, error={}",
                    tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the count of audit records with filters.
     */
    @Operation(summary = "Get audit count with filters",
            description = "Fetch count of audit records by dynamic filters")
    @ApiResponse(responseCode = "200", description = "Fetched count successfully")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getAuditCount(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-Business-ID") String businessId,
            @Parameter(description = "Transaction ID (passed via header)", required = false)
            @RequestHeader("X-Transaction-ID") String transactionId,
            @RequestParam Map<String, String> params) {

        try {
            params.put("tenantId", tenantId);

            log.info("Counting audits tenantId={}, businessId={}, filters={}", tenantId, businessId, params);

            long count = auditService.countAudits(params, businessId);

            Map<String, Long> response = new HashMap<>();
            response.put("count", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error counting audits tenantId={}, businessId={}, error={}",
                    tenantId, businessId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}