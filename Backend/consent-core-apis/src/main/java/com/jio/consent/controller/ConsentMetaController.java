package com.jio.consent.controller;

import com.jio.consent.dto.Request.CreateConsentMetaRequest;
import com.jio.consent.dto.Response.ConsentMetaResponse;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.service.ConsentMetaService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1.0/consent-meta")
@Tag(name = "Consent Meta Management System", description = "Operations pertaining to consent metadata")
public class ConsentMetaController {

    ConsentMetaService consentMetaService;

    @Autowired
    public ConsentMetaController(ConsentMetaService consentMetaService) {
        this.consentMetaService = consentMetaService;
    }

    @PostMapping("/create")
    @Operation(summary = "Create a consent meta",
            requestBody = @RequestBody(description = "Request body for creating consent meta", required = true,
                    content = @Content(schema = @Schema(implementation = CreateConsentMetaRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Consent Meta created successfully",
                            content = @Content(schema = @Schema(implementation = ConsentMetaResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ConsentMetaResponse> createConsentMeta(@Valid @org.springframework.web.bind.annotation.RequestBody CreateConsentMetaRequest request, @RequestHeader Map<String, String> headers) throws Exception {
        return new ResponseEntity<>(this.consentMetaService.createConsentMeta(request), HttpStatus.CREATED);
    }

    @GetMapping("/{consentMetaId}")
    @Operation(summary = "Get consent meta by ID",
            parameters = {
                    @Parameter(name = "consentMetaId", description = "Consent Meta ID", required = true, in = ParameterIn.PATH, example = "tenant-id:random-uuid"),
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent Meta retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ConsentMetaResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent Meta not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ConsentMetaResponse> getConsentMetaById(@PathVariable("consentMetaId") String consentMetaId) throws ConsentException {
        return new ResponseEntity<>(this.consentMetaService.getConsentMetaById(consentMetaId), HttpStatus.OK);
    }

}

