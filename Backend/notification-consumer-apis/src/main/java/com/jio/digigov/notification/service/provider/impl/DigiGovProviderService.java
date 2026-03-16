package com.jio.digigov.notification.service.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateResponseDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateResponseDto;
import com.jio.digigov.notification.dto.provider.*;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.NetworkType;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.service.provider.NotificationProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DigiGov provider implementation.
 * Wraps the existing DigiGovClientService to implement the NotificationProviderService interface.
 * This adapter allows DigiGov to work with the new provider abstraction layer.
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DigiGovProviderService implements NotificationProviderService {

    private final DigiGovClientService digiGovClientService;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DIGIGOV;
    }

    @Override
    public List<NotificationChannel> getSupportedChannels() {
        return List.of(NotificationChannel.SMS, NotificationChannel.EMAIL);
    }

    @Override
    public boolean supportsChannel(NotificationChannel channel) {
        return channel == NotificationChannel.SMS || channel == NotificationChannel.EMAIL;
    }

    @Override
    public boolean requiresApproval() {
        return true; // DigiGov requires template approval via Admin API
    }

    @Override
    public ProviderTemplateResponse onboardTemplate(ProviderTemplateRequest request, NotificationConfig config) {
        try {
            log.info("Onboarding template to DigiGov for eventType: {}, channel: {}",
                    request.getEventType(), request.getChannelType());

            NotificationConfig ngConfig = config;

            // Build DigiGov onboard request
            OnboardTemplateRequestDto onboardRequest = buildDigiGovOnboardRequest(request);

            // Call DigiGov onboard API
            OnboardTemplateResponseDto response = digiGovClientService.onboardTemplate(
                    onboardRequest,
                    ngConfig,
                    request.getNotificationType(),
                    request.getTransactionId()
            );

            if (response != null && StringUtils.hasText(response.getTemplateId())) {
                log.info("Successfully onboarded template to DigiGov: {}", response.getTemplateId());
                return ProviderTemplateResponse.success(
                        response.getTemplateId(),
                        "Template onboarded successfully to DigiGov",
                        true, // DigiGov requires approval
                        request.getTransactionId()
                );
            } else {
                log.error("Failed to onboard template to DigiGov: No templateId returned");
                return ProviderTemplateResponse.failure(
                        "ONBOARD_FAILED",
                        "Failed to onboard template: No templateId returned",
                        request.getTransactionId()
                );
            }

        } catch (Exception e) {
            log.error("Error onboarding template to DigiGov");
            return ProviderTemplateResponse.failure(
                    "ONBOARD_ERROR",
                    "Error onboarding template: " + e.getMessage(),
                    request.getTransactionId()
            );
        }
    }

    @Override
    public ProviderTemplateResponse approveTemplate(String templateId, NotificationConfig config,
                                                     String transactionId) {
        try {
            log.info("Approving template in DigiGov: {}", templateId);

            NotificationConfig ngConfig = config;

            // Build approve request
            ApproveTemplateRequestDto approveRequest = ApproveTemplateRequestDto.builder()
                    .templateId(templateId)
                    .status("A") // A = Approved
                    .type("all") // all = both SMS and EMAIL
                    .build();

            Map<String, String> headers = new HashMap<>();
            headers.put("txn", transactionId);

            // Call DigiGov approve API (uses ADMIN credentials)
            ApproveTemplateResponseDto response = digiGovClientService.approveTemplate(
                    approveRequest,
                    ngConfig,
                    headers,
                    com.jio.digigov.notification.enums.NotificationType.NOTIFICATION,
                    transactionId
            );

            if (response != null && (response.isSmsActive() || response.isEmailActive())) {
                log.info("Successfully approved template in DigiGov: {} - {}", templateId, response.getCombinedStatus());
                return ProviderTemplateResponse.success(
                        templateId,
                        "Template approved successfully in DigiGov",
                        false,
                        transactionId
                );
            } else {
                String status = response != null ? response.getCombinedStatus() : "null response";
                log.error("Failed to approve template in DigiGov: {} - {}", templateId, status);
                return ProviderTemplateResponse.failure(
                        "APPROVE_FAILED",
                        "Failed to approve template in DigiGov: " + status,
                        transactionId
                );
            }

        } catch (Exception e) {
            log.error("Error approving template in DigiGov: {}", templateId);
            return ProviderTemplateResponse.failure(
                    "APPROVE_ERROR",
                    "Error approving template: " + e.getMessage(),
                    transactionId
            );
        }
    }

    @Override
    public ProviderEmailResponse sendEmail(ProviderEmailRequest request, NotificationConfig config) {
        boolean hasRetried = false;

        while (true) {
            try {
                log.info("Sending email via DigiGov for template: {}, to: {}, retry: {}",
                        request.getTemplateId(), request.getTo(), hasRetried);

                NotificationConfig ngConfig = config;

                // Generate token and extract access_token
                String token = tokenService.generateTokenWithConfig(ngConfig, CredentialType.CLIENT).getAccessToken();

                // Build DigiGov email request
            Map<String, Object> emailJson = new HashMap<>();
            emailJson.put("templateId", request.getTemplateId());
            emailJson.put("to", String.join(",", request.getTo()));

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                emailJson.put("cc", String.join(",", request.getCc()));
            }

            if (request.getSubjectArguments() != null) {
                emailJson.put("argsSubject", request.getSubjectArguments());
            }

            if (request.getBodyArguments() != null) {
                emailJson.put("argsBody", request.getBodyArguments());
            }

            emailJson.put("messagedetails", request.getMessageDetails() != null ?
                    request.getMessageDetails() : "Email notification");

            String emailJsonString = objectMapper.writeValueAsString(emailJson);

            // Create multipart form data
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("emailReq", emailJsonString);

            // Prepare headers
            Map<String, String> headers = new HashMap<>();
            if (request.getAdditionalHeaders() != null) {
                headers.putAll(request.getAdditionalHeaders());
            }
            headers.put("txn", request.getTransactionId());

                // Call DigiGov send notification API
                Map<String, Object> response = digiGovClientService.sendNotification(
                        formData,
                        ngConfig,
                        token,
                        headers
                );

                // Parse response
                if (response != null && response.containsKey("emailStatus")) {
                    String emailStatus = String.valueOf(response.get("emailStatus"));

                    // DigiGov returns "Request Accepted" for successful email submission
                    if ("Request Accepted".equalsIgnoreCase(emailStatus)) {
                        String messageId = response.containsKey("txn") ?
                                String.valueOf(response.get("txn")) : null;

                        log.info("Successfully sent email via DigiGov: emailStatus={}, txn={}", emailStatus, messageId);
                        return ProviderEmailResponse.success(
                                messageId,
                                "Email sent successfully via DigiGov",
                                request.getTransactionId()
                        );
                    } else {
                        log.error("Failed to send email via DigiGov: emailStatus={}", emailStatus);
                        return ProviderEmailResponse.failure(
                                "SEND_FAILED",
                                "Email send failed with status: " + emailStatus,
                                request.getTransactionId(),
                                isRetryableError("SEND_FAILED")
                        );
                    }
                } else {
                    String errorMsg = response != null && response.containsKey("error") ?
                            String.valueOf(response.get("error")) : "Unknown error";

                    log.error("Failed to send email via DigiGov: {}", errorMsg);
                    return ProviderEmailResponse.failure(
                            "SEND_FAILED",
                            errorMsg,
                            request.getTransactionId(),
                            isRetryableError("SEND_FAILED")
                    );
                }

            } catch (Exception e) {
                // Check if this is an authentication error
                if (!hasRetried && isAuthenticationError(e)) {
                    log.warn("Authentication error detected: {}. Clearing token cache and retrying once.",
                            e.getMessage());

                    // Clear cached token
                    NotificationConfig ngConfig = config;
                    if (tokenService instanceof com.jio.digigov.notification.service.cache.CachedTokenService) {
                        ((com.jio.digigov.notification.service.cache.CachedTokenService) tokenService)
                                .evictToken(ngConfig.getBusinessId(), CredentialType.CLIENT);
                    }

                    hasRetried = true;
                    continue; // Retry immediately
                }

                // Not an auth error or already retried - propagate error
                log.error("Error sending email via DigiGov (retry: {})", hasRetried);
                return ProviderEmailResponse.failure(
                        "SEND_ERROR",
                        "Error sending email: " + e.getMessage(),
                        request.getTransactionId(),
                        true // Network errors are generally retryable
                );
            }
        }
    }

    @Override
    public boolean validateConfiguration(NotificationConfig config) {
        if (config == null || config.getConfigurationJson() == null) {
            log.error("Configuration or configurationJson is null");
            return false;
        }

        var details = config.getConfigurationJson();

        boolean valid = StringUtils.hasText(details.getBaseUrl()) &&
                StringUtils.hasText(details.getClientId()) &&
                StringUtils.hasText(details.getClientSecret()) &&
                StringUtils.hasText(details.getSid());

        if (!valid) {
            log.error("DigiGov configuration validation failed: missing required fields");
        }

        return valid;
    }

    @Override
    public boolean testConnection(NotificationConfig config) {
        try {
            log.info("Testing DigiGov connection for businessId: {}", config.getBusinessId());

            NotificationConfig ngConfig = config;

            // Try to generate a token - if successful, connection is working
            String token = tokenService.generateTokenWithConfig(ngConfig, CredentialType.CLIENT).getAccessToken();

            boolean success = StringUtils.hasText(token);
            if (success) {
                log.info("DigiGov connection test successful for businessId: {}", config.getBusinessId());
            } else {
                log.error("DigiGov connection test failed: No token generated");
            }

            return success;

        } catch (Exception e) {
            log.error("DigiGov connection test failed");
            return false;
        }
    }

    @Override
    public String mapErrorCode(String providerErrorCode) {
        // Map DigiGov error codes to standard notification system error codes
        if (providerErrorCode == null) {
            return "UNKNOWN_ERROR";
        }

        return switch (providerErrorCode.toUpperCase()) {
            case "INVALID_TOKEN", "TOKEN_EXPIRED" -> "AUTHENTICATION_ERROR";
            case "INVALID_TEMPLATE_ID" -> "TEMPLATE_NOT_FOUND";
            case "INVALID_EMAIL", "INVALID_MOBILE" -> "INVALID_RECIPIENT";
            case "NETWORK_ERROR", "TIMEOUT" -> "NETWORK_ERROR";
            case "RATE_LIMIT_EXCEEDED" -> "RATE_LIMIT_ERROR";
            default -> providerErrorCode;
        };
    }

    @Override
    public boolean isRetryableError(String errorCode) {
        if (errorCode == null) {
            return false;
        }

        String code = errorCode.toUpperCase();
        return code.contains("NETWORK") ||
                code.contains("TIMEOUT") ||
                code.contains("RATE_LIMIT") ||
                code.contains("SERVICE_UNAVAILABLE") ||
                code.contains("INTERNAL_SERVER_ERROR");
    }

    /**
     * Check if an exception represents an authentication/token error.
     * Used to determine if we should clear token cache and retry.
     */
    private boolean isAuthenticationError(Exception e) {
        if (e == null) {
            return false;
        }

        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("invalid credentials") ||
                lowerMessage.contains("invalid_token") ||
                lowerMessage.contains("token_expired") ||
                lowerMessage.contains("token expired") ||
                lowerMessage.contains("authentication failed") ||
                lowerMessage.contains("401") ||
                lowerMessage.contains("unauthorized") ||
                lowerMessage.contains("403") ||
                lowerMessage.contains("forbidden");
    }


    /**
     * Build DigiGov onboard request from provider request.
     */
    private OnboardTemplateRequestDto buildDigiGovOnboardRequest(ProviderTemplateRequest request) {
        OnboardTemplateRequestDto.OnboardTemplateRequestDtoBuilder builder =
                OnboardTemplateRequestDto.builder();

        if (request.getChannelType() == NotificationChannel.SMS) {
            // Build SMS onboard request
            var smsTemplate = OnboardTemplateRequestDto.SmsTemplateDto.builder()
                    .whiteListedNumber(request.getWhitelistedNumbers())
                    .template(request.getSmsTemplate())
                    .templateDetails(request.getSmsTemplateDetails())
                    .oprCountries(request.getOperatorCountries())
                    .dltEntityId(request.getDltEntityId())
                    .dltTemplateId(request.getDltTemplateId())
                    .from(request.getSmsFrom())
                    .build();
            builder.smsTemplate(smsTemplate);

        } else if (request.getChannelType() == NotificationChannel.EMAIL) {
            // Build EMAIL onboard request
            var emailTemplate = OnboardTemplateRequestDto.EmailTemplateDto.builder()
                    .to(request.getEmailTo())
                    .cc(request.getEmailCc())
                    .templateDetails(request.getEmailTemplateDetails())
                    .templateBody(request.getEmailBody())
                    .templateSubject(request.getEmailSubject())
                    .templateFromName(request.getEmailFromName())
                    .emailType(request.getEmailType())
                    .build();
            builder.emailTemplate(emailTemplate);
        }

        return builder.build();
    }
}
