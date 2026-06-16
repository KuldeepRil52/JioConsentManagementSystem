package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.validation.ValidParentIdentity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidParentIdentity
public class CreateParentalConsentRequest {

    @NotBlank(message = ErrorCodes.JCMP1070)
    @Schema(description = "Consent Handle ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String consentHandleId;

    @NotBlank(message = ErrorCodes.JCMP1071)
    @Schema(description = "Parent Identity Value (e.g., mobile number or email)", example = "9876543210")
    private String parentIdentity;
    
    @Pattern(regexp = "^(MOBILE|EMAIL)$", message = ErrorCodes.JCMP1072)
    @Schema(description = "Parent Identity Type - must be MOBILE or EMAIL", example = "MOBILE")
    private String parentIdentityType;

    @NotNull(message = ErrorCodes.JCMP1073)
    @Schema(description = "Is Parental Consent Required", example = "true")
    private Boolean isParental;

    @NotBlank(message = ErrorCodes.JCMP1074)
    @Schema(description = "Parent Name", example = "John Doe")
    private String parentName;


    @Pattern(regexp = "^(https?://)[^\\s]+$", message = ErrorCodes.JCMP1075)
    @Schema(description = "Redirect URI for callback (http or https, supports domain or IP)", example = "https://example.com/callback")
    private String redirectUri;
}

