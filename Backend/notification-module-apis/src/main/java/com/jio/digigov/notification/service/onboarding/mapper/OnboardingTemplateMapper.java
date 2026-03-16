package com.jio.digigov.notification.service.onboarding.mapper;

import com.jio.digigov.notification.dto.onboarding.EventTemplateDefinition;
import com.jio.digigov.notification.dto.request.onboarding.EventDltConfigDto;
import com.jio.digigov.notification.dto.request.template.CreateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.EmailTemplateDto;
import com.jio.digigov.notification.dto.request.template.SmsTemplateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert EventTemplateDefinition to CreateTemplateRequestDto.
 *
 * This mapper transforms the template definitions from the onboarding provider
 * into the request DTOs required by TemplateService.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
@Slf4j
public class OnboardingTemplateMapper {

    /**
     * Converts an EventTemplateDefinition to a CreateTemplateRequestDto for SMS.
     *
     * @param definition The template definition from the provider
     * @param dltConfig DLT Entity ID and Template ID for regulatory compliance
     * @return CreateTemplateRequestDto for SMS template creation
     */
    public CreateTemplateRequestDto toSmsRequest(EventTemplateDefinition definition, EventDltConfigDto dltConfig) {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto();
        request.setEventType(definition.getEventType());
        request.setLanguage(definition.getSms().getLanguage());
        request.setRecipientType(definition.getRecipientType() != null ? definition.getRecipientType() : "DATA_PRINCIPAL");

        SmsTemplateDto smsDto = new SmsTemplateDto();
        smsDto.setWhiteListedNumber(definition.getSms().getWhiteListedNumbers());
        smsDto.setTemplate(definition.getSms().getTemplate());
        smsDto.setTemplateDetails(definition.getSms().getTemplateDetails());
        smsDto.setOprCountries(definition.getSms().getOprCountries());
        smsDto.setDltEntityId(dltConfig.getDltEntityId());
        smsDto.setDltTemplateId(dltConfig.getDltTemplateId());
        smsDto.setFrom(definition.getSms().getFrom());
        smsDto.setArgumentsMap(definition.getSms().getArgumentsMap());

        request.setSmsTemplate(smsDto);
        request.setEmailTemplate(null);

        return request;
    }

    /**
     * Converts an EventTemplateDefinition to a CreateTemplateRequestDto for Email.
     *
     * @param definition The template definition from the provider
     * @return CreateTemplateRequestDto for Email template creation
     */
    public CreateTemplateRequestDto toEmailRequest(EventTemplateDefinition definition) {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto();
        request.setEventType(definition.getEventType());
        request.setLanguage(definition.getEmail().getLanguage());
        request.setRecipientType(definition.getRecipientType() != null ? definition.getRecipientType() : "DATA_PRINCIPAL");

        // Build Email template DTO
        EmailTemplateDto emailDto = new EmailTemplateDto();
        emailDto.setTo(definition.getEmail().getToRecipients());
        emailDto.setCc(definition.getEmail().getCcRecipients());
        emailDto.setTemplateDetails(definition.getEmail().getTemplateDetails());
        emailDto.setTemplateBody(definition.getEmail().getTemplateBody());
        emailDto.setTemplateSubject(definition.getEmail().getTemplateSubject());
        emailDto.setTemplateFromName(definition.getEmail().getTemplateFromName());
        emailDto.setEmailType(definition.getEmail().getEmailType());
        emailDto.setFrom(definition.getEmail().getFrom());
        emailDto.setArgumentsSubjectMap(definition.getEmail().getArgumentsSubjectMap());
        emailDto.setArgumentsBodyMap(definition.getEmail().getArgumentsBodyMap());

        request.setSmsTemplate(null);  // Only Email
        request.setEmailTemplate(emailDto);

        return request;
    }
}
