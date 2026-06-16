package com.jio.auth.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.auth.docs.SessionDocs;
import com.jio.auth.dto.SessionResponseDto;
import com.jio.auth.dto.SessionStatusResponseDto;
import com.jio.auth.dto.TenantSecretCode;
import com.jio.auth.service.SessionService;
import com.jio.auth.service.signing.RequestResponseSignatureService;
import com.jio.auth.validation.ValidateBody;
import com.jio.auth.validation.ValidateHeader;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/secureCode")
@Tag(name = "Secure Code APIs", description = "Endpoints for static token session handling")
public class SecureCodeController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ValidateBody validateBody;

    @Autowired
    private ValidateHeader validateHeader;

    @Autowired
    private RequestResponseSignatureService  requestResponseSignatureService;

    @SessionDocs.Create
    @PostMapping("/create")
    public ResponseEntity<SessionResponseDto> createSession(@RequestBody TreeMap<String, String> payload, @RequestHeader LinkedHashMap<String, String> headers) {
        requestResponseSignatureService.verifyRequest(payload, headers);
        validateBody.validateSessionBody(payload);

        ObjectMapper mapper = new ObjectMapper();
        SessionResponseDto response = sessionService.createSession(payload, headers);
        Map<String, Object> map =
                mapper.convertValue(response, new TypeReference<TreeMap<String, Object>>() {});

        String signature = requestResponseSignatureService.signResponse(map);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("x-jws-signature", signature);

        return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
    }

    @SessionDocs.Validate
    @PostMapping("/validate")
    public SessionStatusResponseDto validateSession(@RequestHeader Map<String, String> headers) {
        validateHeader.validateSessionHeader(headers);
        return sessionService.checkSession(headers);
    }

    @SessionDocs.CheckAccessToken
    @GetMapping("/validate")
    public ResponseEntity<SessionStatusResponseDto> checkAccessTokenOnly(
            @RequestHeader Map<String, String> headers) {

        SessionStatusResponseDto response = sessionService.checkAccessTokenOnly(headers);

        String tenantId = response.getTenantId();
        String businessId = response.getBusinessId();

        return ResponseEntity
                .ok()
                .header("tenant-id", tenantId)
                .header("business-id", businessId)
                .body(response);
    }

    @GetMapping("/tenant/validate")
    public ResponseEntity<TenantSecretCode> checkTenantToken(
            @RequestHeader Map<String, String> headers) {
        validateHeader.validateTenantToken(headers);
        TenantSecretCode response = sessionService.CheckTenantSecretWithIdentityValue(headers);
        return ResponseEntity
                .ok(response);
    }


}
