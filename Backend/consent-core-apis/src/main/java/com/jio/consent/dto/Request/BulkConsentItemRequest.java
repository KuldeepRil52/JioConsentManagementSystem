package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.IdentityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkConsentItemRequest {

    @NotBlank(message = ErrorCodes.JCMP1054)
    @Schema(description = "Transaction ID for the individual consent", example = "b7e4d7f1-92f2-4d6e-a43c-9920b15e1b22")
    private String txnId;

    @NotBlank(message = ErrorCodes.JCMP1030)
    @Schema(description = "Template ID", example = "TPL-001")
    private String templateId;

    @NotBlank(message = ErrorCodes.JCMP1055)
    @Schema(description = "Template version", example = "1.0")
    private String templateVersion;

    @NotNull(message = ErrorCodes.JCMP1033)
    @Schema(description = "Identity type", example = "MOBILE", allowableValues = {"MOBILE", "EMAIL"})
    private IdentityType identityType;

    @NotBlank(message = ErrorCodes.JCMP1034)
    @Schema(description = "Identity value", example = "9876543210")
    private String identityValue;

    @Schema(description = "Timestamp of the consent request", example = "2026-03-12T10:30:00Z")
    private LocalDateTime timeStamp;

    @NotEmpty(message = ErrorCodes.JCMP1027)
    @Valid
    @Schema(description = "List of preferences with actions")
    private List<BulkConsentPreferenceRequest> preferences;
}
