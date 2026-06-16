package com.jio.digigov.notification.mapper;

import com.jio.digigov.notification.dto.response.template.TemplateResponseDto;
import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.util.TenantContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {
    
    public TemplateResponseDto toTemplateResponse(NotificationTemplate template) {
        if (template == null) {
            return null;
        }
        
        // Determine channel type
        String channel = determineChannel(template);
        
        // Build SMS config response if present
        TemplateResponseDto.SmsConfigResponse smsConfig = null;
        if (template.getSmsConfig() != null) {
            smsConfig = TemplateResponseDto.SmsConfigResponse.builder()
                .whiteListedNumber(template.getSmsConfig().getWhiteListedNumber())
                .oprCountries(template.getSmsConfig().getOprCountries())
                .dltEntityId(template.getSmsConfig().getDltEntityId())
                .dltTemplateId(template.getSmsConfig().getDltTemplateId())
                .from(template.getSmsConfig().getFrom())
                .argumentsMap(template.getSmsConfig().getArgumentsMap())
                .build();
        }
        
        // Build Email config response if present
        TemplateResponseDto.EmailConfigResponse emailConfig = null;
        if (template.getEmailConfig() != null) {
            emailConfig = TemplateResponseDto.EmailConfigResponse.builder()
                .to(template.getEmailConfig().getTo())
                .cc(template.getEmailConfig().getCc())
                .subject(template.getEmailConfig().getTemplateSubject())
                .body(template.getEmailConfig().getTemplateBody())
                .fromName(template.getEmailConfig().getTemplateFromName())
                .from(template.getEmailConfig().getFrom())
                .replyTo(template.getEmailConfig().getReplyTo())
                .emailType(template.getEmailConfig().getEmailType())
                .argumentsBodyMap(template.getEmailConfig().getArgumentsBodyMap())
                .argumentsSubjectMap(template.getEmailConfig().getArgumentsSubjectMap())
                .build();
        }
        
        // Get template content and details
        String templateContent = getTemplateContent(template);
        String templateDetails = getTemplateDetails(template);
        
        return TemplateResponseDto.builder()
            .id(template.getId())
            .templateId(template.getTemplateId())
            .tenantId(TenantContextHolder.getTenantId())
            .businessId(template.getBusinessId())
            .scopeLevel(template.getScopeLevel().name())
            .eventType(template.getEventType())
            .language(template.getLanguage())
            .type(template.getType().name())
            .channel(channel)
            .status(template.getStatus().name())
            .version(template.getVersion())
            .template(templateContent)
            .templateDetails(templateDetails)
            .smsConfig(smsConfig)
            .emailConfig(emailConfig)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
    
    private String determineChannel(NotificationTemplate template) {
        if (template.getSmsConfig() != null && template.getEmailConfig() != null) {
            return NotificationChannel.SMS.name() + "," + NotificationChannel.EMAIL.name();
        } else if (template.getSmsConfig() != null) {
            return NotificationChannel.SMS.name();
        } else if (template.getEmailConfig() != null) {
            return NotificationChannel.EMAIL.name();
        }
        return "UNKNOWN";
    }
    
    private String getTemplateContent(NotificationTemplate template) {
        if (template.getSmsConfig() != null && template.getSmsConfig().getTemplate() != null) {
            return template.getSmsConfig().getTemplate();
        }
        if (template.getEmailConfig() != null && template.getEmailConfig().getTemplateBody() != null) {
            return template.getEmailConfig().getTemplateBody();
        }
        return null;
    }
    
    private String getTemplateDetails(NotificationTemplate template) {
        if (template.getSmsConfig() != null && template.getSmsConfig().getTemplateDetails() != null) {
            return template.getSmsConfig().getTemplateDetails();
        }
        if (template.getEmailConfig() != null && template.getEmailConfig().getTemplateDetails() != null) {
            return template.getEmailConfig().getTemplateDetails();
        }
        return null;
    }
}