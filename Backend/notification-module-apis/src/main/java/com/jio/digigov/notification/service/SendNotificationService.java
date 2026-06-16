package com.jio.digigov.notification.service;

import com.jio.digigov.notification.dto.request.SendEmailRequestDto;
import com.jio.digigov.notification.dto.request.SendSMSRequestDto;
import com.jio.digigov.notification.dto.response.SendSmsNotificationResponseDto;

import java.util.Map;

/**
 * Service for sending SMS and Email notifications
 */
public interface SendNotificationService {
    
    /**
     * Sends SMS notification using existing template
     * 
     * @param request SMS send request with template ID and recipient details
     * @param headers Request headers with tenant context
     * @return Send notification response with status and tracking details
     */
    SendSmsNotificationResponseDto sendSMS(SendSMSRequestDto request, Map<String, String> headers);
    
    /**
     * Sends Email notification using existing template
     * 
     * @param request Email send request with template ID and recipient details
     * @param headers Request headers with tenant context
     * @return Send notification response with status and tracking details
     */
    SendSmsNotificationResponseDto sendEmail(SendEmailRequestDto request, Map<String, String> headers);
    
    /**
     * Validates send request before processing
     * 
     * @param request Send request (SMS or Email)
     * @param templateType Expected template type (SMS or EMAIL)
     */
    void validateSendRequest(Object request, String templateType);
}