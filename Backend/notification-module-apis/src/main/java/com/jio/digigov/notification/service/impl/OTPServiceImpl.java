package com.jio.digigov.notification.service.impl;

import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.dto.request.InitOTPRequestDto;
import com.jio.digigov.notification.dto.request.VerifyOTPRequestDto;
import com.jio.digigov.notification.dto.response.OTPInitResponseDto;
import com.jio.digigov.notification.dto.response.OTPVerifyResponseDto;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.IdType;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.exception.ConfigurationNotFoundException;
import com.jio.digigov.notification.exception.ValidationException;
import com.jio.digigov.notification.repository.NotificationConfigRepository;
import com.jio.digigov.notification.service.AuditService;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.OTPService;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.service.audit.AuditEventService;
import com.jio.digigov.notification.util.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPServiceImpl implements OTPService {

    private final DigiGovClientService digiGovClientService;
    private final TokenService tokenService;
    private final NotificationConfigRepository notificationConfigRepository;
    private final AuditService auditService;
    private final AuditEventService auditEventService;

    @Override
    @Transactional
    public OTPInitResponseDto initOTP(InitOTPRequestDto request, Map<String, String> headers, HttpServletRequest httpRequest) {
        log.info("Initializing OTP for templateId: {}, idValue: {}, idType: {}", 
                 request.getTemplateId(), request.getIdValue(), request.getIdType());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate the incoming request
            validateOTPRequest(request, "INIT");
            
            // Determine the notification channel
            String notificationChannel = determineNotificationChannel(request.getIdType());
            log.debug("Determined notification channel: {}", notificationChannel);
            
            // Obtain configuration and authentication token
            NotificationConfig config = getConfiguration(headers.get(HeaderConstants.X_TENANT_ID), headers.get(HeaderConstants.X_BUSINESS_ID));
            String token = tokenService.generateTokenWithConfig(config, CredentialType.CLIENT).getAccessToken();

            // Prepare request for DigiGov service
            Map<String, Object> digiGovRequest = buildOTPInitRequest(request);
            
            // Send request to DigiGov
            Map<String, Object> response = digiGovClientService.initOTP(digiGovRequest, config, token, headers);
            
            // Handle the response
            OTPInitResponseDto otpResponse = processOTPInitResponse(response, request.getTxnId());

            // Audit successful OTP initialization
            String tenantId = headers.get(HeaderConstants.X_TENANT_ID);
            String businessId = headers.get(HeaderConstants.X_BUSINESS_ID);
            auditEventService.auditOtpOperation(
                    request.getTxnId(),
                    "INIT",
                    "SUCCESS",
                    tenantId,
                    businessId,
                    request.getTxnId(),
                    httpRequest
            );

            return otpResponse;

        } catch (Exception e) {
            log.error("OTP initialization failed for templateId: {}, idValue: {}",
                     request.getTemplateId(), request.getIdValue(), e);

            // Audit failed OTP initialization
            String tenantId = headers.get(HeaderConstants.X_TENANT_ID);
            String businessId = headers.get(HeaderConstants.X_BUSINESS_ID);
            auditEventService.auditOtpOperation(
                    request.getTxnId(),
                    "INIT",
                    "FAILED",
                    tenantId,
                    businessId,
                    request.getTxnId(),
                    httpRequest
            );

            // Return error response instead of throwing exception
            return OTPInitResponseDto.builder()
                    .status("Failed")
                    .txnId(request.getTxnId())
                    .expiry(null)
                    .build();
        }
    }

    @Override
    @Transactional
    public OTPVerifyResponseDto verifyOTP(VerifyOTPRequestDto request, Map<String, String> headers, HttpServletRequest httpRequest) {
        log.info("Verifying OTP for txnId: {}", request.getTxnId());
        
        try {
            // Validate the incoming request
            validateOTPRequest(request, "VERIFY");

            // Step 2: Get configuration and token
            NotificationConfig config = getConfiguration(headers.get(HeaderConstants.X_TENANT_ID), headers.get(HeaderConstants.X_BUSINESS_ID));
            String token = tokenService.generateTokenWithConfig(config, CredentialType.CLIENT).getAccessToken();

            // Step 3: Build DigiGov request
            Map<String, Object> digiGovRequest = buildOTPVerifyRequest(request);
            
            // Step 4: Call DigiGov
            Map<String, Object> response = digiGovClientService.verifyOTP(digiGovRequest, config, token, headers);
            
            // Step 5: Process response
            OTPVerifyResponseDto otpResponse = processOTPVerifyResponse(response);

            // Audit successful OTP verification
            String tenantId = headers.get(HeaderConstants.X_TENANT_ID);
            String businessId = headers.get(HeaderConstants.X_BUSINESS_ID);
            auditEventService.auditOtpOperation(
                    request.getTxnId(),
                    "VERIFY",
                    "SUCCESS",
                    tenantId,
                    businessId,
                    request.getTxnId(),
                    httpRequest
            );

            return otpResponse;

        } catch (Exception e) {
            log.error("OTP verification failed for txnId: {}", request.getTxnId());

            // Audit failed OTP verification
            String tenantId = headers.get(HeaderConstants.X_TENANT_ID);
            String businessId = headers.get(HeaderConstants.X_BUSINESS_ID);
            auditEventService.auditOtpOperation(
                    request.getTxnId(),
                    "VERIFY",
                    "FAILED",
                    tenantId,
                    businessId,
                    request.getTxnId(),
                    httpRequest
            );

            // Return failed verification instead of throwing exception
            return OTPVerifyResponseDto.builder()
                    .STATUS("INVALID")
                    .build();
        }
    }
    
    @Override
    public void validateOTPRequest(Object request, String operation) {
        if ("INIT".equals(operation) && request instanceof InitOTPRequestDto initRequest) {
            if (!StringUtils.hasText(initRequest.getTemplateId())) {
                throw new ValidationException("Template ID is required for OTP initialization");
            }
            if (initRequest.getIdType() == null) {
                throw new ValidationException("ID type is required for OTP initialization");
            }
            if (!StringUtils.hasText(initRequest.getIdValue())) {
                throw new ValidationException("ID value is required for OTP initialization");
            }
            if (!StringUtils.hasText(initRequest.getTxnId())) {
                throw new ValidationException("Transaction ID is required for OTP initialization");
            }
//            if (initRequest.getArgsBody() == null || initRequest.getArgsBody().isEmpty()) {
//                throw new ValidationException("Template arguments are required for OTP initialization");
//            }
            
            // Validate ID type values
            if (initRequest.getIdType() != 3 && initRequest.getIdType() != 4) {
                throw new ValidationException("ID type must be 3 (SMS) or 4 (Email)");
            }

            // Basic format validation
            if (initRequest.getIdType() == IdType.PHONE.getCode()) {
                // Validate mobile number format (basic)
                if (!initRequest.getIdValue().matches("\\d{10}")) {
                    throw new ValidationException("Invalid mobile number format. Must be 10 digits");
                }
            } else if (initRequest.getIdType() == IdType.EMAIL.getCode()) {
                // Validate email format (basic)
                if (!initRequest.getIdValue().contains("@")) {
                    throw new ValidationException("Invalid email format");
                }
            } else {
                throw new ValidationException("Unsupported ID type: " + initRequest.getIdType());
            }
            
        } else if ("VERIFY".equals(operation) && request instanceof VerifyOTPRequestDto verifyRequest) {
            if (!StringUtils.hasText(verifyRequest.getTxnId())) {
                throw new ValidationException("Transaction ID is required for OTP verification");
            }
            if (!StringUtils.hasText(verifyRequest.getUserOTP())) {
                throw new ValidationException("OTP is required for verification");
            }
        } else {
            throw new ValidationException("Invalid request type for OTP operation: " + operation);
        }
    }
    
    @Override
    public String determineNotificationChannel(Integer idType) {
        if (idType == null) {
            return "UNKNOWN";
        }
        
        switch (idType) {
            case 3:
                return "SMS";
            case 4:
                return "EMAIL";
            default:
                return "UNKNOWN";
        }
    }
    
    // Private helper methods

    /**
     * Get configuration for the tenant and business with 3-level fallback.
     */
    private NotificationConfig getConfiguration(String tenantId, String businessId) {
        // Set tenant context for configuration lookup
        TenantContextHolder.setTenantId(tenantId);

        // Use businessId from request
        if (!StringUtils.hasText(businessId)) {
            throw new RuntimeException("BusinessId not found in request for tenantId: " + tenantId);
        }

        // Get configuration with 3-level fallback
        NotificationConfig config = notificationConfigRepository.findWithFallback(businessId, tenantId)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration not found for businessId: " + businessId));

        // Validate DigiGov configuration (OTP is DigiGov-only)
        if (config.getProviderType() != ProviderType.DIGIGOV) {
            throw new IllegalArgumentException("OTP service only supports DigiGov provider, found: " + config.getProviderType());
        }

        if (config.getConfigurationJson() == null) {
            throw new IllegalArgumentException("DigiGov configuration must have configurationJson set");
        }

        return config;
    }
    
    private Map<String, Object> buildOTPInitRequest(InitOTPRequestDto request) {
        Map<String, Object> digiGovRequest = new HashMap<>();
        digiGovRequest.put("idType", request.getIdType());
        digiGovRequest.put("idValue", request.getIdValue());
        digiGovRequest.put("txnId", request.getTxnId());
        digiGovRequest.put("systemName", request.getSystemName());
        digiGovRequest.put("templateId", request.getTemplateId());
        // digiGovRequest.put("argsBody", request.getArgsBody());
        
        if (request.getArgsFromName() != null && !request.getArgsFromName().isEmpty()) {
            digiGovRequest.put("argsFromName", request.getArgsFromName());
        }
        
        return digiGovRequest;
    }
    
    private Map<String, Object> buildOTPVerifyRequest(VerifyOTPRequestDto request) {
        Map<String, Object> digiGovRequest = new HashMap<>();
        digiGovRequest.put("userOTP", request.getUserOTP());
        digiGovRequest.put("txnId", request.getTxnId());
        
        return digiGovRequest;
    }
    
    private OTPInitResponseDto processOTPInitResponse(Map<String, Object> response, String txnId) {
        String status = (String) response.getOrDefault("status", "Failed");
        String expiry = (String) response.get("expiry");
        String responseTxnId = (String) response.getOrDefault("txnId", txnId);
        
        return OTPInitResponseDto.builder()
                .status(status)
                .expiry(expiry)
                .txnId(responseTxnId)
                .build();
    }
    
    private OTPVerifyResponseDto processOTPVerifyResponse(Map<String, Object> response) {
        // DigiGov returns "STATUS" field with "VALID" or "INVALID"
        String status = (String) response.getOrDefault("status", "INVALID");
        
        return OTPVerifyResponseDto.builder()
                .STATUS(status)
                .build();
    }
    
    /**
     * Masks sensitive data for audit purposes
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
}