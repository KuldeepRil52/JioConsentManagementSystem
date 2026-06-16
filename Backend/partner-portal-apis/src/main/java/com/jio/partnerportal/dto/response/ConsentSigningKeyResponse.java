package com.jio.partnerportal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentSigningKeyResponse {

    @Schema(description = "Response message", example = "Consent signing key generated successfully")
    private String message;

    @Schema(description = "Business ID", example = "566d6143-a5e2-47f6-91ae-e271e2105000")
    private String businessId;

    @Schema(description = "Public key in PEM format", example = "-----BEGIN PUBLIC KEY-----\\nMIICIjANBgkqhkiG9w0BAQEFAAOCAg8A...")
    private String publicKeyPem;

    @Schema(description = "Certificate type", example = "rsa-4096")
    private String certType;

    @Schema(description = "Scope level", example = "TENANT", allowableValues = {"TENANT", "BUSINESS"})
    private String scopeLevel;

}

