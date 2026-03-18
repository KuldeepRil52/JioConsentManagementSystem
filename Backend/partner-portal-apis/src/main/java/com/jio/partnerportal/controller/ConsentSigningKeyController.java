package com.jio.partnerportal.controller;

import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.CertType;
import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.dto.response.ConsentSigningKeyResponse;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.ConsentSigningKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1.0/consent-signing-key")
public class ConsentSigningKeyController {

    private final ConsentSigningKeyService consentSigningKeyService;

    @Autowired
    public ConsentSigningKeyController(ConsentSigningKeyService consentSigningKeyService) {
        this.consentSigningKeyService = consentSigningKeyService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/generate")
    @Operation(
            summary = "Generate consent signing key",
            description = "Generates a consent signing key by calling the Vault API and stores the public key in the client_credentials collection. If an entry already exists for the given businessId and scopeLevel, it returns the existing value.",
            parameters = {
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "566d6143-a5e2-47f6-91ae-e271e2105000")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"}, example = "TENANT")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "566d6143-a5e2-47f6-91ae-e271e2105000")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
                    @Parameter(name = "certType", description = "Certificate type (optional, defaults to rsa-2048)", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string", allowableValues = {"rsa-2048", "rsa-3072", "rsa-4096"}, example = "rsa-4096"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Consent signing key generated or retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConsentSigningKeyResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "header": "business-id",
                                                  "errorMessage": "Business ID is required",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-10-06T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-10-06T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<ConsentSigningKeyResponse> generateConsentSigningKey(
            @RequestHeader Map<String, String> headers,
            @RequestParam(value = "certType", required = false, defaultValue = "rsa-2048") String certType, HttpServletRequest req )
            throws PartnerPortalException {

        // Validate certType using enum
        try {
            CertType.valueOf(certType.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new PartnerPortalException(ErrorCodes.JCMP1003); // Invalid certType
        }

        String tenantId = headers.get("tenant-id");
        String businessId = headers.get("business-id");
        String scopeLevel = headers.get("scope-level");

        ConsentSigningKeyResponse response = consentSigningKeyService.generateConsentSigningKey(
                tenantId, businessId, scopeLevel, certType,req);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

