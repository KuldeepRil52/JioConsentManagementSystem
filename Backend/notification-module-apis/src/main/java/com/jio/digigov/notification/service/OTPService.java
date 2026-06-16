package com.jio.digigov.notification.service;

import com.jio.digigov.notification.dto.request.InitOTPRequestDto;
import com.jio.digigov.notification.dto.request.VerifyOTPRequestDto;
import com.jio.digigov.notification.dto.response.OTPInitResponseDto;
import com.jio.digigov.notification.dto.response.OTPVerifyResponseDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Service for OTP operations
 */
public interface OTPService {
    
    /**
     * Initiates OTP delivery via SMS or Email
     *
     * @param request OTP initialization request
     * @param headers Request headers with tenant context
     * @param httpRequest HTTP servlet request for IP extraction and audit logging
     * @return OTP initialization response with expiry and transaction ID
     */
    OTPInitResponseDto initOTP(InitOTPRequestDto request, Map<String, String> headers, HttpServletRequest httpRequest);

    /**
     * Verifies OTP
     *
     * @param request OTP verification request
     * @param headers Request headers with tenant context
     * @param httpRequest HTTP servlet request for IP extraction and audit logging
     * @return OTP verification response with validation result
     */
    OTPVerifyResponseDto verifyOTP(VerifyOTPRequestDto request, Map<String, String> headers, HttpServletRequest httpRequest);
    
    /**
     * Validates OTP request before processing
     * 
     * @param request OTP request (init or verify)
     * @param operation Operation type (INIT or VERIFY)
     */
    void validateOTPRequest(Object request, String operation);
    
    /**
     * Determines notification channel based on ID type
     * 
     * @param idType ID type from request
     * @return Notification channel (SMS or EMAIL)
     */
    String determineNotificationChannel(Integer idType);
}