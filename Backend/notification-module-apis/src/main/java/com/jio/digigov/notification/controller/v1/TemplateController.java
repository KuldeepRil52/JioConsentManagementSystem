package com.jio.digigov.notification.controller.v1;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.request.template.CreateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.UpdateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.TemplateFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.dto.response.template.CreateTemplateResponseDto;
import com.jio.digigov.notification.dto.response.template.UpdateTemplateResponseDto;
import com.jio.digigov.notification.dto.response.template.TemplateResponseDto;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.constant.DefaultValues;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.exception.ValidationException;
import com.jio.digigov.notification.service.template.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Template Management", description = "Unified SMS and Email template management operations. Create, retrieve, update and delete notification templates with validation and approval workflows.")
public class TemplateController extends BaseController {
    
    private final TemplateService templateService;
    
    @PostMapping
    @Operation(summary = "Create unified SMS and/or Email template", 
              description = "Creates template(s) with auto-approval for specific event type and language")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Template(s) created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Template already exists"),
        @ApiResponse(responseCode = "502", description = "DigiGov service error")
    })
    public ResponseEntity<StandardApiResponseDto<CreateTemplateResponseDto>> createTemplate(
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateTemplateRequestDto request,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level (BUSINESS only for templates)", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel,

            @Parameter(description = "Template type", required = false)
            @RequestHeader(value = "X-Type", required = false, defaultValue = "NOTIFICATION")
            String type) {
        
        log.info("Creating template request received for eventType: {}", request.getEventType());

        // Validate and extract headers
        ScopeLevel scope = parseScopeLevel(scopeLevel);
        NotificationType notificationType = parseNotificationType(type);

        // Business ID is mandatory for template operations
        validateBusinessId(businessId);

        // Only BUSINESS scope supported for templates
        // validateBusinessScope(scope);

        // Extract/generate correlation ID to ensure transactionId is never null
        String correlationId = extractCorrelationId(httpRequest);

        CreateTemplateResponseDto templateResponse = templateService.createTemplate(
            request, tenantId, businessId, scope, notificationType, correlationId, httpRequest);
        StandardApiResponseDto<CreateTemplateResponseDto> response = StandardApiResponseDto.success(
            templateResponse,
            "Template created successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all templates with filtering and pagination", 
              description = "Retrieves templates based on filters and scope level")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<StandardApiResponseDto<PagedResponseDto<TemplateResponseDto>>> getAllTemplates(
            HttpServletRequest httpRequest,
            @ModelAttribute TemplateFilterRequestDto filterRequest,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel) {
        
        log.info("Get all templates request for tenant: {}, business: {}", tenantId, businessId);
        
        ScopeLevel scope = parseScopeLevel(scopeLevel);
        validateBusinessId(businessId);
        // validateBusinessScope(scope);
        
        PagedResponseDto<TemplateResponseDto> templatesResponse = templateService.getAllTemplates(
            filterRequest, tenantId, businessId, scope);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<PagedResponseDto<TemplateResponseDto>> response = StandardApiResponseDto.success(
            templatesResponse,
            "Templates retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{templateId}")
    @Operation(summary = "Get template by ID", 
              description = "Retrieves specific template by its identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<StandardApiResponseDto<TemplateResponseDto>> getTemplateById(
            HttpServletRequest httpRequest,
            @Parameter(description = "Template identifier", required = true)
            @PathVariable String templateId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel) {
        
        log.info("Get template by ID: {} for tenant: {}, business: {}", templateId, tenantId, businessId);
        
        ScopeLevel scope = parseScopeLevel(scopeLevel);
        validateBusinessId(businessId);
        // validateBusinessScope(scope);
        
        TemplateResponseDto templateResponse = templateService.getTemplateById(
            templateId, tenantId, businessId, scope);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<TemplateResponseDto> response = StandardApiResponseDto.success(
            templateResponse,
            "Template retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/digigov/{templateId}")
    @Operation(summary = "Get template by DigiGov template ID", 
              description = "Retrieves specific template by DigiGov template identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<StandardApiResponseDto<TemplateResponseDto>> getByTemplateId(
            HttpServletRequest httpRequest,
            @Parameter(description = "DigiGov template identifier", required = true)
            @PathVariable String templateId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel) {
        
        log.info("Get template by DigiGov template ID: {} for tenant: {}, business: {}", templateId, tenantId, businessId);
        
        ScopeLevel scope = parseScopeLevel(scopeLevel);
        validateBusinessId(businessId);
        // validateBusinessScope(scope);
        
        TemplateResponseDto templateResponse = templateService.getByTemplateId(
            templateId, tenantId, businessId, scope);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<TemplateResponseDto> response = StandardApiResponseDto.success(
            templateResponse,
            "Template retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/digigov/{templateId}")
    @Operation(summary = "Delete template by DigiGov template ID", 
              description = "Deletes specific template by DigiGov template identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<StandardApiResponseDto<Void>> deleteByTemplateId(
            HttpServletRequest httpRequest,
            @Parameter(description = "DigiGov template identifier", required = true)
            @PathVariable String templateId,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel) {
        
        log.info("Delete template by DigiGov template ID: {} for tenant: {}, business: {}", templateId, tenantId, businessId);

        ScopeLevel scope = parseScopeLevel(scopeLevel);
        validateBusinessId(businessId);
        // validateBusinessScope(scope);

        templateService.deleteByTemplateId(templateId, tenantId, businessId, scope, httpRequest);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<Void> response = StandardApiResponseDto.<Void>success(
            "Template deleted successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template by MongoDB ObjectId",
              description = "Updates template by deleting from DB and creating new template in DigiGov with auto-approval. Only content/configuration can be updated, identifying fields remain immutable.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied - only owner can update"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "502", description = "DigiGov service error")
    })
    public ResponseEntity<StandardApiResponseDto<UpdateTemplateResponseDto>> updateTemplateById(
            HttpServletRequest httpRequest,
            @Parameter(description = "MongoDB ObjectId of template to update", required = true)
            @PathVariable String id,
            @Valid @RequestBody UpdateTemplateRequestDto request,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level (BUSINESS only for templates)", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel,

            @Parameter(description = "Template type", required = false)
            @RequestHeader(value = "X-Type", required = false, defaultValue = "NOTIFICATION")
            String type) {

        log.info("Update template by ID request received for id: {}", id);

        // Validate and extract headers
        ScopeLevel scope = parseScopeLevel(scopeLevel);
        NotificationType notificationType = parseNotificationType(type);

        // Business ID is mandatory for template operations
        validateBusinessId(businessId);

        // Extract/generate correlation ID
        String correlationId = extractCorrelationId(httpRequest);

        UpdateTemplateResponseDto updateResponse = templateService.updateTemplateById(
            id, request, tenantId, businessId, scope, notificationType, correlationId, httpRequest);

        StandardApiResponseDto<UpdateTemplateResponseDto> response = StandardApiResponseDto.success(
            updateResponse,
            "Template updated successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/digigov/{templateId}")
    @Operation(summary = "Update template by DigiGov template ID",
              description = "Updates template by deleting from DB and creating new template in DigiGov with auto-approval. Only content/configuration can be updated, identifying fields remain immutable.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied - only owner can update"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "502", description = "DigiGov service error")
    })
    public ResponseEntity<StandardApiResponseDto<UpdateTemplateResponseDto>> updateTemplateByTemplateId(
            HttpServletRequest httpRequest,
            @Parameter(description = "DigiGov template identifier", required = true)
            @PathVariable String templateId,
            @Valid @RequestBody UpdateTemplateRequestDto request,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level (BUSINESS only for templates)", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel,

            @Parameter(description = "Template type", required = false)
            @RequestHeader(value = "X-Type", required = false, defaultValue = "NOTIFICATION")
            String type) {

        log.info("Update template by DigiGov template ID request received for templateId: {}", templateId);

        // Validate and extract headers
        ScopeLevel scope = parseScopeLevel(scopeLevel);
        NotificationType notificationType = parseNotificationType(type);

        // Business ID is mandatory for template operations
        validateBusinessId(businessId);

        // Extract/generate correlation ID
        String correlationId = extractCorrelationId(httpRequest);

        UpdateTemplateResponseDto updateResponse = templateService.updateTemplateByTemplateId(
            templateId, request, tenantId, businessId, scope, notificationType, correlationId, httpRequest);

        StandardApiResponseDto<UpdateTemplateResponseDto> response = StandardApiResponseDto.success(
            updateResponse,
            "Template updated successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @Operation(summary = "Get template count with breakdown", 
              description = "Returns count of templates with optional breakdown by categories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template count retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<StandardApiResponseDto<CountResponseDto>> getTemplateCount(
            HttpServletRequest httpRequest,
            @ModelAttribute TemplateFilterRequestDto filterRequest,

            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String transactionId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,

            @Parameter(description = "Scope level", required = false)
            @RequestHeader(value = HeaderConstants.X_SCOPE_LEVEL, required = false, defaultValue = "BUSINESS")
            String scopeLevel) {
        
        log.info("Get template count for tenant: {}, business: {}", tenantId, businessId);
        
        ScopeLevel scope = parseScopeLevel(scopeLevel);
        validateBusinessId(businessId);
        // validateBusinessScope(scope);
        
        CountResponseDto countResponse = templateService.getTemplateCount(
            filterRequest, tenantId, businessId, scope);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<CountResponseDto> response = StandardApiResponseDto.success(
            countResponse,
            "Template count retrieved successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
    
    // Helper methods for validation and parsing
    private ScopeLevel parseScopeLevel(String scopeLevel) {
        try {
            return ScopeLevel.valueOf(scopeLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid scope level: " + scopeLevel);
        }
    }
    
    private NotificationType parseNotificationType(String type) {
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid notification type: " + type);
        }
    }
    
    private void validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new ValidationException("X-Business-Id header is required for template operations");
        }
    }
    
//    private void // validateBusinessScope(ScopeLevel scope) {
//        if (scope != ScopeLevel.BUSINESS) {
//            throw new ValidationException("Only BUSINESS scope is supported for template operations");
//        }
//    }
}