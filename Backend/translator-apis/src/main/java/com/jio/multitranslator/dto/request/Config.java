package com.jio.multitranslator.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.multitranslator.constant.ErrorCodes;
import com.jio.multitranslator.dto.ProviderType;
import com.jio.multitranslator.utils.NotEmptyRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing configuration for translation providers (Bhashini or Microsoft).
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NotEmptyRequest(message = ErrorCodes.JCMPT037)
public class Config {

    @Schema(description = "Provider type", example = "BHASHINI or MICROSOFT")
    @NotNull(message = ErrorCodes.JCMPT006)
    private ProviderType provider;

    /** Common configuration fields */
    @NotBlank(message = ErrorCodes.JCMPT007)
    private String apiBaseUrl;

    /** Bhashini-specific configuration fields */
    private String modelPipelineEndpoint;
    private String callbackUrl;
    private String userId;
    private String apiKey;
    private String pipelineId;

    /** Microsoft-specific configuration fields */
    private String subscriptionKey;
    private String region;
    private String endpoint;
}
