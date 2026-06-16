package com.example.scanner.controller;

import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.request.CreateTemplateRequest;
import com.example.scanner.dto.request.UpdateTemplateRequest;
import com.example.scanner.dto.response.ErrorResponse;
import com.example.scanner.dto.response.TemplateResponse;
import com.example.scanner.dto.response.TemplateWithCookiesResponse;
import com.example.scanner.dto.response.UpdateTemplateResponse;
import com.example.scanner.entity.ConsentTemplate;
import com.example.scanner.exception.ConsentException;
import com.example.scanner.service.ConsentTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cookie-templates")
@Tag(name = "Consent Template", description = "Consent templates linked to completed cookie scans")
@Slf4j
public class ConsentTemplateController {

    @Autowired
    private ConsentTemplateService service;

    @Operation(
            summary = "Create consent template",
            description = """
                Creates consent template linked to completed scan. ScanId must exist with COMPLETED status.
                
                Error Codes: R4001 (Validation), R4041 (Scan not found), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", required = true, example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "business-id", description = "Business ID", required = true, example = "b1c2d3e4-f5g6-7890-1234-567890abcdef")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CreateTemplateRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Template created successfully",
                            content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation failed",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Scan not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping
    public ResponseEntity<?> createTemplate(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("business-id") String businessId,
            @Valid @RequestBody CreateTemplateRequest createRequest,
            BindingResult bindingResult) {

        try {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                        "Tenant ID is required", "X-Tenant-ID header is missing", "/cookie-templates");
            }

            if (bindingResult.hasErrors()) {
                return handleValidationErrors(bindingResult, "/cookie-templates");
            }

            ConsentTemplate createdTemplate = service.createTemplate(tenantId, createRequest, businessId);
            TemplateResponse response = new TemplateResponse(createdTemplate.getTemplateId(),
                    "Template created successfully and linked to scan: " + createdTemplate.getScanId());

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                    "Validation failed", e.getMessage(), "/cookie-templates");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND,
                        "Scan not found", e.getMessage(), "/cookie-templates");
            }
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                    "Failed to create template", e.getMessage(), "/cookie-templates");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                    "Failed to create template", e.getMessage(), "/cookie-templates");
        }
    }

    @Operation(
            summary = "Get templates with filters",
            description = "Retrieves templates for tenant. Supports optional filtering by businessId, scanId, templateId",
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", required = true, example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "businessId", description = "Business ID (optional)", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "scanId", description = "Scan ID (optional)", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "Template ID (optional)", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Templates retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = TemplateWithCookiesResponse.class))
                            )),
                    @ApiResponse(responseCode = "400", description = "Invalid tenant ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "No templates found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("/tenant")
    public ResponseEntity<?> getTemplatesByTenantAndScanId(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "scanId", required = false) String scanId,
            @RequestParam(value = "templateId", required = false) String templateId) {

        try {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                        "Tenant ID is required", "X-Tenant-ID header is missing", "/cookie-templates/tenant");
            }

            List<TemplateWithCookiesResponse> templates = service.getTemplateWithCookies(tenantId, businessId, scanId, templateId);

            if (templates.isEmpty()) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND,
                        "No templates found", "No templates found for given criteria", "/cookie-templates/tenant");
            }

            return ResponseEntity.ok(templates);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                    "Failed to retrieve templates", e.getMessage(), "/cookie-templates/tenant");
        }
    }

    @PutMapping("/{templateId}/update")
    @Operation(
            summary = "Update template (creates new version)",
            description = """
                Creates new version of template. TemplateId remains same, version increments.
                
                Error Codes: R4001 (Validation), R4041 (Not found), R4221 (Cannot update), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "business-id", description = "Business ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "Template ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template updated successfully",
                            content = @Content(schema = @Schema(implementation = UpdateTemplateResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Cannot update template",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<?> updateTemplate(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("business-id") String businessId,
            @PathVariable String templateId,
            @org.springframework.web.bind.annotation.RequestBody @Valid UpdateTemplateRequest updateRequest) {

        try {
            UpdateTemplateResponse response = service.updateTemplate(tenantId, templateId, businessId, updateRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                    "Validation failed", e.getMessage(), "/cookie-templates/" + templateId + "/update");
        } catch (IllegalStateException | ConsentException e) {
            return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.BUSINESS_RULE_VIOLATION,
                    "Cannot update template", e.getMessage(), "/cookie-templates/" + templateId + "/update");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                    "Failed to update template", e.getMessage(), "/cookie-templates/" + templateId + "/update");
        }
    }

    @GetMapping("/{templateId}/history")
    @Operation(
            summary = "Get template version history",
            description = "Retrieves all versions of template (latest first). Shows complete audit trail",
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "Template ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "History retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ConsentTemplate.class))
                            )),
                    @ApiResponse(responseCode = "400", description = "Invalid template ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Template not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<List<ConsentTemplate>> getTemplateHistory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String templateId) {

        try {
            List<ConsentTemplate> history = service.getTemplateHistory(tenantId, templateId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{templateId}/versions/{version}")
    @Operation(
            summary = "Get specific template version",
            description = "Retrieves specific version of template",
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "Template ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "version", description = "Version number", example = "2")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Version retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ConsentTemplate.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid ID or version",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Version not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<?> getTemplateVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String templateId,
            @PathVariable Integer version) {

        try {
            Optional<ConsentTemplate> templateOpt = service.getTemplateByIdAndVersion(tenantId, templateId, version);
            if (templateOpt.isEmpty()) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND,
                        "Version not found", "No template found with ID '" + templateId + "' and version " + version,
                        "/cookie-templates/" + templateId + "/versions/" + version);
            }
            return ResponseEntity.ok(templateOpt.get());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                    "Invalid parameters", e.getMessage(), "/cookie-templates/" + templateId + "/versions/" + version);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                    "Failed to retrieve version", e.getMessage(), "/cookie-templates/" + templateId + "/versions/" + version);
        }
    }

    private ResponseEntity<ErrorResponse> handleValidationErrors(BindingResult bindingResult, String path) {
        List<String> errors = bindingResult.getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError) {
                        FieldError fieldError = (FieldError) error;
                        return fieldError.getField() + ": " + error.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        String errorMessage = "Validation failed: " + String.join(", ", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "Request validation failed", errorMessage, path);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String errorCode,
                                                             String message, String details, String path) {
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, details, Instant.now(), path);
        return ResponseEntity.status(status).body(errorResponse);
    }
}