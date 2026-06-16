package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.dto.request.EmailTemplateRequestDto;
import com.jio.digigov.notification.dto.request.SMSTemplateRequestDto;
import com.jio.digigov.notification.dto.response.TemplateCreationResponseDto;
import com.jio.digigov.notification.service.LegacyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy REST Controller for Template Management
 * Provides separate endpoints for SMS and Email template creation with DigiGov integration
 * Note: This is legacy - use /v1/templates for new unified API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Template Management (Legacy)", description = "Legacy APIs for SMS and Email template onboarding")
public class LegacyTemplateController extends BaseController {
    
    private final LegacyTemplateService templateService;
    
    public LegacyTemplateController(@Qualifier("legacyTemplateService") LegacyTemplateService templateService) {
        this.templateService = templateService;
    }
    
    /**
     * Create SMS Template with DigiGov onboarding
     * 
     * Flow: Get Config → Generate Token → Onboard → Save DB → Approve → Update Status
     */
    @Operation(
        summary = "Create SMS Template",
        description = "Creates and onboards SMS template to DigiGov with automatic approval"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SMS template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Configuration not found"),
        @ApiResponse(responseCode = "502", description = "DigiGov service error"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/sms")
    public ResponseEntity<TemplateCreationResponseDto> createSMSTemplate(
            @Valid @RequestBody SMSTemplateRequestDto request,
            @Parameter(hidden = true) @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @Parameter(hidden = true) @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @Parameter(description = "Scope Level (TENANT/BUSINESS)")
            @RequestHeader(HeaderConstants.SCOPE_LEVEL) String scopeLevelHeader,
            @Parameter(description = "Type (NOTIFICATION/OTPVALIDATOR)")
            @RequestHeader(HeaderConstants.TYPE) String typeHeader,
            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String txn,
            HttpServletRequest httpRequest) {

        // Extract and validate headers
        ScopeLevel scopeLevel = extractScopeLevel(httpRequest);
        NotificationType notificationType = extractNotificationType(httpRequest);
        String transactionId = extractTransactionId(httpRequest);

        log.info("Creating SMS template - tenantId: {}, businessId: {}, scope: {}, type: {}, txn: {}",
                tenantId, businessId, scopeLevel, notificationType, transactionId);
        log.info("SMS template request: {}", request);
        // Create template with full onboarding flow
        TemplateCreationResponseDto response = templateService.createSMSTemplate(
            request, tenantId, businessId, scopeLevel, notificationType, transactionId);
        
        log.info("SMS template created successfully - templateId: {}, status: {}, txn: {}", 
                response.getTemplateId(), response.getStatus(), transactionId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create Email Template with DigiGov onboarding
     * 
     * Flow: Get Config → Generate Token → Onboard → Save DB → Approve → Update Status
     */
    @Operation(
        summary = "Create Email Template",
        description = "Creates and onboards Email template to DigiGov with automatic approval"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Configuration not found"),
        @ApiResponse(responseCode = "502", description = "DigiGov service error"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/email")
    public ResponseEntity<TemplateCreationResponseDto> createEmailTemplate(
            @Valid @RequestBody EmailTemplateRequestDto request,
            @Parameter(hidden = true) @RequestHeader(HeaderConstants.X_TENANT_ID) String tenantId,
            @Parameter(hidden = true) @RequestHeader(HeaderConstants.X_BUSINESS_ID) String businessId,
            @Parameter(description = "Scope Level (TENANT/BUSINESS)")
            @RequestHeader(HeaderConstants.SCOPE_LEVEL) String scopeLevelHeader,
            @Parameter(description = "Type (NOTIFICATION/OTPVALIDATOR)")
            @RequestHeader(HeaderConstants.TYPE) String typeHeader,
            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.X_TRANSACTION_ID, required = false) String txn,
            HttpServletRequest httpRequest) {
        
        // Extract and validate headers
        ScopeLevel scopeLevel = extractScopeLevel(httpRequest);
        NotificationType notificationType = extractNotificationType(httpRequest);
        String transactionId = extractTransactionId(httpRequest);
        
        log.info("Creating Email template - tenantId: {}, businessId: {}, scope: {}, type: {}, txn: {}", 
                tenantId, businessId, scopeLevel, notificationType, transactionId);
        log.info("Email template request: {}", request);
        // Create template with full onboarding flow
        TemplateCreationResponseDto response = templateService.createEmailTemplate(
            request, tenantId, businessId, scopeLevel, notificationType, transactionId);
        
        log.info("Email template created successfully - templateId: {}, status: {}, txn: {}", 
                response.getTemplateId(), response.getStatus(), transactionId);
        
        return ResponseEntity.ok(response);
    }
}