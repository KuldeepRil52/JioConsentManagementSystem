package com.jio.digigov.notification.service.template;

import com.jio.digigov.notification.dto.request.template.CreateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.UpdateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.TemplateFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.template.CreateTemplateResponseDto;
import com.jio.digigov.notification.dto.response.template.UpdateTemplateResponseDto;
import com.jio.digigov.notification.dto.response.template.TemplateResponseDto;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface TemplateService {
    
    /**
     * Creates a unified template with SMS and/or Email configurations
     * @param request Template creation request with SMS/Email configs
     * @param tenantId Tenant identifier
     * @param businessId Business identifier (required)
     * @param scopeLevel Always BUSINESS for templates
     * @param type NOTIFICATION or OTPVALIDATOR
     * @param transactionId Transaction identifier for tracking
     * @param httpRequest HTTP servlet request for audit logging
     * @return Template creation response
     */
    CreateTemplateResponseDto createTemplate(CreateTemplateRequestDto request,
                                        String tenantId,
                                        String businessId,
                                        ScopeLevel scopeLevel,
                                        NotificationType type,
                                        String transactionId,
                                        HttpServletRequest httpRequest);
    
    /**
     * Retrieves templates with filtering and pagination
     * @param request Filter and pagination parameters
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level (BUSINESS only for templates)
     * @return Paginated template list
     */
    PagedResponseDto<TemplateResponseDto> getAllTemplates(TemplateFilterRequestDto request, 
                                                   String tenantId, 
                                                   String businessId, 
                                                   ScopeLevel scopeLevel);
    
    /**
     * Retrieves specific template by ID
     * @param templateId Template identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level for access validation
     * @return Template details
     */
    TemplateResponseDto getTemplateById(String templateId, 
                                   String tenantId, 
                                   String businessId, 
                                   ScopeLevel scopeLevel);
    
    /**
     * Gets count of templates with optional breakdown
     * @param request Filter parameters
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level
     * @return Count response with breakdown
     */
    CountResponseDto getTemplateCount(TemplateFilterRequestDto request, 
                                  String tenantId, 
                                  String businessId, 
                                  ScopeLevel scopeLevel);
    
    /**
     * Retrieves template by DigiGov template ID
     * @param templateId DigiGov template identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level for access validation
     * @return Template details
     */
    TemplateResponseDto getByTemplateId(String templateId, 
                                   String tenantId, 
                                   String businessId, 
                                   ScopeLevel scopeLevel);
    
    /**
     * Deletes template by DigiGov template ID
     * @param templateId DigiGov template identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level for access validation
     * @param httpRequest HTTP servlet request for audit logging
     */
    void deleteByTemplateId(String templateId,
                           String tenantId,
                           String businessId,
                           ScopeLevel scopeLevel,
                           HttpServletRequest httpRequest);

    /**
     * Updates template by MongoDB ObjectId
     * Deletes existing template from DB, creates new template in DigiGov with auto-approval
     * @param id MongoDB ObjectId
     * @param request Update template request with new content/configuration
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level for access validation
     * @param type NOTIFICATION or OTPVALIDATOR
     * @param transactionId Transaction identifier for tracking
     * @param httpRequest HTTP servlet request for audit logging
     * @return Update template response with old and new templateIds
     */
    UpdateTemplateResponseDto updateTemplateById(String id,
                                                UpdateTemplateRequestDto request,
                                                String tenantId,
                                                String businessId,
                                                ScopeLevel scopeLevel,
                                                NotificationType type,
                                                String transactionId,
                                                HttpServletRequest httpRequest);

    /**
     * Updates template by DigiGov template ID
     * Deletes existing template from DB, creates new template in DigiGov with auto-approval
     * @param templateId DigiGov template identifier
     * @param request Update template request with new content/configuration
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level for access validation
     * @param type NOTIFICATION or OTPVALIDATOR
     * @param transactionId Transaction identifier for tracking
     * @param httpRequest HTTP servlet request for audit logging
     * @return Update template response with old and new templateIds
     */
    UpdateTemplateResponseDto updateTemplateByTemplateId(String templateId,
                                                         UpdateTemplateRequestDto request,
                                                         String tenantId,
                                                         String businessId,
                                                         ScopeLevel scopeLevel,
                                                         NotificationType type,
                                                         String transactionId,
                                                         HttpServletRequest httpRequest);

    /**
     * Resolves template content with dynamic arguments
     * @param templateId Template identifier
     * @param arguments Template arguments for placeholder substitution
     * @param tenantId Tenant identifier
     * @return Resolved template content
     */
    String resolveTemplate(String templateId, Map<String, Object> arguments, String tenantId);

    /**
     * Validates template existence for event trigger with language fallback
     * @param eventType Event type identifier
     * @param channelType Channel type (SMS, EMAIL)
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param language Preferred language (with fallback to business default then English)
     * @return Template ID if found, throws exception if not found
     */
    String validateTemplateExists(String eventType, String channelType, String tenantId,
                                String businessId, String language);
}