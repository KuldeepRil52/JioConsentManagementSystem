package com.jio.digigov.notification.service;

import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateResponseDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateResponseDto;
import com.jio.digigov.notification.enums.NotificationType;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * Client service for DigiGov API integration
 * Handles template onboard and approve operations
 */
public interface DigiGovClientService {

    /**
     * Onboard template to DigiGov
     */
    OnboardTemplateResponseDto onboardTemplate(OnboardTemplateRequestDto request, NotificationConfig config, NotificationType notificationType, String transactionId);

    /**
     * Approve template in DigiGov
     */
    ApproveTemplateResponseDto approveTemplate(ApproveTemplateRequestDto request, NotificationConfig config, Map<String, String> headers, NotificationType notificationType, String transactionId);

    /**
     * Initialize OTP via DigiGov
     */
    Map<String, Object> initOTP(Map<String, Object> request, NotificationConfig config, String token, Map<String, String> headers);

    /**
     * Verify OTP via DigiGov
     */
    Map<String, Object> verifyOTP(Map<String, Object> request, NotificationConfig config, String token, Map<String, String> headers);

    /**
     * Send notification via DigiGov
     */
    Map<String, Object> sendNotification(MultiValueMap<String, Object> request, NotificationConfig config, String token, Map<String, String> headers);
}