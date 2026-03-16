package com.jio.digigov.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.entity.EmailTemplate;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.SMSTemplate;
import com.jio.digigov.notification.dto.request.SendEmailRequestDto;
import com.jio.digigov.notification.dto.request.SendSMSRequestDto;
import com.jio.digigov.notification.dto.response.SendSmsNotificationResponseDto;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.enums.TemplateStatus;
import com.jio.digigov.notification.exception.ConfigurationNotFoundException;
import com.jio.digigov.notification.exception.ValidationException;
import com.jio.digigov.notification.repository.NotificationConfigRepository;
import com.jio.digigov.notification.service.AuditService;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.SendNotificationService;
import com.jio.digigov.notification.service.LegacyTemplateService;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendNotificationServiceImpl implements SendNotificationService {
    
    private final LegacyTemplateService templateService;
    private final DigiGovClientService digiGovClientService;
    private final TokenService tokenService;
    private final NotificationConfigRepository notificationConfigRepository;
    private final AuditService auditService;

    @Override
    public SendSmsNotificationResponseDto sendSMS(SendSMSRequestDto request, Map<String, String> headers) {
        log.info("Sending SMS notification for template: {}, recipient: {}", request.getTemplateId(), request.getMobileNumber());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate the incoming request
            validateSendRequest(request, "SMS");
            
            // Retrieve SMS template
            SMSTemplate template = getSMSTemplate(request.getTemplateId());

            // Obtain configuration and authentication token
            NotificationConfig config = getConfiguration(headers);
            String token = tokenService.generateTokenWithConfig(config, CredentialType.CLIENT).getAccessToken();

            // Prepare request for DigiGov service
            MultiValueMap<String, Object> digiGovRequest = buildSendSMSRequest(request, template);
            
            // Send notification through DigiGov
            Map<String, Object> response = digiGovClientService.sendNotification(digiGovRequest, config, token, headers);
            
            // Handle the response
            SendSmsNotificationResponseDto smsResponse = processSendSMSResponse(response, request);
            
            // Record audit trail
            long responseTime = System.currentTimeMillis() - startTime;
            auditService.auditNotificationSending(
                request.getTemplateId(),
                maskSensitiveData(request.getMobileNumber()),
                "SMS",
                headers,
                auditService.maskSensitiveData(request),
                auditService.maskSensitiveData(response),
                smsResponse.getSmsStatus(),
                responseTime
            );
            
            return smsResponse;
            
        } catch (Exception e) {
            log.error("SMS send failed for template: {}, recipient: {}", request.getTemplateId(), request.getMobileNumber());
            
            // Audit the failed operation
            long responseTime = System.currentTimeMillis() - startTime;
            auditService.auditNotificationSending(
                request.getTemplateId(),
                maskSensitiveData(request.getMobileNumber()),
                "SMS",
                headers,
                auditService.maskSensitiveData(request),
                null,
                NotificationStatus.FAILED.name(),
                responseTime
            );
            
            return SendSmsNotificationResponseDto.builder()
                    .smsStatus(NotificationStatus.FAILED.name())
                    .templateId(request.getTemplateId())
                    .recipient(request.getMobileNumber())
                    .notificationType("SMS")
                    .timestamp(LocalDateTime.now())
                    .message("SMS send failed")
                    .errorDetails(e.getMessage())
                    .build();
        }
    }
    
    @Override
    public SendSmsNotificationResponseDto sendEmail(SendEmailRequestDto request, Map<String, String> headers) {
        log.info("Sending Email notification for template: {}, recipient: {}", request.getTemplateId(), request.getTo());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate the incoming request
            validateSendRequest(request, "EMAIL");
            
            // Retrieve Email template
            EmailTemplate template = getEmailTemplate(request.getTemplateId());

            // Obtain configuration and authentication token
            NotificationConfig config = getConfiguration(headers);
            String token = tokenService.generateTokenWithConfig(config, CredentialType.CLIENT).getAccessToken();

            // Prepare request for DigiGov service
            MultiValueMap<String, Object> digiGovRequest = buildSendEmailRequest(request, template);

            log.info("Email request payload: {}", digiGovRequest);
            // Send notification through DigiGov
            Map<String, Object> response = digiGovClientService.sendNotification(digiGovRequest, config, token, headers);

            // Handle the response
            SendSmsNotificationResponseDto emailResponse = processSendEmailResponse(response, request);
            
            // Record audit trail
            long responseTime = System.currentTimeMillis() - startTime;
            auditService.auditNotificationSending(
                request.getTemplateId(),
                maskSensitiveData(request.getTo()),
                "EMAIL",
                headers,
                auditService.maskSensitiveData(request),
                auditService.maskSensitiveData(response),
                emailResponse.getEmailStatus(),
                responseTime
            );
            
            return emailResponse;
            
        } catch (Exception e) {
            log.error("Email send failed for template: {}, recipient: {}", request.getTemplateId(), request.getTo());
            
            // Audit the failed operation
            long responseTime = System.currentTimeMillis() - startTime;
            auditService.auditNotificationSending(
                request.getTemplateId(),
                maskSensitiveData(request.getTo()),
                "EMAIL",
                headers,
                auditService.maskSensitiveData(request),
                null,
                NotificationStatus.FAILED.name(),
                responseTime
            );
            
            return SendSmsNotificationResponseDto.builder()
                    .emailStatus(NotificationStatus.FAILED.name())
                    .templateId(request.getTemplateId())
                    .recipient(request.getTo())
                    .notificationType("EMAIL")
                    .timestamp(LocalDateTime.now())
                    .message("Email send failed")
                    .errorDetails(e.getMessage())
                    .build();
        }
    }

    @Override
    public void validateSendRequest(Object request, String templateType) {
        switch (templateType) {
            case "SMS" -> validateSmsRequest(request);
            case "EMAIL" -> validateEmailRequest(request);
            default -> throw new ValidationException(
                    "Invalid request type for template type: " + templateType
            );
        }
    }

    private void validateSmsRequest(Object request) {
        if (!(request instanceof SendSMSRequestDto smsRequest)) {
            throw new ValidationException("Invalid request object for SMS");
        }

        requireText(smsRequest.getTemplateId(), "Template ID is required for SMS");
        requireText(smsRequest.getMobileNumber(), "Mobile number is required for SMS");

        if (smsRequest.getArgs() == null || smsRequest.getArgs().isEmpty()) {
            throw new ValidationException("Template arguments are required for SMS");
        }

        if (!smsRequest.getMobileNumber().matches("\\d{10}")) {
            throw new ValidationException("Invalid mobile number format. Must be 10 digits");
        }
    }

    private void validateEmailRequest(Object request) {
        if (!(request instanceof SendEmailRequestDto emailRequest)) {
            throw new ValidationException("Invalid request object for Email");
        }

        requireText(emailRequest.getTemplateId(), "Template ID is required for Email");
        requireText(emailRequest.getTo(), "Recipient email is required for Email");

        if (emailRequest.getArgsBody() == null || emailRequest.getArgsBody().isEmpty()) {
            throw new ValidationException("Template arguments are required for Email");
        }
        // Email format is already validated by @Email annotation
    }

    private void requireText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationException(errorMessage);
        }
    }

    // Private helper methods

    private NotificationConfig getConfiguration(Map<String, String> headers) {
        String tenantId = TenantContextHolder.getTenantId();
        String businessId = TenantContextHolder.getBusinessId();

        if (!StringUtils.hasText(businessId)) {
            throw new ConfigurationNotFoundException("Business ID not found in context");
        }

        // Get configuration with 3-level fallback
        NotificationConfig config = notificationConfigRepository.findWithFallback(businessId, tenantId)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration not found for businessId: " + businessId));

        // Validate DigiGov configuration (this service only supports DigiGov)
        if (config.getProviderType() != ProviderType.DIGIGOV) {
            throw new IllegalArgumentException("Send notification service only supports DigiGov provider, found: " + config.getProviderType());
        }

        if (config.getConfigurationJson() == null) {
            throw new IllegalArgumentException("DigiGov configuration must have configurationJson set");
        }

        return config;
    }

    private SMSTemplate getSMSTemplate(String templateId) {
        // For now, we'll assume the template exists
        // In a full implementation, this would query the database
        SMSTemplate template = new SMSTemplate();
        template.setTemplateId(templateId);
        template.setStatus(TemplateStatus.ACTIVE);
        return template;
    }

    private EmailTemplate getEmailTemplate(String templateId) {
        // For now, we'll assume the template exists
        // In a full implementation, this would query the database
        EmailTemplate template = new EmailTemplate();
        template.setTemplateId(templateId);
        template.setStatus(TemplateStatus.ACTIVE);
        return template;
    }

    private MultiValueMap<String, Object> buildSendSMSRequest(SendSMSRequestDto request, SMSTemplate template) {
        Map<String, Object> smsJson = new HashMap<>();

        // Build inner JSON object
        smsJson.put("templateId", request.getTemplateId());
        smsJson.put("mobileNumber", request.getMobileNumber());
        smsJson.put("messagedetails",
                request.getMessageDetails() != null ? request.getMessageDetails() : "SMS notification");

        if (request.getArgs() != null && !request.getArgs().isEmpty()) {
            smsJson.put("args", request.getArgs());
        }

        // Convert inner JSON to string
        String smsJsonString;
        try {
            smsJsonString = new ObjectMapper().writeValueAsString(smsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build SMS request JSON", e);
        }

        // Prepare form fields
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("smsReq", smsJsonString);

        // Wrap into form-data request map
//        Map<String, Object> digiGovRequest = new HashMap<>();
//        digiGovRequest.put("smsReq", smsJsonString);

        return formData;
    }

    private MultiValueMap<String, Object> buildSendEmailRequest(SendEmailRequestDto request, EmailTemplate template) {
        Map<String, Object> emailJson = new HashMap<>();

        // Build inner JSON object
        emailJson.put("templateId", request.getTemplateId());
        emailJson.put("to", request.getTo());

        if (request.getCc() != null) {
            emailJson.put("cc", request.getCc());
        }

        if (request.getArgsBody() != null && !request.getArgsBody().isEmpty()) {
            emailJson.put("argsBody", request.getArgsBody());
        }

        if (request.getArgsSubject() != null && !request.getArgsSubject().isEmpty()) {
            emailJson.put("argsSubject", request.getArgsSubject());
        }

        emailJson.put("messagedetails",
                request.getMessageDetails() != null ? request.getMessageDetails() : "Email notification");

        // Convert inner JSON to string
        String emailJsonString;
        try {
            emailJsonString = new ObjectMapper().writeValueAsString(emailJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build email request JSON", e);
        }

        // Prepare form fields
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("emailReq", emailJsonString);

//        //  Wrap as string, not object
//        Map<String, Object> digiGovRequest = new HashMap<>();
//        digiGovRequest.put("emailReq", emailJsonString); // value is String

        return formData;
    }

    private SendSmsNotificationResponseDto processSendSMSResponse(Map<String, Object> response, SendSMSRequestDto request) {
        String responseStatus = (String) response.getOrDefault("smsStatus", "Failed");
        String txnId = (String) response.get("txn");
        
        boolean isSuccess = "Request Accepted".equals(responseStatus);
        String status = isSuccess ? "Request Accepted" : "Not Provided";
        String message = isSuccess ? "SMS sent successfully" : "SMS send failed";
        
        return SendSmsNotificationResponseDto.builder()
                .smsStatus(status)
                .txn(txnId)
                .templateId(request.getTemplateId())
                .recipient(request.getMobileNumber())
                .notificationType("SMS")
                .timestamp(LocalDateTime.now())
                .message(message)
                .errorDetails(isSuccess ? null : responseStatus)
                .build();
    }
    
    private SendSmsNotificationResponseDto processSendEmailResponse(Map<String, Object> response, SendEmailRequestDto request) {
        String responseStatus = (String) response.getOrDefault("emailStatus", "Failed");
        String txnId = (String) response.get("txn");
        
        boolean isSuccess = "Request Accepted".equals(responseStatus);
        String status = isSuccess ? "Request Accepted" : "Request Rejected";
        String message = isSuccess ? "Email sent successfully" : "Email send failed";
        
        return SendSmsNotificationResponseDto.builder()
                .emailStatus(status)
                .txn(txnId)
                .templateId(request.getTemplateId())
                .recipient(request.getTo())
                .notificationType("EMAIL")
                .timestamp(LocalDateTime.now())
                .message(message)
                .errorDetails(isSuccess ? null : responseStatus)
                .build();
    }
    
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + 
               String.valueOf(System.nanoTime()).substring(10);
    }
    
    /**
     * Masks sensitive data for audit purposes
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        if (data.contains("@")) {
            // Email masking
            String[] parts = data.split("@");
            if (parts.length == 2) {
                String username = parts[0];
                String domain = parts[1];
                String maskedUsername = username.length() <= 2 ? "**" : 
                    username.substring(0, 1) + "****" + username.substring(username.length() - 1);
                return maskedUsername + "@" + domain;
            }
        }
        // Phone number masking
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
}