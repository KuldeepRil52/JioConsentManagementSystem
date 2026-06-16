package com.example.scanner.dto.response;

import com.example.scanner.dto.ConsentDetail;
import com.example.scanner.enums.VersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard template-level response with grouped consents")
public class DashboardTemplateResponse {

    @Schema(description = "Template ID")
    private String templateId;

    @Schema(description = "Template status (active/revoked/expired)")
    private VersionStatus status;

    @Schema(description = "Main scanned site URL")
    private String scannedSites;

    @Schema(description = "List of scanned subdomains")
    private List<String> scannedSubDomains;

    @Schema(description = "Scan transaction ID")
    private String scanId;

    @Schema(description = "List of consents for this template")
    private List<ConsentDetail> consents;
}