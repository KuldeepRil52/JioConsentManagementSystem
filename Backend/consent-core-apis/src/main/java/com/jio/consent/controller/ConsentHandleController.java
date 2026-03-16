package com.jio.consent.controller;

import com.jio.consent.constant.Constants;
import com.jio.consent.dto.Request.CreateHandleRequest;
import com.jio.consent.dto.Request.CreateParentalConsentRequest;
import com.jio.consent.dto.Response.ConsentHandleResponse;
import com.jio.consent.dto.Response.CountResponse;
import com.jio.consent.dto.Response.GetHandleResponse;
import com.jio.consent.dto.Response.GetParentalConsentHandleResponse;
import com.jio.consent.dto.Response.ParentalConsentResponse;
import com.jio.consent.dto.Response.SearchResponse;
import com.jio.consent.entity.ConsentHandle;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.service.ConsentHandleService;
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
@RequestMapping("/v1.0/consent-handle")
@Tag(name = "Consent Handle Management System", description = "Operations pertaining to consent handles")
public class ConsentHandleController {

    ConsentHandleService consentHandleService;

    @Value("${jws.signature.enabled:false}")
    private boolean jwsSignatureEnabled;

    @Autowired
    public ConsentHandleController(ConsentHandleService consentHandleService) {
        this.consentHandleService = consentHandleService;
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new consent handle",
            requestBody = @RequestBody(description = "Request body for creating a consent handle", required = true,
                    content = @Content(schema = @Schema(implementation = CreateHandleRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "business-id", description = "Business ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "x-jws-signature", description = "JWS signature for request validation", required = false, in = ParameterIn.HEADER, example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."),
                    @Parameter(name = "requestor-type", description = "Type of requestor entity", required = false, in = ParameterIn.HEADER, example = "DATA_FIDUCIARY")
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Consent Handle created successfully",
                            content = @Content(schema = @Schema(implementation = ConsentHandleResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ConsentHandleResponse> createConsentHandle(@Valid @org.springframework.web.bind.annotation.RequestBody CreateHandleRequest request, @RequestHeader Map<String, String> headers) throws ConsentException {
        // Validate signature if x-jws-signature header is present and JWS signature is enabled
        String signature = headers.get(Constants.HEADER_SIGNATURE);
        if (jwsSignatureEnabled && signature != null && !signature.trim().isEmpty()) {
            this.consentHandleService.validateSignature(signature, headers, request);
        }
        
        ConsentHandle consentHandle = this.consentHandleService.createConsentHandle(request, headers);
        ConsentHandleResponse response = ConsentHandleResponse.builder()
                .consentHandleId(consentHandle.getConsentHandleId())
                .message("Consent Handle Created successfully!")
                .txnId(headers.get(Constants.TXN_ID))
                .build();
        
        // Sign response if JWS signature is enabled and x-jws-signature was present in request
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(HttpStatus.CREATED);
        if (jwsSignatureEnabled && signature != null && !signature.trim().isEmpty()) {
            String responseSignature = this.consentHandleService.signResponse(response, headers);
            responseBuilder.header(Constants.HEADER_SIGNATURE, responseSignature);
        }
        
        return responseBuilder.body(response);
    }

    @GetMapping("/get/{consentHandleId}")
    @Operation(summary = "Get consent handle by ID",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent Handle retrieved successfully",
                            content = @Content(schema = @Schema(implementation = GetHandleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent Handle not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<GetHandleResponse> getHandleById(@PathVariable("consentHandleId") String consentHandleId) throws ConsentException {
        return new ResponseEntity<>(this.consentHandleService.getConsentHandleById(consentHandleId), HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search for consent handles",
            description = "Search and filter consent handles based on query parameters. Both tenant-id and txn headers are mandatory.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent handles retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SearchResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<SearchResponse<ConsentHandle>> search(@Parameter(hidden = true) @RequestParam Map<String, Object> reqParams) throws ConsentException {
        SearchResponse<ConsentHandle> templates = this.consentHandleService.searchHandlesByParams(reqParams);
        return new ResponseEntity<>(templates, HttpStatus.OK);
    }

    @GetMapping("/count")
    @Operation(summary = "Get count of consent handles",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent handle count retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CountResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CountResponse> count() {
        return new ResponseEntity<>(CountResponse.builder().count(this.consentHandleService.count()).build(), HttpStatus.OK);
    }

    @PostMapping("/parental-consent")
    @Operation(summary = "Create a parental consent request",
            description = "Creates a parental consent request by generating a secure code and updating the consent handle with parental details",
            requestBody = @RequestBody(description = "Request body for creating a parental consent request", required = true,
                    content = @Content(schema = @Schema(implementation = CreateParentalConsentRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Parental consent request created successfully",
                            content = @Content(schema = @Schema(implementation = ParentalConsentResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent Handle not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ParentalConsentResponse> createParentalConsentRequest(
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateParentalConsentRequest request,
            @RequestHeader(Constants.TXN_ID) String txnId,
            @RequestHeader(Constants.TENANT_ID_HEADER) String tenantId) throws ConsentException {
        ParentalConsentResponse response = this.consentHandleService.createParentalConsentRequest(request, tenantId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/parental-consent/{consentHandleId}")
    @Operation(summary = "Get parental consent handle by ID",
            description = "Retrieves parental consent handle details including parent information by consent handle ID",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "consentHandleId", description = "Consent Handle ID", required = true, in = ParameterIn.PATH, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Parental consent handle retrieved successfully",
                            content = @Content(schema = @Schema(implementation = GetParentalConsentHandleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent Handle not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<GetParentalConsentHandleResponse> getParentalConsentHandleById(
            @PathVariable("consentHandleId") String consentHandleId,
            @RequestHeader(Constants.TXN_ID) String txnId,
            @RequestHeader(Constants.TENANT_ID_HEADER) String tenantId) throws ConsentException {
        return new ResponseEntity<>(this.consentHandleService.getParentalConsentHandleById(consentHandleId), HttpStatus.OK);
    }

}
