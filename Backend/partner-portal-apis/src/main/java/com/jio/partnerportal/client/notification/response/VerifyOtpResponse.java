package com.jio.partnerportal.client.notification.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response from notification service verify OTP")
public class VerifyOtpResponse {

    @Schema(description = "OTP verification status", example = "true")
    @JsonProperty("verified")
    private Boolean verified;

    @Schema(description = "Status", example = "SUCCESS")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Message", example = "OTP verified successfully")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Transaction ID", example = "SYS-TXN-5c9a61b8-87a6-4bac-85f7-1e873209ce75")
    @JsonProperty("txnId")
    private String txnId;
}

