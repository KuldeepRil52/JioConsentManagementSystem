package com.jio.digigov.notification.service;

import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.dto.request.EmailTemplateRequestDto;
import com.jio.digigov.notification.dto.request.SMSTemplateRequestDto;
import com.jio.digigov.notification.dto.response.TemplateCreationResponseDto;

/**
 * Service for template management with complete DigiGov onboarding flow
 * Flow: Get Config → Generate Token → Onboard → Save DB → Approve → Update Status
 */
public interface LegacyTemplateService {

    /**
     * Create SMS template with complete onboarding flow
     * 1. Get configuration for tenant/business
     * 2. Call DigiGov onboard API
     * 3. Save template with PENDING status
     * 4. Call DigiGov approve API
     * 5. Update template status
     */
    TemplateCreationResponseDto createSMSTemplate(
            SMSTemplateRequestDto request,
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel,
            NotificationType notificationType,
            String transactionId);

    /**
     * Create Email template with complete onboarding flow
     */
    TemplateCreationResponseDto createEmailTemplate(
            EmailTemplateRequestDto request,
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel,
            NotificationType notificationType,
            String transactionId);
}