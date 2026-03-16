package com.jio.auth.controller;

import com.jio.auth.constants.Keys;
import com.jio.auth.docs.AuthDocs;
import com.jio.auth.dto.IntrospectResponse;
import com.jio.auth.dto.JwtResponse;
import com.jio.auth.dto.RevokeResponse;
import com.jio.auth.service.TokenService;
import com.jio.auth.validation.ValidateBody;
import com.jio.auth.validation.ValidateHeader;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth2")
@Tag(name = "Auth Handler APIs", description = "Endpoints for jwt session token handling")
public class AuthController {
    @Autowired
    private ValidateHeader validateHeader;

    @Autowired
    private ValidateBody validateBody;

    @Autowired
    private Keys keys;

    private final TokenService tokenService;

    public AuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @AuthDocs.Introspect
    @PostMapping(value = "/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<IntrospectResponse> introspectToken(
            @RequestParam("token") String token
    ) {
        String jsonContent = keys.getPublicKey();
        IntrospectResponse response = tokenService.validateToken(token, jsonContent);
        if (!response.isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @AuthDocs.Revoke
    @PostMapping("/revoke")
    public ResponseEntity<RevokeResponse> revokeToken(
            @RequestHeader Map<String, String> headers
    ) {
        String jsonContent = keys.getPrivateKey();
        validateHeader.validateHeaders(headers);
        String token = headers.get("x-session-token");
        log.info("Revoke request received for token: {}", token);
        tokenService.revoke(token, jsonContent);
        log.info("Token {} successfully revoked", token);
        RevokeResponse response = new RevokeResponse(token, true);
        return ResponseEntity.ok(response);
    }

    @AuthDocs.Generate
    @PostMapping("/token")
    public ResponseEntity<JwtResponse> getToken(@RequestBody Map<String, String> payload, @RequestHeader Map<String, String> headers){
        String jsonContent = keys.getPrivateKey();
        validateBody.ValidateClaimsBody(payload);
        String tenantId = payload.get("tenantId");
        String businessId = payload.get("businessId");
        JwtResponse response = tokenService.generateJwt(payload, jsonContent);
        return ResponseEntity
                .ok()
                .header("tenant-id", tenantId)
                .header("business-id", businessId)
                .body(response);
    }

    @AuthDocs.Validate
    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<IntrospectResponse> validateToken(
            @RequestParam("token") String token,
            @RequestHeader Map<String, String> headers
    ) {

        String jsonContent = keys.getPublicKey();
        IntrospectResponse response = tokenService.validateTokenAndFetchWso2Token(token, jsonContent);
        log.info("Token successfully validated");
        HashMap<String, String> map = tokenService.extractTenantAndBusinessFromJwt(token);
        if (!response.isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok().header("tenant-id", map.get("tenantId")).header("business-id", map.get("businessId")).body(response);
    }
}

