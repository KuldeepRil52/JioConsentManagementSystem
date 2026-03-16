package com.example.scanner.dto.response;

import com.example.scanner.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard consent data response")
public class DashboardResponse {

    @Schema(description = "Consent ID")
    private String consentID;

    @Schema(description = "Consent Handle ID")
    private String consentHandle;

    @Schema(description = "Consent start date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime startDate;

    @Schema(description = "Consent end date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime endDate;

    @Schema(description = "Cookie categories")
    private List<String> cookieCategory;

    @Schema(description = "Main scanned URL")
    private String scannedSite;

    @Schema(description = "Subdomains scanned")
    private List<String> subDomain;

    @Schema(description = "Consent status")
    private Status status;

    @Schema(description = "Template version")
    private Integer version;
}