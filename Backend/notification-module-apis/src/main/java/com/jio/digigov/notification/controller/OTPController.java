package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.dto.request.InitOTPRequestDto;
import com.jio.digigov.notification.dto.request.VerifyOTPRequestDto;
import com.jio.digigov.notification.dto.response.NotificationResponseDto;
import com.jio.digigov.notification.dto.response.OTPInitResponseDto;
import com.jio.digigov.notification.dto.response.OTPVerifyResponseDto;
import com.jio.digigov.notification.service.OTPService;
import com.jio.digigov.notification.enums.NotificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/otp", "/g2cOTPAuth/v1.7"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OTP Management", description = "APIs for OTP initialization and verification")
public class OTPController extends BaseController {

    private final OTPService otpService;

    @Operation(
        summary = "Initialize OTP", 
        description = "Initializes OTP delivery via SMS or Email"
    )
    @ApiResponse(responseCode = "200", description = "OTP initialized successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PostMapping("/init")
    public ResponseEntity<NotificationResponseDto<OTPInitResponseDto>> initOTP(
            HttpServletRequest httpRequest,
            @Valid @RequestBody InitOTPRequestDto request) {
        
        log.info("Initializing OTP for idType: {}, templateId: {}, txnId: {}", 
                 request.getIdType(), request.getTemplateId(), request.getTxnId());
        log.info("Init OTP Request payload: {}", request);
        // Extract headers
        Map<String, String> headers = extractHeaders(httpRequest);

        // Initialize OTP
        OTPInitResponseDto response = otpService.initOTP(request, headers, httpRequest);
        
        // Build API response based on status
        NotificationResponseDto<OTPInitResponseDto> apiResponse;
        if ("Success".equals(response.getStatus()) || NotificationStatus.SUCCESS.name().equals(response.getStatus())) {
            apiResponse = NotificationResponseDto.success(response, "OTP initialized successfully");
        } else {
            apiResponse = NotificationResponseDto.error("OTP initialization failed", "OTP_INIT_FAILED",
                    "Failed to initialize OTP");
        }
        
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
        summary = "Verify OTP", 
        description = "Verifies OTP using DigiGov Gateway integration"
    )
    @ApiResponse(responseCode = "200", description = "OTP verification result")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PostMapping("/verify")
    public ResponseEntity<NotificationResponseDto<OTPVerifyResponseDto>> verifyOTP(
            HttpServletRequest httpRequest,
            @Valid @RequestBody VerifyOTPRequestDto request) {
        
        log.info("Verifying OTP for txnId: {}", request.getTxnId());
        log.info("Verify OTP Request payload: {}", request);
        // Extract headers
        Map<String, String> headers = extractHeaders(httpRequest);

        // Verify OTP
        OTPVerifyResponseDto response = otpService.verifyOTP(request, headers, httpRequest);
        
        // Build API response based on verification result
        String message = response.isValid() ? 
            "OTP verified successfully" : 
            "OTP verification failed";
        
        NotificationResponseDto<OTPVerifyResponseDto> apiResponse = NotificationResponseDto.success(response, message);
        
        return ResponseEntity.ok(apiResponse);
    }
}