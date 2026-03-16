package com.jio.digigov.notification.dto.request.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "Template filtering and pagination parameters")
public class TemplateFilterRequestDto {
    
    @Schema(description = "Filter by event type(s) - comma separated", example = "CONSENT_GRANTED,CONSENT_REVOKED")
    private String eventType;

    @Schema(description = "Filter by recipient type", example = "DATA_PRINCIPAL",
            allowableValues = {"DATA_PRINCIPAL", "DATA_FIDUCIARY", "DATA_PROCESSOR", "DATA_PROTECTION_OFFICER"})
    private String recipientType;

    @Schema(description = "Filter by language(s) - comma separated", example = "english,hindi")
    private String language;

    @Schema(description = "Filter by channel", example = "SMS", allowableValues = {"SMS", "EMAIL"})
    private String channel;
    
    @Schema(description = "Filter by template type", example = "NOTIFICATION", allowableValues = {"NOTIFICATION", "OTPVALIDATOR"})
    private String type;
    
    @Schema(description = "Filter by status", example = "ACTIVE", allowableValues = {"ACTIVE", "PENDING", "INACTIVE", "FAILED"})
    private String status;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Templates created after this date", example = "2024-01-01")
    private LocalDate fromDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Templates created before this date", example = "2024-01-31")
    private LocalDate toDate;
    
    @Schema(description = "Search in template content", example = "OTP")
    private String search;
    
    @Schema(description = "Page number (default: 1)", example = "1", defaultValue = "1")
    private Integer page = 1;
    
    @Schema(description = "Items per page (default: 20, max: 100)", example = "20", defaultValue = "20")
    private Integer pageSize = 20;
    
    @Schema(description = "Sort field and order", example = "createdAt:desc", defaultValue = "createdAt:desc")
    private String sort = "createdAt:desc";
}