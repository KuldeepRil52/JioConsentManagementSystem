package com.jio.consent.controller;

import com.jio.consent.client.vault.response.VerifyResponse;
import com.jio.consent.constant.Constants;
import com.jio.consent.dto.Request.CreateConsentRequest;
import com.jio.consent.dto.Request.UpdateConsentRequest;
import com.jio.consent.dto.Request.ValidateTokenRequest;
import com.jio.consent.dto.Response.*;
import com.jio.consent.entity.Consent;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1.0/consent")
@Tag(name = "Consent Management System", description = "Operations pertaining to consents")
public class ConsentController {

    ConsentService consentService;

    @Value("${jws.signature.enabled:false}")
    private boolean jwsSignatureEnabled;

    @Autowired
    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }


    @PostMapping("/create")
    @Operation(summary = "Create a consent by consent handle ID",
            requestBody = @RequestBody(description = "Request body for creating consent", required = true,
                    content = @Content(schema = @Schema(implementation = CreateConsentRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Consent created successfully",
                            content = @Content(schema = @Schema(implementation = ConsentCreateResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ConsentCreateResponse> createConsentByConsentHandleId(@Valid @org.springframework.web.bind.annotation.RequestBody CreateConsentRequest request, @RequestHeader Map<String, String> headers) throws Exception {
        return new ResponseEntity<>(this.consentService.createConsentByConsentHandleId(request), HttpStatus.CREATED);
    }


    @PostMapping("/validate-token")
    @Operation(summary = "Validate a consent token",
            requestBody = @RequestBody(description = "Request body for validating consent token", required = true,
                    content = @Content(schema = @Schema(implementation = ValidateTokenRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "x-jws-signature", description = "JWS signature for request validation", required = false, in = ParameterIn.HEADER, example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."),
                    @Parameter(name = "requestor-type", description = "Type of requestor entity", required = false, in = ParameterIn.HEADER, example = "DATA_FIDUCIARY")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent token validated successfully",
                            content = @Content(schema = @Schema(implementation = VerifyResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent token not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<VerifyResponse> validateConsent(@Valid @org.springframework.web.bind.annotation.RequestBody ValidateTokenRequest request, @RequestHeader Map<String, String> headers) throws Exception {
        // Validate signature if x-jws-signature header is present and JWS signature is enabled
        String signature = headers.get(Constants.HEADER_SIGNATURE);
        if (jwsSignatureEnabled && signature != null && !signature.trim().isEmpty()) {
            this.consentService.validateSignature(signature, headers, request);
        }
        
        VerifyResponse response = this.consentService.validateConsentToken(request);
        
        // Sign response if JWS signature is enabled and x-jws-signature was present in request
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(HttpStatus.OK);
        if (jwsSignatureEnabled && signature != null && !signature.trim().isEmpty()) {
            String responseSignature = this.consentService.signResponse(response, headers);
            responseBuilder.header(Constants.HEADER_SIGNATURE, responseSignature);
        }
        
        return responseBuilder.body(response);
    }

    @Operation(summary = "Update Consent By Data principle",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent token validated successfully",
                            content = @Content(schema = @Schema(implementation = ConsentTokenValidateResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent token not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PutMapping("/update/{consentId}")
    public ResponseEntity<UpdateConsentResponse> updateConsent(@Valid @org.springframework.web.bind.annotation.RequestBody UpdateConsentRequest request, @PathVariable("consentId") String consentId) throws Exception {
        Consent response = this.consentService.updateConsent(request, consentId);
        return new ResponseEntity<>(UpdateConsentResponse.builder().consentId(response.getConsentId()).message("Consent Updated Successfully!").build(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/search")
    @Operation(summary = "Search for consents",
            description = "Search and filter consents based on query parameters. Both tenant-id and txn headers are mandatory.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "TemplateId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "consentId", description = "ConsentId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "customerIdentifiers.type", description = "Identifier Type", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"MOBILE", "EMAIL"}, example = "MOBILE")),
                    @Parameter(name = "customerIdentifiers.value", description = "Identifier value", required = false, in = ParameterIn.QUERY, example = "9818123456 / abc@gmail.com"),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consents retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SearchResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<SearchResponse<Consent>> search(@Parameter(hidden = true) @RequestParam Map<String, Object> reqParams) throws ConsentException {
        SearchResponse<Consent> consentSearchResponse = this.consentService.searchConsentsByParams(reqParams);
        return new ResponseEntity<>(consentSearchResponse, HttpStatus.OK);
    }

    @GetMapping("/count")
    @Operation(summary = "Get count of consents",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent count retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CountResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CountResponse> count() {
        return new ResponseEntity<>(CountResponse.builder().count(this.consentService.count()).build(), HttpStatus.OK);
    }

    @GetMapping("/count-by-params")
    @Operation(summary = "Get count of consents based on parameters",
            description = "Get count of consents filtered by query parameters. Uses same parameter filtering as search API. Both tenant-id and txn headers are mandatory.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "TemplateId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "consentId", description = "ConsentId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "status", description = "Consent Status", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string", allowableValues = {"ACTIVE", "INACTIVE", "EXPIRED", "WITHDRAWN"}, example = "ACTIVE")),
                    @Parameter(name = "businessId", description = "Business ID", required = false, in = ParameterIn.QUERY, example = "business123"),
                    @Parameter(name = "consentHandleId", description = "Consent Handle ID", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "customerIdentifiers.type", description = "Identifier Type", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"MOBILE", "EMAIL"}, example = "MOBILE")),
                    @Parameter(name = "customerIdentifiers.value", description = "Customer Identifier value", required = false, in = ParameterIn.QUERY, example = "9818123456")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent count retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CountResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CountResponse> countByParams(@Parameter(hidden = true) @RequestParam Map<String, Object> reqParams) throws ConsentException {
        CountResponse response = this.consentService.countByParams(reqParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/count-status-by-params")
    @Operation(summary = "Get count of consents grouped by status",
            description = "Get count of consents grouped by consent status along with total count. Uses same parameter filtering as search API. Both tenant-id and txn headers are mandatory.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "templateId", description = "TemplateId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "consentId", description = "ConsentId (UUID)", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "businessId", description = "Business ID", required = false, in = ParameterIn.QUERY, example = "business123"),
                    @Parameter(name = "consentHandleId", description = "Consent Handle ID", required = false, in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "customerIdentifiers.type", description = "Identifier Type", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"MOBILE", "EMAIL"}, example = "MOBILE")),
                    @Parameter(name = "customerIdentifiers.value", description = "Customer Identifier value", required = false, in = ParameterIn.QUERY, example = "9818123456")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent count by status retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ConsentStatusCountResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ConsentStatusCountResponse> countByStatus(@Parameter(hidden = true) @RequestParam Map<String, Object> reqParams) throws ConsentException {
        ConsentStatusCountResponse response = this.consentService.countByStatus(reqParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/verify-payload-hash/{consentId}")
    @Operation(summary = "Verify payload hash for a consent",
            description = "Verify if the current payloadHash matches the hash of the consentString after fetching. Only verifies active consents.",
            parameters = {
                    @Parameter(name = "consentId", description = "Consent ID (UUID)", required = true, in = ParameterIn.PATH, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payload hash verification completed",
                            content = @Content(schema = @Schema(implementation = VerifyPayloadHashResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Active consent not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<VerifyPayloadHashResponse> verifyPayloadHash(@PathVariable("consentId") String consentId) throws ConsentException {
        boolean isValid = this.consentService.verifyPayloadHash(consentId);
        VerifyPayloadHashResponse response = VerifyPayloadHashResponse.builder()
                .consentId(consentId)
                .isValid(isValid)
                .message(isValid ? "Payload hash verification successful" : "Payload hash verification failed")
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/searchConsentByIdentity")
    @Operation(summary = "Search consents by identity using secure code",
            description = "Search for consents associated with the identity from the user session. " +
                    "The X-Secure-Code header is used to authenticate and retrieve the user's identity. " +
                    "The customerIdentifiers.value query parameter must match the identity from the session.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "X-Secure-Code", description = "Secure code for user session authentication", required = true, in = ParameterIn.HEADER, example = "secure-code-token-value"),
                    @Parameter(name = "customerIdentifiers.value", description = "Customer identifier value (must match session identity)", required = true, in = ParameterIn.QUERY, example = "8208449648")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consents retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SearchResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing or invalid X-Secure-Code header"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - user session not found or expired"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - identity mismatch between session and request"),
                    @ApiResponse(responseCode = "404", description = "Not found - no consents found for the identity"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<SearchResponse<Consent>> searchConsentByIdentity(
            @RequestHeader(value = Constants.HEADER_SECURE_CODE, required = false) String secureCode,
            @RequestParam(value = "customerIdentifiers.value", required = false) String customerIdentifierValue) throws ConsentException {
        SearchResponse<Consent> consentSearchResponse = this.consentService.searchConsentByIdentity(secureCode, customerIdentifierValue);
        return new ResponseEntity<>(consentSearchResponse, HttpStatus.OK);
    }

}
