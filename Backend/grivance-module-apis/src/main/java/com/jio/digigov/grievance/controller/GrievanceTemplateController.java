package com.jio.digigov.grievance.controller;

import com.jio.digigov.grievance.constant.HeaderConstants;
import com.jio.digigov.grievance.dto.ErrorResponse;
import com.jio.digigov.grievance.dto.request.GrievanceTemplateRequest;
import com.jio.digigov.grievance.dto.response.GrievanceTemplateResponse;
import com.jio.digigov.grievance.entity.GrievanceTemplate;
import com.jio.digigov.grievance.enumeration.Status;
import com.jio.digigov.grievance.exception.BusinessException;
import com.jio.digigov.grievance.mapper.GrievanceTemplateMapper;
import com.jio.digigov.grievance.service.GrievanceTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/grievance-templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Grievance Template", description = "Manage grievance templates for different businesses with multilingual and versioning support.")
public class GrievanceTemplateController {

    private final GrievanceTemplateService service;

    /**
     * Create a new Grievance Template.
     */
    @PostMapping
    @Operation(summary = "Create grievance template", description = "Creates a new grievance template for a tenant and business. Versioning handled automatically.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GrievanceTemplate created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> createGrievanceTemplate(
            @Valid @RequestBody GrievanceTemplateRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            HttpServletRequest httpRequest) {

        log.info("TXN={} | Creating GrievanceTemplate name={} business={} tenant={}",
                transactionId, request.getGrievanceTemplateName(), businessId, tenantId);

        try {
            // Convert request to entity
            GrievanceTemplate entity = GrievanceTemplateMapper.toEntity(request);

            // Version handling
            int version = 1;
            if (Status.PUBLISHED.equals(request.getStatus())) {
                version = service.getLatestVersion(businessId, request.getGrievanceTemplateName()) + 1;
            }
            entity.setVersion(version);

            // Save entity
            GrievanceTemplate saved = service.create(entity, tenantId, businessId, transactionId, httpRequest);

            // Return success response
            return ResponseEntity.ok(GrievanceTemplateMapper.toResponse(saved));

        } catch (BusinessException be) {
            // Handle business validation errors
            log.warn("TXN={} | Business validation failed: {}", transactionId, be.getMessage());
            ErrorResponse error = new ErrorResponse(be.getErrorCode(), be.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (IllegalArgumentException iae) {
            // Handle invalid arguments
            log.warn("TXN={} | Invalid request data: {}", transactionId, iae.getMessage());
            ErrorResponse error = new ErrorResponse("INVALID_REQUEST", iae.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            // Handle unexpected server errors
            log.error("TXN={} | Failed to create GrievanceTemplate error={}", transactionId, e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "Failed to create grievance template");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Search grievance templates by filters (component, status, etc.)
     */
    @GetMapping("/search")
    @Operation(summary = "Search grievance templates", description = "Search grievance templates using query parameters for filtering.")
    public ResponseEntity<List<GrievanceTemplateResponse>> searchTemplates(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam Map<String, String> filters) {

        log.info("TXN={} | Searching grievance templates filters={} page={} size={}",
                transactionId, filters, page, size);
        try {
            List<GrievanceTemplateResponse> responses = service
                    .search(tenantId, businessId, filters, page, size)
                    .stream()
                    .map(GrievanceTemplateMapper::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("TXN={} | Search failed error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * List grievance templates with optional pagination.
     */
    @GetMapping("/list")
    @Operation(summary = "List grievance templates (optional pagination)",
            description = "Retrieve grievance templates with optional pagination for a business.")
    public ResponseEntity<List<GrievanceTemplateResponse>> listPaginated(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        log.info("TXN={} | Listing grievance templates | tenantId={} businessId={} page={} size={}",
                transactionId, tenantId, businessId, page, size);
        try {
            List<GrievanceTemplateResponse> responses = service
                    .listByBusinessId(tenantId, businessId, page, size)
                    .stream()
                    .map(GrievanceTemplateMapper::toResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("TXN={} | Failed to list templates | error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update grievance template.
     */
    @PutMapping("/{templateId}")
    @Operation(summary = "Update grievance template", description = "Update an existing grievance template by ID.")
    public ResponseEntity<Object> updateTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody GrievanceTemplateRequest request,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId,
            HttpServletRequest httpRequest) {

        log.info("TXN={} | Updating GrievanceTemplate ID={} for business={}", transactionId, templateId, businessId);
        try {
            GrievanceTemplate updated = service.update(templateId, request, tenantId, businessId, transactionId, httpRequest);
            return ResponseEntity.ok(GrievanceTemplateMapper.toResponse(updated));
        } catch (BusinessException be) {
            // Handle business validation errors
            log.warn("TXN={} | Business validation failed: {}", transactionId, be.getMessage());
            ErrorResponse error = new ErrorResponse(be.getErrorCode(), be.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("TXN={} | Failed to update template error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get a Grievance Template by ID.
     */
    @GetMapping("/{templateId}")
    @Operation(summary = "Get grievance template by ID", description = "Retrieve a grievance template by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GrievanceTemplateResponse> getGrievanceTemplateById(
            @PathVariable String templateId,
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId) {

        log.info("TXN={} | Fetching GrievanceTemplate={} tenant={} business={}", transactionId, templateId, tenantId, businessId);
        try {
            return service.getById(templateId, tenantId, businessId)
                    .map(GrievanceTemplateMapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        log.warn("TXN={} | Template={} not found", transactionId, templateId);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("TXN={} | Error fetching template={} error={}", transactionId, templateId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Count total templates for a business.
     */
    @GetMapping("/count")
    @Operation(summary = "Count grievance templates", description = "Get total number of grievance templates for a business.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> countTemplates(
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @RequestHeader(HeaderConstants.X_TRANSACTION_ID) String transactionId) {

        log.info("TXN={} | Counting templates for tenant={} business={}", transactionId, tenantId, businessId);
        try {
            long total = service.count(tenantId, businessId);
            return ResponseEntity.ok(Map.of("total", total));
        } catch (Exception e) {
            log.error("TXN={} | Failed to count templates error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
