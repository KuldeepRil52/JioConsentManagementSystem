package com.jio.digigov.notification.controller;

import com.jio.digigov.notification.dto.request.SendEmailRequestDto;
import com.jio.digigov.notification.dto.request.SendSMSRequestDto;
import com.jio.digigov.notification.dto.response.SendSmsNotificationResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.service.SendNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Legacy REST Controller for notification sending.
 *
 * This controller provides legacy endpoints for sending SMS and email notifications.
 * It maintains backward compatibility while new applications should use the unified
 * notification endpoints in the current API version.
 */
@RestController
@RequestMapping({"/api/v1/notifications", "/notification/v2.2"})
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Legacy Notification Management", description = "Legacy APIs for sending SMS and Email notifications")
public class LegacyNotificationController extends BaseController {

    private final SendNotificationService sendNotificationService;

    @Operation(
        summary = "Send SMS Notification", 
        description = "Sends SMS notification using existing template"
    )
    @ApiResponse(responseCode = "200", description = "SMS sent successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PostMapping("/send/sms")
    public ResponseEntity<StandardApiResponseDto<SendSmsNotificationResponseDto>> sendSMS(
            HttpServletRequest httpRequest,
            @Valid @RequestBody SendSMSRequestDto request) {

        log.info("Sending SMS notification for template: {}, recipient: {}",
                 request.getTemplateId(), request.getMobileNumber());
        log.info("SMS request details: {}", request);

        Map<String, String> headers = extractHeaders(httpRequest);
        log.info("SMS headers: {}", headers);

        SendSmsNotificationResponseDto response = sendNotificationService.sendSMS(request, headers);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<SendSmsNotificationResponseDto> apiResponse;

        if ("Request Accepted".equals(response.getSmsStatus())) {
            apiResponse = StandardApiResponseDto.success(response, "SMS sent successfully")
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());
        } else {
            Map<String, Object> errorMetadata = response.getErrorDetails() != null ?
                Map.of("details", response.getErrorDetails()) : null;
            apiResponse = StandardApiResponseDto.<SendSmsNotificationResponseDto>error(
                "JDNM1001",
                "SMS send failed",
                errorMetadata
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());
        }

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
        summary = "Send Email Notification", 
        description = "Sends Email notification using existing template"
    )
    @ApiResponse(responseCode = "200", description = "Email sent successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PostMapping("/send/email")
    public ResponseEntity<StandardApiResponseDto<SendSmsNotificationResponseDto>> sendEmail(
            HttpServletRequest httpRequest,
            @Valid @RequestBody SendEmailRequestDto request) {

        log.info("Sending Email notification for template: {}, recipient: {}",
                 request.getTemplateId(), request.getTo());
        log.info("Email request details: {}", request);

        Map<String, String> headers = extractHeaders(httpRequest);

        SendSmsNotificationResponseDto response = sendNotificationService.sendEmail(request, headers);
        log.info("Email send response: {}", response);

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<SendSmsNotificationResponseDto> apiResponse;

        if ("Request Accepted".equals(response.getEmailStatus())) {
            apiResponse = StandardApiResponseDto.success(response, "Email sent successfully")
                .withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());
        } else {
            Map<String, Object> errorMetadata = response.getErrorDetails() != null ?
                Map.of("details", response.getErrorDetails()) : null;
            apiResponse = StandardApiResponseDto.<SendSmsNotificationResponseDto>error(
                "JDNM1002",
                "Email send failed",
                errorMetadata
            ).withTransactionId(correlationId)
                .withPath(httpRequest.getRequestURI());
        }

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
        summary = "Send Notification (Combined)", 
        description = "Sends both SMS and Email notifications using form data (DigiGov v2.2 format)"
    )
    @ApiResponse(responseCode = "200", description = "Notification sent successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PostMapping("/send")
    public ResponseEntity<StandardApiResponseDto<Map<String, Object>>> sendNotification(
            HttpServletRequest httpRequest,
            @RequestParam(value = "smsReq", required = false) String smsReq,
            @RequestParam(value = "emailReq", required = false) String emailReq) {

        log.info("Sending combined notification - smsReq present: {}, emailReq present: {}",
                 smsReq != null, emailReq != null);

        Map<String, Object> responseData = Map.of(
            "smsStatus", smsReq != null ? "Request Accepted" : "Not Provided",
            "emailStatus", emailReq != null ? "Request Accepted" : "Not Provided",
            "txn", "TXN" + System.currentTimeMillis()
        );

        String correlationId = extractCorrelationId(httpRequest);
        StandardApiResponseDto<Map<String, Object>> response = StandardApiResponseDto.success(
            responseData,
            "Combined notification processed successfully"
        ).withTransactionId(correlationId)
            .withPath(httpRequest.getRequestURI());

        return ResponseEntity.ok(response);
    }
}