package com.jio.vault.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.vault.constants.ErrorCode;
import com.jio.vault.documents.ClientPublicCert;
import com.jio.vault.dto.*;
import com.jio.vault.dto.cryptodto.DecryptedPayloadResponse;
import com.jio.vault.dto.cryptodto.EncryptPayloadResponse;
import com.jio.vault.exception.CustomException;
import com.jio.vault.service.PayloadCryptoService;
import com.jio.vault.service.VaultService;
import com.jio.vault.service.TenantValidationService;
import com.jio.vault.validation.ValidateHeader;
import com.jio.vault.validation.ValidateBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/client")
public class ClientController {

    private final VaultService certService;
    private final TenantValidationService tenantValidationService;

    @Autowired
    private PayloadCryptoService  payloadCryptoService;

    @Autowired
    private ValidateHeader validateHeader;

    @Autowired
    private ValidateBody validateBody;

    public ClientController(VaultService certService, TenantValidationService tenantValidationService) {
        this.certService = certService;
        this.tenantValidationService = tenantValidationService;
    }


    @Operation(
            summary = "Onboard client private certificate",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request body should contain certificate details",
                    required = false,
                    content = @Content(schema = @Schema(implementation = OnboardCertRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Example Request",
                                            value = "{\n  \"certType\": \"rsa-4096\"\n}"
                                    )
                            }
                            )

            ),
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Onboards the client certificate successfully",
                            content = @Content(schema = @Schema(implementation = ClientPublicCert.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Client certificate already exists",
                            content = @Content(schema = @Schema(implementation = ClientPublicCert.class))
                    )
            }
    )
    @PostMapping("/key/onboard")
    public ResponseEntity<OnboardCertResponse> onboardCert(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) OnboardCertRequest request
    ) {
        validateHeader.validateHeadersExceptKeyId(headers);
        String tenantId = headers.get("tenant-id");
        String businessId = headers.get("business-id");

        String certType = request != null ? request.getCertType() : null;
        OnboardCertResponse cert = tenantValidationService.validateCert(tenantId, businessId);
        log.info("validated the Tenant and Business");
        if(cert != null){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(cert);
        }
        cert = new OnboardCertResponse();
        ClientPublicCert existing = certService.createAndSaveCert(businessId, tenantId, certType);
        cert.setTenantId(existing.getTenantId());
        cert.setBusinessId(existing.getBusinessId());
        cert.setCertType(existing.getCertType());
        cert.setPublicKeyPem(existing.getPublicKeyPem());
        cert.setMessage("The BusinessID has been onboarded Successfully");
        return ResponseEntity.ok(cert);

    }

    @Operation(summary = "Sign payload",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body should contain payload which is json", required = true,
                    content = @Content(schema = @Schema(implementation = SignPayloadRequest.class))),
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Signs the payload successfully and returns the jwt token",
                            content = @Content(schema = @Schema(implementation = JwtSignResponse.class)))
            })
    @PostMapping("/sign")
    public ResponseEntity<JwtSignResponse> signPayload(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) SignPayloadRequest request
    )  {

        validateHeader.validateHeadersExceptKeyId(headers);
        validateBody.validatePayload(request);
        String tenantId = headers.get("tenant-id");
        String businessId = headers.get("business-id");
        tenantValidationService.validateTenantAndBusiness(tenantId, businessId);
        String payloadString = request.getPayloadAsString();
        String jwt = certService.generateJwtForTenantAndBusiness(businessId, tenantId, payloadString);
        return ResponseEntity.ok(new JwtSignResponse(jwt));
    }




    @Operation(
            summary = "Verify signed JWT",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request body should contain the JWT to verify",
                    required = true,
                    content = @Content(schema = @Schema(implementation = VerifyJwtSignRequest.class))
            ),
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")

            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Verifies the JWT successfully and returns the payload",
                            content = @Content(schema = @Schema(implementation = VerifyJwtSignResponse.class))
                    )
            }
    )
    @PostMapping("/verify")
    public ResponseEntity<VerifyJwtSignResponse> verifyJwt(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) VerifyJwtSignRequest request
    ) {
        validateHeader.validateHeadersExceptKeyId(headers);
        validateBody.validateJwtRequest(request);

        String tenantId = headers.get("tenant-id");
        String businessId = headers.get("business-id");

        tenantValidationService.validateTenantAndBusiness(tenantId, businessId);

        String jwt = request.getJwt();
        VerifyJwtSignResponse response = certService.verifyJwtForTenantAndBusiness(tenantId, businessId, jwt);

        if (response.getPayload() == null || response.getPayload().isEmpty()) {
            throw new CustomException(ErrorCode.JWT_VALIDATION_FAILED);
        }
        if(!response.isValid()){
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }



    @Operation(
            summary = "Get all key IDs for a tenant and business",
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Returns all Key IDs for the given Tenant and Business",
                            content = @Content(schema = @Schema(implementation = ClientKeyDto.class))
                    )
            }
    )
    @GetMapping("/getKey")
    public ResponseEntity<ClientKeyDto> getKeyIds(
            @RequestHeader Map<String, String> headers
    ) {
        validateHeader.validateHeadersExceptKeyId(headers);

        String tenantId = headers.get("tenant-id");
        String businessId = headers.get("business-id");

        tenantValidationService.validateTenantAndBusiness(tenantId, businessId);

        return ResponseEntity.ok(certService.getKeysByTenantAndBusiness(tenantId, businessId));
    }



    @Operation(
            summary = "Encrypt data",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Data to encrypt in base64 format",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EncryptRequest.class))
            ),
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Encrypts the data successfully",
                            content = @Content(schema = @Schema(implementation = EncryptResponse.class))
                    )
            }
    )
    @PostMapping("/encrypt")
    public ResponseEntity<EncryptResponse> encrypt(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) EncryptRequest request
    ) {
        validateHeader.validateHeadersExceptKeyId(headers);
        validateBody.validateEncryptRequest(request);

        String tenantId = headers.get("tenant-id");
        String businessId = headers.get("business-id");

        String requestAsString;
        try {
            requestAsString = new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Failed to parse the request body");
        }
        tenantValidationService.validateTenantAndBusiness(tenantId, businessId);
        EncryptResponse response = certService.encryptData(tenantId, businessId, requestAsString);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Decrypt data",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Data to decrypt",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DecryptRequest.class))
            ),
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Decrypts the data successfully",
                            content = @Content(schema = @Schema(implementation = DecryptResponse.class))
                    )
            }
    )
    @PostMapping("/decrypt")
    public ResponseEntity<DecryptResponse> decrypt(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) DecryptRequest request
    ) {
        validateHeader.validateHeadersExceptKeyId(headers);
        validateBody.validateDecryptRequest(request);

        String tenantId = headers.get("tenant-id");
        String businessId = headers.get("business-id");


        String requestAsString;
        try {
            requestAsString = new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Failed to parse the request body");
        }

        tenantValidationService.validateTenantAndBusiness(tenantId, businessId);
        DecryptResponse response = certService.decryptData(tenantId, businessId, requestAsString);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Encrypt data",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Data to encrypt in json format",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "string": "sample",
                                              "object": {
                                                "anynumber": 1234,
                                                "something": {
                                                  "another string": "abcd"
                                                }
                                              }
                                            }
                                            """
                            )
                    )

            ),
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "01d86a53-15ea-4952-be12-8f7a77c3a72c"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "01d86a53-15ea-4952-be12-8f7a77c3a72c"),
                    @Parameter(name = "Data-Category-Type", in = ParameterIn.HEADER, required = true, description = "Data-Category-Type", example = "consent"),
                    @Parameter(name = "Data-Category-Value", in = ParameterIn.HEADER, required = true, description = "Data-Category-Value", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully encrypted response",
                            content = @Content(
                                    schema = @Schema(implementation = EncryptPayloadResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                            {
                                                "tenantId": "01d86a53-15ea-4952-be12-8f7a77c3a72c",
                                                "businessId": "01d86a53-15ea-4952-be12-8f7a77c3a72c",
                                                "dataCategoryType": "consent",
                                                "dataCategoryValue": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                                                "referenceId": "a35d3fac-c5a7-4c8c-a95c-a174e3f81b24",
                                                "encryptedString": "vault:v1:HP9gY7cWoHlFrv5YZAzVK2SCeODjLbsI6Pns+JQGHgJXtFD1tZ6mnyeVFonWGFBICelBtcRrYVSblXAKvfZr9I9ZiM22Q0tz51lVsLv40XZHZC3xJYD0oM3aHX+EHEw6SMWhAmWss26ilFdF+ZnXn/0Y0q/8gMXuzembCL4TKOZWBo3IVlgc20FoFLexrkBwU/vuKvpQtZAXUWUwJCeL4u14bpln4dW4ubRsO+Db8CoS8shef/uZlmN0zheo0uA/e6nky00c7Ecii+NBFvSO9rDFUTRXMlFWWPFcC8cC/DiOi2b+zYt1R1M+d56nnp+Bqxq1mv+2jLxDmiRHwxsAekOlm1YqsaSDtb2udW0ZpBUuVZZZy9tVI9vb+IyVeWws9wHARfa42rNJAjBPiHXtsdrpdQZaTuP4Cw0wKbrUKgTmzVF8zwtg4y5QzTdV5HyScki2KUpXARNLWwb52Va/uHY94brPmU/h43L+o69LHUS1lMiMTPTSg6vuO5v6hqRwMTmOdz8DZNxDTR+FCALLAcpNhg0L3BP7wB1X7lyxXYKlHcW/+5PT2lYSFOExZ7wDXS/hBIWPpf2rIpm6LS0GI2eDalmhEby6qZUj7LVvBzcH3isObXxyTWTVaz8+TSQlukjFz2qrsrkknHnqR8jsVXw+tCHnwn1zcM9/WRqcDDE=",
                                                "createdTimeStamp": "2025-11-04T20:14:09.253588800"
                                            }
                                            """
                                    )
                            )
                    )

            }
    )

    @PostMapping("/encryptPayload")
    public ResponseEntity<EncryptPayloadResponse> encryptPayload(
            @RequestHeader Map<String, String> headers,
            @RequestBody String jsonPayload) {

        try {
            new ObjectMapper().readTree(jsonPayload);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Malformed JSON payload");
        }

        validateHeader.validateEncryptHeaders(headers);
        EncryptPayloadResponse response = payloadCryptoService.encryptAndStorePayload(headers, jsonPayload);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Decrypt data",
            parameters = {
                    @Parameter(name = "Tenant-Id", in = ParameterIn.HEADER, required = true, description = "Tenant Identifier", example = "01d86a53-15ea-4952-be12-8f7a77c3a72c"),
                    @Parameter(name = "Business-Id", in = ParameterIn.HEADER, required = true, description = "Business Identifier", example = "01d86a53-15ea-4952-be12-8f7a77c3a72c"),
                    @Parameter(name = "Reference-Id", in = ParameterIn.HEADER, required = true, description = "Reference Identifier to get the cipher/encrypted text", example = "a35d3fac-c5a7-4c8c-a95c-a174e3f81b24")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Decrypts the data successfully",
                            content = @Content(
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                        "referenceId": "a35d3fac-c5a7-4c8c-a95c-a174e3f81b24",
                                                        "decryptedPayload": {
                                                            "string": "sample",
                                                            "object": {
                                                                "anynumber": 1234,
                                                                "something": {
                                                                    "another string": "abcd"
                                                                }
                                                            }
                                                        }
                                                    }
                                            """
                                    )
                            )
                    )

            }
    )
    @GetMapping("/decryptedPayload")
    public ResponseEntity<DecryptedPayloadResponse> decryptedPayload(
            @RequestHeader Map<String, String> headers
    ){
        validateHeader.validateDecryptHeaders(headers);
        return ResponseEntity.ok(payloadCryptoService.decryptCipher(headers));
    }



}
