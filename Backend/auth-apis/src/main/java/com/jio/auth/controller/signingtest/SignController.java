package com.jio.auth.controller.signingtest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jio.auth.service.signing.RequestResponseSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RestController
@RequestMapping("/test")
public class SignController {

    @Autowired
    private RequestResponseSignatureService signatureService;

    @PostMapping("/sign")
    public ResponseEntity<TreeMap<String, Object>> signPayload(
            @RequestHeader LinkedHashMap<String, String> headers,
            @RequestBody TreeMap<String, Object> payload) {

        log.info("Received payload for signing: {}", payload);
        String detachedJwt = signatureService.signRequest(payload);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("x-jws-signature", detachedJwt);

        log.info("Returning signed response with detached JWT");
        return new ResponseEntity<>(payload, responseHeaders, HttpStatus.OK);
    }

    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyPayload(
            @RequestHeader LinkedHashMap<String, String> headers,
            @RequestBody TreeMap<String, String> payload) {

        log.info("Received payload for verification: {}", payload);

        boolean verified = signatureService.verifyResponse(payload, headers);
        log.info("Verification result: {}", verified);

        return ResponseEntity.ok(verified);
    }
}

